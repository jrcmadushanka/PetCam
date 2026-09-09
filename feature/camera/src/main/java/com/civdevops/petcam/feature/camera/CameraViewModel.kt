package com.civdevops.petcam.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.RecordingFailure
import com.civdevops.petcam.domain.camera.CameraOperationResult
import com.civdevops.petcam.domain.camera.PhotoCaptureFailure
import com.civdevops.petcam.domain.camera.PhotoCaptureResult
import com.civdevops.petcam.domain.camera.RecordingCommandResult
import com.civdevops.petcam.domain.camera.VideoRecordingRequest
import com.civdevops.petcam.domain.camera.VideoRecordingResult
import com.civdevops.petcam.domain.usecase.camera.CapturePhotoUseCase
import com.civdevops.petcam.domain.usecase.camera.ObserveRecordingStateUseCase
import com.civdevops.petcam.domain.usecase.camera.PauseVideoRecordingUseCase
import com.civdevops.petcam.domain.usecase.camera.ResolveAvailableCameraLensUseCase
import com.civdevops.petcam.domain.usecase.camera.ResolveEffectiveFlashModeUseCase
import com.civdevops.petcam.domain.usecase.camera.ResolveSupportedVideoQualityUseCase
import com.civdevops.petcam.domain.usecase.camera.ResumeVideoRecordingUseCase
import com.civdevops.petcam.domain.usecase.camera.SetTorchEnabledUseCase
import com.civdevops.petcam.domain.usecase.camera.StartVideoRecordingUseCase
import com.civdevops.petcam.domain.usecase.camera.StopVideoRecordingUseCase
import com.civdevops.petcam.domain.usecase.settings.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val resolveAvailableCameraLensUseCase: ResolveAvailableCameraLensUseCase,
    private val resolveEffectiveFlashModeUseCase: ResolveEffectiveFlashModeUseCase,
    private val resolveSupportedVideoQualityUseCase: ResolveSupportedVideoQualityUseCase,
    private val capturePhotoUseCase: CapturePhotoUseCase,
    private val observeRecordingStateUseCase: ObserveRecordingStateUseCase,
    private val startVideoRecordingUseCase: StartVideoRecordingUseCase,
    private val pauseVideoRecordingUseCase: PauseVideoRecordingUseCase,
    private val resumeVideoRecordingUseCase: ResumeVideoRecordingUseCase,
    private val stopVideoRecordingUseCase: StopVideoRecordingUseCase,
    private val setTorchEnabledUseCase: SetTorchEnabledUseCase
) : ViewModel() {

    private val appSettings = observeSettingsUseCase()
    private val capabilities = MutableStateFlow<CameraCapabilities?>(null)
    private val lensOverride = MutableStateFlow<CameraLens?>(null)
    private val flashOverride = MutableStateFlow<FlashMode?>(null)
    private val modeOverride = MutableStateFlow<CaptureMode?>(null)
    private val microphonePermissionGranted = MutableStateFlow(false)
    private val photoCaptureState = MutableStateFlow<PhotoCaptureState>(PhotoCaptureState.Idle)
    private val videoUiState = MutableStateFlow(VideoUiState())

    private val _effects = MutableSharedFlow<CameraEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

    private var pendingVideoStart: VideoRecordingRequest? = null

    private val cameraSettings = appSettings.map { it.camera }

    private val configurationState: Flow<CameraConfigurationState> =
        combine(
            cameraSettings,
            capabilities,
            lensOverride,
            flashOverride
        ) { settings, capabilitiesValue, lensOverrideValue, flashOverrideValue ->

            if (capabilitiesValue == null) return@combine CameraConfigurationState.Loading

            val preferredLens = lensOverrideValue ?: settings.defaultLens
            val effectiveLens = resolveAvailableCameraLensUseCase(preferredLens, capabilitiesValue)
            val lensCapabilities = requireNotNull(capabilitiesValue[effectiveLens])
            val preferredFlash = flashOverrideValue ?: settings.flashMode
            val effectiveFlash = resolveEffectiveFlashModeUseCase(preferredFlash, lensCapabilities)
            val effectiveVideoQuality = resolveSupportedVideoQualityUseCase(
                settings.videoQuality,
                lensCapabilities.supportedVideoQualities
            )

            CameraConfigurationState.Ready(
                capabilities = capabilitiesValue,
                lens = effectiveLens,
                flashMode = effectiveFlash,
                videoQuality = effectiveVideoQuality
            )
        }

    private val presentationSettings = combine(appSettings, modeOverride) { settings, mode ->
        CameraPresentationSettings(
            captureMode = mode ?: settings.camera.defaultMode,
            recordAudio = settings.camera.recordAudio,
            keepScreenAwake = settings.experience.keepScreenAwakeWhileRecording
        )
    }

    val uiState = combine(
        configurationState,
        presentationSettings,
        photoCaptureState,
        observeRecordingStateUseCase(),
        videoUiState
    ) { configuration, presentation, photoCapture, recordingState, video ->
        CameraUiState(
            configuration = configuration,
            captureMode = presentation.captureMode,
            recordAudio = presentation.recordAudio,
            keepScreenAwakeWhileRecording = presentation.keepScreenAwake,
            photoCapture = photoCapture,
            recordingState = recordingState,
            videoCapture = video.captureState,
            recordingCommandFailure = video.commandFailure,
            videoTorchEnabled = video.torchEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CameraUiState()
    )

    fun onCapabilitiesChanged(value: CameraCapabilities?) {
        capabilities.value = value
    }

    fun onMicrophonePermissionChanged(granted: Boolean) {
        microphonePermissionGranted.value = granted

        val request = pendingVideoStart ?: return
        pendingVideoStart = null

        if (granted && uiState.value.captureMode == CaptureMode.VIDEO) {
            startVideo(request)
        } else if (!granted) {
            videoUiState.value = videoUiState.value.copy(
                captureState = VideoCaptureState.Failed(RecordingFailure.AUDIO_PERMISSION_DENIED),
                commandFailure = null
            )
        }
    }

    fun onAction(action: CameraAction) {
        when (action) {
            CameraAction.SwitchLens -> switchLens()
            CameraAction.CycleFlash -> cycleFlash()
            CameraAction.CapturePhoto -> capturePhoto()
            is CameraAction.SetCaptureMode -> setCaptureMode(action.mode)
            CameraAction.StartVideoRecording -> requestVideoStart()
            CameraAction.PauseVideoRecording -> runRecordingCommand { pauseVideoRecordingUseCase() }
            CameraAction.ResumeVideoRecording -> runRecordingCommand { resumeVideoRecordingUseCase() }
            CameraAction.StopVideoRecording -> stopVideo()
            CameraAction.ToggleVideoTorch -> toggleVideoTorch()
        }
    }

    private fun setCaptureMode(mode: CaptureMode) {
        val state = uiState.value

        if (state.recordingInProgress) return
        if (state.captureMode == mode) return

        pendingVideoStart = null

        if (mode == CaptureMode.PHOTO && videoUiState.value.torchEnabled) {
            viewModelScope.launch {
                if (setTorchEnabledUseCase(false) == CameraOperationResult.Success) {
                    videoUiState.value = VideoUiState()
                    photoCaptureState.value = PhotoCaptureState.Idle
                    modeOverride.value = CaptureMode.PHOTO
                }
            }
            return
        }

        photoCaptureState.value = PhotoCaptureState.Idle
        videoUiState.value = VideoUiState()
        modeOverride.value = mode
    }

    private fun switchLens() {
        if (uiState.value.recordingInProgress) return

        val ready = uiState.value.configuration as? CameraConfigurationState.Ready ?: return
        val requestedLens = if (ready.lens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        val requestedCapabilities = ready.capabilities[requestedLens] ?: return

        if (videoUiState.value.torchEnabled && !requestedCapabilities.flashSupported) {
            videoUiState.value = videoUiState.value.copy(torchEnabled = false)

            viewModelScope.launch {
                setTorchEnabledUseCase(false)
            }
        }

        lensOverride.value = requestedLens
    }

    private fun cycleFlash() {
        val state = uiState.value
        if (state.recordingInProgress || state.captureMode != CaptureMode.PHOTO) return

        val ready = state.configuration as? CameraConfigurationState.Ready ?: return

        if (!ready.flashSupported) {
            flashOverride.value = FlashMode.OFF
            return
        }

        flashOverride.value = when (ready.flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
    }

    private fun capturePhoto() {
        val state = uiState.value
        if (state.captureMode != CaptureMode.PHOTO || state.recordingInProgress) return
        if (photoCaptureState.value == PhotoCaptureState.Capturing) return

        viewModelScope.launch {
            photoCaptureState.value = PhotoCaptureState.Capturing

            photoCaptureState.value = try {
                when (val result = capturePhotoUseCase()) {
                    is PhotoCaptureResult.Saved -> PhotoCaptureState.Saved(result.mediaId)
                    is PhotoCaptureResult.Failed -> PhotoCaptureState.Failed(result.failure)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                PhotoCaptureState.Failed(PhotoCaptureFailure.UNKNOWN)
            }
        }
    }

    private fun requestVideoStart() {
        val state = uiState.value
        if (state.captureMode != CaptureMode.VIDEO || state.recordingInProgress) return

        val ready = state.configuration as? CameraConfigurationState.Ready ?: return
        val quality = ready.videoQuality

        if (quality == null) {
            videoUiState.value = VideoUiState(
                captureState = VideoCaptureState.Failed(RecordingFailure.QUALITY_UNAVAILABLE)
            )
            return
        }

        val request = VideoRecordingRequest(quality = quality, recordAudio = state.recordAudio)

        if (request.recordAudio && !microphonePermissionGranted.value) {
            pendingVideoStart = request
            _effects.tryEmit(CameraEffect.RequestMicrophonePermission)
            return
        }

        startVideo(request)
    }

    private fun startVideo(request: VideoRecordingRequest) {
        viewModelScope.launch {
            videoUiState.value = videoUiState.value.copy(
                captureState = VideoCaptureState.Idle,
                commandFailure = null
            )

            when (val result = startVideoRecordingUseCase(request)) {
                RecordingCommandResult.Success -> Unit
                is RecordingCommandResult.Failed -> {
                    videoUiState.value = videoUiState.value.copy(
                        captureState = VideoCaptureState.Failed(result.failure),
                        commandFailure = null
                    )
                }
            }
        }
    }

    private fun runRecordingCommand(command: suspend () -> RecordingCommandResult) {
        viewModelScope.launch {
            when (val result = command()) {
                RecordingCommandResult.Success -> {
                    videoUiState.value = videoUiState.value.copy(commandFailure = null)
                }

                is RecordingCommandResult.Failed -> {
                    videoUiState.value = videoUiState.value.copy(commandFailure = result.failure)
                }
            }
        }
    }

    private fun stopVideo() {
        viewModelScope.launch {
            videoUiState.value = videoUiState.value.copy(commandFailure = null)

            when (val result = stopVideoRecordingUseCase()) {
                is VideoRecordingResult.Saved -> {
                    videoUiState.value = videoUiState.value.copy(
                        captureState = VideoCaptureState.Saved(result.mediaId),
                        commandFailure = null
                    )
                }

                is VideoRecordingResult.Failed -> {
                    videoUiState.value = videoUiState.value.copy(
                        captureState = VideoCaptureState.Failed(result.failure),
                        commandFailure = null
                    )
                }
            }
        }
    }

    private fun toggleVideoTorch() {
        val state = uiState.value
        if (state.captureMode != CaptureMode.VIDEO) return

        val ready = state.configuration as? CameraConfigurationState.Ready ?: return
        if (!ready.flashSupported) return

        val enabled = !videoUiState.value.torchEnabled

        viewModelScope.launch {
            when (setTorchEnabledUseCase(enabled)) {
                CameraOperationResult.Success -> {
                    videoUiState.value = videoUiState.value.copy(torchEnabled = enabled)
                }

                is CameraOperationResult.Failed -> {
                    videoUiState.value = videoUiState.value.copy(torchEnabled = false)
                }
            }
        }
    }

    private data class CameraPresentationSettings(
        val captureMode: CaptureMode,
        val recordAudio: Boolean,
        val keepScreenAwake: Boolean
    )

    private data class VideoUiState(
        val captureState: VideoCaptureState = VideoCaptureState.Idle,
        val commandFailure: RecordingFailure? = null,
        val torchEnabled: Boolean = false
    )
}