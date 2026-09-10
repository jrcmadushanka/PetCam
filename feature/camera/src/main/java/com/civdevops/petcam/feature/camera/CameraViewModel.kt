package com.civdevops.petcam.feature.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSound
import com.civdevops.petcam.core.model.audio.PetSoundCategories
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.RecordingFailure
import com.civdevops.petcam.core.model.camera.RecordingState
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.domain.audio.AttentionSoundFailure
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackResult
import com.civdevops.petcam.domain.camera.CameraOperationResult
import com.civdevops.petcam.domain.camera.PhotoCaptureFailure
import com.civdevops.petcam.domain.camera.PhotoCaptureResult
import com.civdevops.petcam.domain.camera.RecordingCommandResult
import com.civdevops.petcam.domain.camera.VideoRecordingRequest
import com.civdevops.petcam.domain.camera.VideoRecordingResult
import com.civdevops.petcam.domain.usecase.audio.ObservePlayablePetSoundsUseCase
import com.civdevops.petcam.domain.usecase.audio.PlayAttentionSoundUseCase
import com.civdevops.petcam.domain.usecase.audio.StopAttentionSoundUseCase
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private val setTorchEnabledUseCase: SetTorchEnabledUseCase,
    private val observePlayablePetSoundsUseCase: ObservePlayablePetSoundsUseCase,
    private val playAttentionSoundUseCase: PlayAttentionSoundUseCase,
    private val stopAttentionSoundUseCase: StopAttentionSoundUseCase,
) : ViewModel() {

    private val appSettings = observeSettingsUseCase()
    private val capabilities = MutableStateFlow<CameraCapabilities?>(null)
    private val lensOverride = MutableStateFlow<CameraLens?>(null)
    private val flashOverride = MutableStateFlow<FlashMode?>(null)
    private val modeOverride = MutableStateFlow<CaptureMode?>(null)
    private val microphonePermissionGranted = MutableStateFlow(false)
    private val photoCaptureState = MutableStateFlow<PhotoCaptureState>(PhotoCaptureState.Idle)
    private val videoUiState = MutableStateFlow(VideoUiState())
    private val selectedAttentionCategory = MutableStateFlow<PetSoundCategory?>(null)
    private val selectedAttentionSound = MutableStateFlow<PetSoundId?>(null)
    private val attentionSoundFailure = MutableStateFlow<AttentionSoundFailure?>(null)

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

    private val attentionSoundState = combine(
        observePlayablePetSoundsUseCase(),
        appSettings.map { it.audio.defaultCategory }.distinctUntilChanged(),
        selectedAttentionCategory,
        selectedAttentionSound,
        attentionSoundFailure
    ) { sounds, defaultCategory, categoryOverride, soundOverride, failure ->
        createAttentionSoundState(
            sounds = sounds,
            defaultCategory = defaultCategory,
            categoryOverride = categoryOverride,
            soundOverride = soundOverride,
            failure = failure
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AttentionSoundUiState()
    )

    private val auxiliaryUiState = combine(
        videoUiState,
        attentionSoundState
    ) { video, attention ->
        AuxiliaryUiState(video, attention)
    }

    val uiState = combine(
        configurationState,
        presentationSettings,
        photoCaptureState,
        observeRecordingStateUseCase(),
        auxiliaryUiState
    ) { configuration, presentation, photoCapture, recordingState, auxiliary ->
        CameraUiState(
            configuration = configuration,
            captureMode = presentation.captureMode,
            recordAudio = presentation.recordAudio,
            keepScreenAwakeWhileRecording = presentation.keepScreenAwake,
            photoCapture = photoCapture,
            recordingState = recordingState,
            videoCapture = auxiliary.video.captureState,
            recordingCommandFailure = auxiliary.video.commandFailure,
            videoTorchEnabled = auxiliary.video.torchEnabled,
            attentionSound = auxiliary.attention
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CameraUiState()
    )

    init {
        viewModelScope.launch {
            observeRecordingStateUseCase().collect { state ->
                if (state is RecordingState.Failed ||
                    state == RecordingState.Finalizing) {
                    stopAttentionSoundSafely()
                }
            }
        }
    }

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
            CameraAction.PauseVideoRecording -> pauseVideo()
            CameraAction.ResumeVideoRecording -> resumeVideo()
            CameraAction.StopVideoRecording -> stopVideo()
            CameraAction.ToggleVideoTorch -> toggleVideoTorch()
            is CameraAction.SelectAttentionCategory -> selectAttentionCategory(action.category)
            is CameraAction.SelectAttentionSound -> selectAttentionSound(action.soundId)
            CameraAction.PlayAttentionSound -> previewAttentionSound()
            CameraAction.CameraInactive -> stopAttentionSound()
        }
    }

    private fun setCaptureMode(mode: CaptureMode) {
        val state = uiState.value

        if (state.recordingInProgress) return
        if (state.captureMode == mode) return

        stopAttentionSound()

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

            val audioSettings = appSettings.first().audio

            if (audioSettings.playOnPhotoCapture) {
                playSelectedAttentionSound(audioSettings, loop = false)
            }

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
                RecordingCommandResult.Success -> {
                    val settings = appSettings.first().audio

                    if (settings.loopDuringRecording) {
                        playSelectedAttentionSound(settings, loop = true)
                    }
                }
                is RecordingCommandResult.Failed -> {
                    videoUiState.value = videoUiState.value.copy(
                        captureState = VideoCaptureState.Failed(result.failure),
                        commandFailure = null
                    )
                }
            }
        }
    }

    private fun pauseVideo() {
        viewModelScope.launch {
            when (val result = pauseVideoRecordingUseCase()) {
                RecordingCommandResult.Success -> {
                    videoUiState.value = videoUiState.value.copy(commandFailure = null)
                    stopAttentionSoundSafely()
                }

                is RecordingCommandResult.Failed -> {
                    videoUiState.value = videoUiState.value.copy(
                        commandFailure = result.failure
                    )
                }
            }
        }
    }

    private fun resumeVideo() {
        viewModelScope.launch {
            when (val result = resumeVideoRecordingUseCase()) {
                RecordingCommandResult.Success -> {
                    videoUiState.value = videoUiState.value.copy(commandFailure = null)

                    val settings = appSettings.first().audio

                    if (settings.loopDuringRecording) {
                        playSelectedAttentionSound(settings, loop = true)
                    }
                }

                is RecordingCommandResult.Failed -> {
                    videoUiState.value = videoUiState.value.copy(
                        commandFailure = result.failure
                    )
                }
            }
        }
    }

    private fun stopVideo() {
        viewModelScope.launch {
            stopAttentionSoundSafely()

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

    private fun createAttentionSoundState(
        sounds: List<PetSound>,
        defaultCategory: PetSoundCategory,
        categoryOverride: PetSoundCategory?,
        soundOverride: PetSoundId?,
        failure: AttentionSoundFailure?
    ): AttentionSoundUiState {
        val availableCategories = sounds.map { it.category }.distinct()

        val categories = PetSoundCategories.values.filter { it in availableCategories } +
                availableCategories.filterNot { it in PetSoundCategories.values }

        val requestedCategory = categoryOverride ?: defaultCategory

        val effectiveCategory = requestedCategory.takeIf { it in categories }
            ?: categories.firstOrNull()

        val categorySounds = effectiveCategory
            ?.let { category -> sounds.filter { it.category == category } }
            .orEmpty()

        val effectiveSound = categorySounds.firstOrNull { it.id == soundOverride }
            ?: categorySounds.firstOrNull()

        return AttentionSoundUiState(
            categories = categories,
            selectedCategory = effectiveCategory,
            sounds = categorySounds,
            selectedSoundId = effectiveSound?.id,
            failure = failure
        )
    }

    private fun selectAttentionCategory(category: PetSoundCategory) {
        if (uiState.value.recordingInProgress) return
        if (category !in attentionSoundState.value.categories) return

        selectedAttentionCategory.value = category
        selectedAttentionSound.value = null
        attentionSoundFailure.value = null
        stopAttentionSound()
    }

    private fun selectAttentionSound(soundId: PetSoundId) {
        if (uiState.value.recordingInProgress) return
        if (attentionSoundState.value.sounds.none { it.id == soundId }) return

        selectedAttentionSound.value = soundId
        attentionSoundFailure.value = null
        stopAttentionSound()
    }

    private fun previewAttentionSound() {
        if (uiState.value.recordingInProgress) return

        viewModelScope.launch {
            val settings = appSettings.first().audio
            playSelectedAttentionSound(settings, loop = false)
        }
    }

    private suspend fun playSelectedAttentionSound(
        settings: AudioSettings,
        loop: Boolean
    ) {
        val soundId = attentionSoundState.value.selectedSoundId

        if (soundId == null) {
            attentionSoundFailure.value = AttentionSoundFailure.SOUND_UNAVAILABLE
            return
        }

        attentionSoundFailure.value = null

        try {
            when (val result = playAttentionSoundUseCase(soundId, settings, loop)) {
                AttentionSoundPlaybackResult.Started -> Unit
                is AttentionSoundPlaybackResult.Failed -> {
                    attentionSoundFailure.value = result.failure
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            attentionSoundFailure.value = AttentionSoundFailure.PLAYBACK_FAILED
        }
    }

    private suspend fun stopAttentionSoundSafely() {
        try {
            stopAttentionSoundUseCase()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Cleanup
        }
    }

    private fun stopAttentionSound() {
        viewModelScope.launch {
            stopAttentionSoundSafely()
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

    private data class AuxiliaryUiState(
        val video: VideoUiState,
        val attention: AttentionSoundUiState
    )
}