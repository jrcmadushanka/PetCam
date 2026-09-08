package com.civdevops.petcam.data.camera

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.takePicture
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.RecordingFailure
import com.civdevops.petcam.core.model.camera.RecordingState
import com.civdevops.petcam.data.media.PhotoStorage
import com.civdevops.petcam.data.media.PhotoStorageException
import com.civdevops.petcam.domain.camera.CameraOperationFailure
import com.civdevops.petcam.domain.camera.CameraOperationResult
import com.civdevops.petcam.domain.camera.PhotoCaptureFailure
import com.civdevops.petcam.domain.camera.PhotoCaptureResult
import com.civdevops.petcam.domain.camera.RecordingCommandResult
import com.civdevops.petcam.domain.camera.VideoRecordingRequest
import com.civdevops.petcam.domain.camera.VideoRecordingResult
import com.civdevops.petcam.domain.repository.CameraRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf

class DefaultCameraRepository @Inject constructor(
    private val cameraXSession: CameraXSession,
    private val photoStorage: PhotoStorage
) : CameraRepository {

    override fun observeCapabilities(): Flow<CameraCapabilities> {
        return cameraXSession.capabilities.filterNotNull()
    }

    override fun observeRecordingState(): Flow<RecordingState> {
        return flowOf(RecordingState.Idle)
    }

    override suspend fun setLens(lens: CameraLens): CameraOperationResult {
        return cameraXSession.switchLens(lens).toDomainResult()
    }

    override suspend fun setFlashMode(flashMode: FlashMode): CameraOperationResult {
        return cameraXSession.setFlashMode(flashMode).toDomainResult()
    }

    override suspend fun capturePhoto(): PhotoCaptureResult {
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

    override suspend fun startVideoRecording(request: VideoRecordingRequest): RecordingCommandResult {
        return RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)
    }

    override suspend fun pauseVideoRecording(): RecordingCommandResult {
        return RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)
    }

    override suspend fun resumeVideoRecording(): RecordingCommandResult {
        return RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)
    }

    override suspend fun stopVideoRecording(): VideoRecordingResult {
        return VideoRecordingResult.Failed(RecordingFailure.INVALID_STATE)
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
}