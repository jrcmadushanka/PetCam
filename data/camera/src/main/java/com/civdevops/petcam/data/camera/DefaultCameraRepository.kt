package com.civdevops.petcam.data.camera

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.takePicture
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.civdevops.petcam.core.model.MediaId
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.RecordingFailure
import com.civdevops.petcam.core.model.camera.RecordingState
import com.civdevops.petcam.data.media.PhotoStorage
import com.civdevops.petcam.data.media.PhotoStorageException
import com.civdevops.petcam.data.media.VideoStorage
import com.civdevops.petcam.domain.camera.CameraOperationFailure
import com.civdevops.petcam.domain.camera.CameraOperationResult
import com.civdevops.petcam.domain.camera.PhotoCaptureFailure
import com.civdevops.petcam.domain.camera.PhotoCaptureResult
import com.civdevops.petcam.domain.camera.RecordingCommandResult
import com.civdevops.petcam.domain.camera.VideoRecordingRequest
import com.civdevops.petcam.domain.camera.VideoRecordingResult
import com.civdevops.petcam.domain.repository.CameraRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.concurrent.Executor
import javax.inject.Inject

class DefaultCameraRepository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val cameraXSession: CameraXSession,
    private val photoStorage: PhotoStorage,
    private val videoStorage: VideoStorage
) : CameraRepository {

    private val recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    private val recordingMutex = Mutex()
    private val recordingEventExecutor: Executor = ContextCompat.getMainExecutor(applicationContext)

    private var activeRecording: Recording? = null
    private var startDeferred: CompletableDeferred<RecordingCommandResult>? = null
    private var finalizeDeferred: CompletableDeferred<VideoRecordingResult>? = null

    private var recordingSegmentStartedAtMs: Long? = null

    private var accumulatedRecordingMs = 0L

    override fun observeCapabilities(): Flow<CameraCapabilities> {
        return cameraXSession.capabilities.filterNotNull()
    }

    override fun observeRecordingState(): Flow<RecordingState> = recordingState.asStateFlow()

    override suspend fun setLens(lens: CameraLens): CameraOperationResult {
        return cameraXSession.switchLens(lens).toDomainResult()
    }

    override suspend fun setFlashMode(flashMode: FlashMode): CameraOperationResult {
        return cameraXSession.setFlashMode(flashMode).toDomainResult()
    }

    override suspend fun capturePhoto(): PhotoCaptureResult {
        if (cameraXSession.videoCaptureUseCase != null) {
            when (cameraXSession.bindPhoto()) {
                CameraSessionBindResult.SUCCESS -> Unit

                CameraSessionBindResult.CAMERA_UNAVAILABLE,
                CameraSessionBindResult.LENS_UNAVAILABLE,
                CameraSessionBindResult.INVALID_STATE -> {
                    return PhotoCaptureResult.Failed(PhotoCaptureFailure.CAMERA_UNAVAILABLE)
                }

                CameraSessionBindResult.UNKNOWN -> {
                    return PhotoCaptureResult.Failed(PhotoCaptureFailure.UNKNOWN)
                }
            }
        }

        if (cameraXSession.boundCamera == null) {
            return PhotoCaptureResult.Failed(PhotoCaptureFailure.CAMERA_UNAVAILABLE)
        }

        return try {
            val mediaId = photoStorage.savePhoto(createPhotoName()) { outputStream ->
                val outputOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()
                cameraXSession.imageCaptureUseCase.takePicture(outputOptions)
            }

            PhotoCaptureResult.Saved(mediaId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: ImageCaptureException) {
            PhotoCaptureResult.Failed(error.toPhotoCaptureFailure())
        } catch (_: PhotoStorageException) {
            PhotoCaptureResult.Failed(PhotoCaptureFailure.STORAGE_UNAVAILABLE)
        } catch (_: SecurityException) {
            PhotoCaptureResult.Failed(PhotoCaptureFailure.STORAGE_UNAVAILABLE)
        } catch (_: IOException) {
            PhotoCaptureResult.Failed(PhotoCaptureFailure.STORAGE_UNAVAILABLE)
        } catch (_: Exception) {
            PhotoCaptureResult.Failed(PhotoCaptureFailure.UNKNOWN)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun startVideoRecording(request: VideoRecordingRequest): RecordingCommandResult {
        return recordingMutex.withLock {
            if (activeRecording != null) {
                return@withLock RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)
            }

            if (recordingState.value is RecordingState.Failed) {
                recordingState.value = RecordingState.Idle
            }

            if (recordingState.value != RecordingState.Idle) {
                return@withLock RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)
            }

            when (cameraXSession.bindVideo(request.quality)) {
                VideoSessionBindResult.SUCCESS -> Unit
                VideoSessionBindResult.QUALITY_UNAVAILABLE ->
                    return@withLock RecordingCommandResult.Failed(RecordingFailure.QUALITY_UNAVAILABLE)

                VideoSessionBindResult.CAMERA_UNAVAILABLE ->
                    return@withLock RecordingCommandResult.Failed(RecordingFailure.CAMERA_UNAVAILABLE)

                VideoSessionBindResult.INVALID_STATE ->
                    return@withLock RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)

                VideoSessionBindResult.UNKNOWN ->
                    return@withLock RecordingCommandResult.Failed(RecordingFailure.START_FAILED)
            }

            val videoCapture = cameraXSession.videoCaptureUseCase
                ?: return@withLock RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)

            val destination = videoStorage.createDestination(createVideoName())
            val outputOptions = MediaStoreOutputOptions.Builder(
                destination.contentResolver,
                destination.collectionUri
            ).setContentValues(destination.contentValues).build()

            val startResult = CompletableDeferred<RecordingCommandResult>()
            val finalResult = CompletableDeferred<VideoRecordingResult>()

            startDeferred = startResult
            finalizeDeferred = finalResult
            recordingState.value = RecordingState.Preparing

            try {
                var pendingRecording =
                    videoCapture.output.prepareRecording(applicationContext, outputOptions)

                if (request.recordAudio) {
                    pendingRecording = pendingRecording.withAudioEnabled()
                }

                activeRecording =
                    pendingRecording.start(recordingEventExecutor, ::handleVideoRecordEvent)

                startResult.await()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: SecurityException) {
                resetRecordingAfterStartFailure(RecordingFailure.AUDIO_PERMISSION_DENIED)
                RecordingCommandResult.Failed(RecordingFailure.AUDIO_PERMISSION_DENIED)
            } catch (_: IllegalStateException) {
                resetRecordingAfterStartFailure(RecordingFailure.INVALID_STATE)
                RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)
            } catch (_: Exception) {
                resetRecordingAfterStartFailure(RecordingFailure.START_FAILED)
                RecordingCommandResult.Failed(RecordingFailure.START_FAILED)
            }
        }
    }

    override suspend fun pauseVideoRecording(): RecordingCommandResult {
        return recordingMutex.withLock {
            if (recordingState.value !is RecordingState.Recording) {
                return@withLock RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)
            }

            val recording = activeRecording
                ?: return@withLock RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)

            try {
                recording.pause()
                RecordingCommandResult.Success
            } catch (_: IllegalStateException) {
                RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)
            } catch (_: Exception) {
                RecordingCommandResult.Failed(RecordingFailure.UNKNOWN)
            }
        }
    }

    override suspend fun resumeVideoRecording(): RecordingCommandResult {
        return recordingMutex.withLock {
            if (recordingState.value !is RecordingState.Paused) {
                return@withLock RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)
            }

            val recording = activeRecording
                ?: return@withLock RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)

            try {
                recording.resume()
                RecordingCommandResult.Success
            } catch (_: IllegalStateException) {
                RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)
            } catch (_: Exception) {
                RecordingCommandResult.Failed(RecordingFailure.UNKNOWN)
            }
        }
    }

    override suspend fun stopVideoRecording(): VideoRecordingResult {
        return recordingMutex.withLock {
            val recording = activeRecording
                ?: return@withLock VideoRecordingResult.Failed(RecordingFailure.INVALID_STATE)

            val completion = finalizeDeferred
                ?: return@withLock VideoRecordingResult.Failed(RecordingFailure.INVALID_STATE)

            if (recordingState.value !is RecordingState.Recording &&
                recordingState.value !is RecordingState.Paused
            ) {
                return@withLock VideoRecordingResult.Failed(RecordingFailure.INVALID_STATE)
            }

            recordingState.value = RecordingState.Finalizing

            try {
                recording.stop()
                completion.await()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                val failure = RecordingFailure.FINALIZATION_FAILED

                activeRecording = null
                recordingState.value = RecordingState.Failed(failure)
                startDeferred?.complete(RecordingCommandResult.Failed(failure))
                finalizeDeferred?.complete(VideoRecordingResult.Failed(failure))
                startDeferred = null
                finalizeDeferred = null
                resetRecordingClock()

                VideoRecordingResult.Failed(failure)
            }
        }
    }

    override suspend fun setTorchEnabled(enabled: Boolean): CameraOperationResult {
        return cameraXSession.setTorchEnabled(enabled).toDomainResult()
    }

    private fun createPhotoName(): String = "PETCAM_${System.currentTimeMillis()}.jpg"

    private fun ImageCaptureException.toPhotoCaptureFailure(): PhotoCaptureFailure {
        return when (imageCaptureError) {
            ImageCapture.ERROR_FILE_IO -> PhotoCaptureFailure.STORAGE_UNAVAILABLE
            ImageCapture.ERROR_CAMERA_CLOSED,
            ImageCapture.ERROR_INVALID_CAMERA -> PhotoCaptureFailure.CAMERA_UNAVAILABLE

            ImageCapture.ERROR_CAPTURE_FAILED -> PhotoCaptureFailure.CAPTURE_FAILED
            else -> PhotoCaptureFailure.UNKNOWN
        }
    }

    private fun CameraSessionBindResult.toDomainResult(): CameraOperationResult {
        return when (this) {
            CameraSessionBindResult.SUCCESS -> CameraOperationResult.Success
            CameraSessionBindResult.CAMERA_UNAVAILABLE ->
                CameraOperationResult.Failed(CameraOperationFailure.CAMERA_UNAVAILABLE)

            CameraSessionBindResult.LENS_UNAVAILABLE ->
                CameraOperationResult.Failed(CameraOperationFailure.LENS_UNAVAILABLE)

            CameraSessionBindResult.INVALID_STATE ->
                CameraOperationResult.Failed(CameraOperationFailure.INVALID_STATE)

            CameraSessionBindResult.UNKNOWN ->
                CameraOperationResult.Failed(CameraOperationFailure.UNKNOWN)
        }
    }

    private fun CameraControlResult.toDomainResult(): CameraOperationResult {
        return when (this) {
            CameraControlResult.SUCCESS -> CameraOperationResult.Success
            CameraControlResult.FLASH_UNAVAILABLE ->
                CameraOperationResult.Failed(CameraOperationFailure.FLASH_UNAVAILABLE)

            CameraControlResult.INVALID_STATE ->
                CameraOperationResult.Failed(CameraOperationFailure.INVALID_STATE)

            CameraControlResult.FOCUS_UNSUPPORTED,
            CameraControlResult.UNKNOWN ->
                CameraOperationResult.Failed(CameraOperationFailure.UNKNOWN)
        }
    }

    private fun handleVideoRecordEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> {
                startRecordingClock()
                recordingState.value = RecordingState.Recording(0L)
                startDeferred?.complete(RecordingCommandResult.Success)
                startDeferred = null
            }

            is VideoRecordEvent.Status -> {
                when (recordingState.value) {
                    is RecordingState.Recording ->
                        recordingState.value = RecordingState.Recording(currentRecordingElapsedMs())

                    is RecordingState.Paused ->
                        recordingState.value = RecordingState.Paused(accumulatedRecordingMs)

                    else -> Unit
                }
            }

            is VideoRecordEvent.Pause -> {
                recordingState.value = RecordingState.Paused(pauseRecordingClock())
            }

            is VideoRecordEvent.Resume -> {
                resumeRecordingClock()
                recordingState.value = RecordingState.Recording(accumulatedRecordingMs)
            }

            is VideoRecordEvent.Finalize -> handleFinalize(event)
        }
    }

    private fun handleFinalize(event: VideoRecordEvent.Finalize) {
        activeRecording = null

        val result = if (!event.hasError()) {
            val uri = event.outputResults.outputUri

            if (uri != Uri.EMPTY) {
                recordingState.value = RecordingState.Idle
                VideoRecordingResult.Saved(MediaId(uri.toString()))
            } else {
                val failure = RecordingFailure.FINALIZATION_FAILED
                recordingState.value = RecordingState.Failed(failure)
                VideoRecordingResult.Failed(failure)
            }
        } else {
            val failure = event.toRecordingFailure()
            cleanupBrokenOutputIfNeeded(event)
            recordingState.value = RecordingState.Failed(failure)
            VideoRecordingResult.Failed(failure)
        }

        resetRecordingClock()

        startDeferred?.complete(
            when (result) {
                is VideoRecordingResult.Saved -> RecordingCommandResult.Success
                is VideoRecordingResult.Failed -> RecordingCommandResult.Failed(result.failure)
            }
        )

        finalizeDeferred?.complete(result)

        startDeferred = null
        finalizeDeferred = null
    }

    private fun VideoRecordEvent.Finalize.toRecordingFailure(): RecordingFailure {
        return when (error) {
            VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE,
            VideoRecordEvent.Finalize.ERROR_INVALID_OUTPUT_OPTIONS -> RecordingFailure.STORAGE_UNAVAILABLE

            VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE -> RecordingFailure.CAMERA_UNAVAILABLE

            VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED,
            VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR,
            VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA,
            VideoRecordEvent.Finalize.ERROR_RECORDING_GARBAGE_COLLECTED,
            VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED,
            VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED -> RecordingFailure.FINALIZATION_FAILED

            else -> RecordingFailure.UNKNOWN
        }
    }

    private fun cleanupBrokenOutputIfNeeded(event: VideoRecordEvent.Finalize) {
        val shouldDelete = when (event.error) {
            VideoRecordEvent.Finalize.ERROR_UNKNOWN,
            VideoRecordEvent.Finalize.ERROR_RECORDER_ERROR,
            VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED,
            VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA,
            VideoRecordEvent.Finalize.ERROR_INVALID_OUTPUT_OPTIONS -> true

            else -> false
        }

        if (!shouldDelete) return

        val uri = event.outputResults.outputUri
        if (uri != Uri.EMPTY) videoStorage.delete(MediaId(uri.toString()))
    }

    private fun resetRecordingAfterStartFailure(failure: RecordingFailure) {
        activeRecording = null
        startDeferred = null
        finalizeDeferred = null
        recordingState.value = RecordingState.Failed(failure)
        resetRecordingClock()
    }

    private fun createVideoName(): String = "PETCAM_${System.currentTimeMillis()}.mp4"

    private fun startRecordingClock() {
        accumulatedRecordingMs = 0L
        recordingSegmentStartedAtMs = SystemClock.elapsedRealtime()
    }

    private fun currentRecordingElapsedMs(): Long {
        val startedAt = recordingSegmentStartedAtMs ?: return accumulatedRecordingMs
        return accumulatedRecordingMs + (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
    }

    private fun pauseRecordingClock(): Long {
        accumulatedRecordingMs = currentRecordingElapsedMs()
        recordingSegmentStartedAtMs = null
        return accumulatedRecordingMs
    }

    private fun resumeRecordingClock() {
        recordingSegmentStartedAtMs = SystemClock.elapsedRealtime()
    }

    private fun resetRecordingClock() {
        accumulatedRecordingMs = 0L
        recordingSegmentStartedAtMs = null
    }
}