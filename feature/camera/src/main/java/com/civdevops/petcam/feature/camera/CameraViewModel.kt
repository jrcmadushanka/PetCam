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
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackState
import com.civdevops.petcam.domain.camera.CameraOperationResult
import com.civdevops.petcam.domain.camera.PhotoCaptureFailure
import com.civdevops.petcam.domain.camera.PhotoCaptureResult
import com.civdevops.petcam.domain.camera.RecordingCommandResult
import com.civdevops.petcam.domain.camera.VideoRecordingRequest
import com.civdevops.petcam.domain.camera.VideoRecordingResult
import com.civdevops.petcam.domain.usecase.audio.ObserveAttentionSoundPlaybackStateUseCase
import com.civdevops.petcam.domain.usecase.audio.ObservePlayablePetSoundsUseCase
import com.civdevops.petcam.domain.usecase.audio.PauseAttentionSoundUseCase
import com.civdevops.petcam.domain.usecase.audio.PlayAttentionSoundUseCase
import com.civdevops.petcam.domain.usecase.audio.ResumeAttentionSoundUseCase
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
import kotlinx.coroutines.Job
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
    private val observeAttentionSoundPlaybackStateUseCase: ObserveAttentionSoundPlaybackStateUseCase,
    private val pauseAttentionSoundUseCase: PauseAttentionSoundUseCase,
    private val resumeAttentionSoundUseCase: ResumeAttentionSoundUseCase,
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
    private val photoShutterPressed = MutableStateFlow(false)
    private var photoShutterSoundJob: Job? = null

    private var attentionPauseOrigin: AttentionPauseOrigin? = null
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

    private val attentionSelectionState = combine(
        observePlayablePetSoundsUseCase(),
        appSettings.map { it.audio.defaultCategory }.distinctUntilChanged(),
        selectedAttentionCategory,
        selectedAttentionSound
    ) { sounds, defaultCategory, categoryOverride, soundOverride ->
        createAttentionSoundSelection(
            sounds = sounds,
            defaultCategory = defaultCategory,
            categoryOverride = categoryOverride,
            soundOverride = soundOverride
        )
    }

    private val attentionSoundState = combine(
        attentionSelectionState,
        observeAttentionSoundPlaybackStateUseCase()
    ) { selection, playback ->
        AttentionSoundUiState(
            categories = selection.categories,
            selectedCategory = selection.selectedCategory,
            sounds = selection.sounds,
            selectedSoundId = selection.selectedSoundId,
            playbackState = playback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AttentionSoundUiState()
    )

    private val auxiliaryUiState = combine(
        videoUiState,
        attentionSoundState,
        photoShutterPressed
    ) { video, attention, shutterPressed ->
        AuxiliaryUiState(video, attention, shutterPressed)
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
            attentionSound = auxiliary.attention,
            photoShutterPressed = auxiliary.photoShutterPressed
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
            CameraAction.CameraInactive -> onCameraInactive()
            CameraAction.PhotoShutterPressed -> onPhotoShutterPressed()
            CameraAction.PhotoShutterReleased -> onPhotoShutterReleased()
            CameraAction.PhotoShutterCancelled -> onPhotoShutterCancelled()
            CameraAction.ToggleAttentionSoundPlayback -> toggleAttentionSoundPlayback()
        }
    }

    private fun onCameraInactive() {
        photoShutterPressed.value = false

        photoShutterSoundJob?.cancel()
        photoShutterSoundJob = null

        attentionPauseOrigin = null
        stopAttentionSound()
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

    private fun onPhotoShutterPressed() {
        val state = uiState.value

        if (state.captureMode != CaptureMode.PHOTO || state.recordingInProgress) return
        if (photoCaptureState.value == PhotoCaptureState.Capturing) return
        if (photoShutterPressed.value) return

        photoShutterPressed.value = true
        photoShutterSoundJob?.cancel()

        photoShutterSoundJob = viewModelScope.launch {
            val settings = appSettings.first().audio

            if (photoShutterPressed.value && settings.playOnPhotoCapture) {
                playSelectedAttentionSound(settings, loop = true)
            }
        }
    }

    private fun onPhotoShutterReleased() {
        if (!photoShutterPressed.value) return
        if (photoCaptureState.value == PhotoCaptureState.Capturing) return

        photoShutterPressed.value = false

        photoShutterSoundJob?.cancel()
        photoShutterSoundJob = null

        photoCaptureState.value = PhotoCaptureState.Capturing

        viewModelScope.launch {
            stopAttentionSoundSafely()
            performPhotoCapture()
        }
    }

    private fun onPhotoShutterCancelled() {
        if (!photoShutterPressed.value) return

        photoShutterPressed.value = false

        photoShutterSoundJob?.cancel()
        photoShutterSoundJob = null

        stopAttentionSound()
    }

    private fun capturePhoto() {
        val state = uiState.value

        if (state.captureMode != CaptureMode.PHOTO || state.recordingInProgress) return
        if (photoCaptureState.value == PhotoCaptureState.Capturing) return

        photoCaptureState.value = PhotoCaptureState.Capturing

        viewModelScope.launch {
            val settings = appSettings.first().audio

            if (settings.playOnPhotoCapture) {
                playSelectedAttentionSound(settings, loop = false)
            }

            performPhotoCapture()
        }
    }

    private suspend fun performPhotoCapture() {
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
                        attentionPauseOrigin = null
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

                    if (attentionSoundState.value.playbackState is AttentionSoundPlaybackState.Playing) {
                        pauseAttentionSoundSafely(AttentionPauseOrigin.RECORDING)
                    }
                }

                is RecordingCommandResult.Failed -> {
                    videoUiState.value =
                        videoUiState.value.copy(commandFailure = result.failure)
                }
            }
        }
    }

    private fun resumeVideo() {
        viewModelScope.launch {
            when (val result = resumeVideoRecordingUseCase()) {
                RecordingCommandResult.Success -> {
                    videoUiState.value = videoUiState.value.copy(commandFailure = null)

                    val shouldResume =
                        attentionPauseOrigin == AttentionPauseOrigin.RECORDING &&
                                attentionSoundState.value.playbackState is AttentionSoundPlaybackState.Paused

                    if (shouldResume) {
                        resumeAttentionSoundSafely()
                    }
                }

                is RecordingCommandResult.Failed -> {
                    videoUiState.value =
                        videoUiState.value.copy(commandFailure = result.failure)
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

    private fun createAttentionSoundSelection(
        sounds: List<PetSound>,
        defaultCategory: PetSoundCategory,
        categoryOverride: PetSoundCategory?,
        soundOverride: PetSoundId?
    ): AttentionSoundSelection {
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

        return AttentionSoundSelection(
            categories = categories,
            selectedCategory = effectiveCategory,
            sounds = categorySounds,
            selectedSoundId = effectiveSound?.id
        )
    }

    private fun selectAttentionCategory(category: PetSoundCategory) {
        if (uiState.value.recordingInProgress) return
        if (category !in attentionSoundState.value.categories) return

        stopAttentionSound()
        selectedAttentionCategory.value = category
        selectedAttentionSound.value = null
    }

    private fun selectAttentionSound(soundId: PetSoundId) {
        if (uiState.value.recordingInProgress) return
        if (attentionSoundState.value.sounds.none { it.id == soundId }) return

        stopAttentionSound()
        selectedAttentionSound.value = soundId
    }

    private fun toggleAttentionSoundPlayback() {
        when (attentionSoundState.value.playbackState) {
            AttentionSoundPlaybackState.Idle,
            is AttentionSoundPlaybackState.Failed -> {
                viewModelScope.launch {
                    attentionPauseOrigin = null

                    val settings = appSettings.first().audio
                    val loop = uiState.value.recordingState is RecordingState.Recording &&
                            settings.loopDuringRecording

                    playSelectedAttentionSound(settings, loop)
                }
            }

            is AttentionSoundPlaybackState.Loading -> Unit

            is AttentionSoundPlaybackState.Playing -> {
                viewModelScope.launch {
                    pauseAttentionSoundSafely(AttentionPauseOrigin.USER)
                }
            }

            is AttentionSoundPlaybackState.Paused -> {
                viewModelScope.launch {
                    resumeAttentionSoundSafely()
                }
            }
        }
    }

    private suspend fun playSelectedAttentionSound(
        settings: AudioSettings,
        loop: Boolean
    ) {
        val soundId = attentionSoundState.value.selectedSoundId ?: return

        try {
            playAttentionSoundUseCase(soundId, settings, loop)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Player exposes failure through playback state.
        }
    }

    private suspend fun pauseAttentionSoundSafely(origin: AttentionPauseOrigin) {
        try {
            pauseAttentionSoundUseCase()

            if (attentionSoundState.value.playbackState is AttentionSoundPlaybackState.Paused) {
                attentionPauseOrigin = origin
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Attention audio must never break camera operations.
        }
    }

    private suspend fun resumeAttentionSoundSafely() {
        try {
            resumeAttentionSoundUseCase()
            attentionPauseOrigin = null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Attention audio must never break camera operations.
        }
    }

    private suspend fun stopAttentionSoundSafely() {
        try {
            stopAttentionSoundUseCase()
            attentionPauseOrigin = null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Attention audio must never break camera operations.
        }
    }

    private fun stopAttentionSound() {
        viewModelScope.launch {
            stopAttentionSoundSafely()
        }
    }

    private fun previewAttentionSound() {
        if (uiState.value.recordingInProgress) return

        viewModelScope.launch {
            val settings = appSettings.first().audio
            playSelectedAttentionSound(settings, loop = false)
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
        val attention: AttentionSoundUiState,
        val photoShutterPressed: Boolean
    )

    private data class AttentionSoundSelection(
        val categories: List<PetSoundCategory>,
        val selectedCategory: PetSoundCategory?,
        val sounds: List<PetSound>,
        val selectedSoundId: PetSoundId?
    )

    private enum class AttentionPauseOrigin {
        USER,
        RECORDING
    }
}