package com.civdevops.petcam.feature.camera

import com.civdevops.petcam.core.model.MediaId
import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.SoundPackId
import com.civdevops.petcam.core.model.audio.PetSound
import com.civdevops.petcam.core.model.audio.PetSoundCategories
import com.civdevops.petcam.core.model.audio.PetSoundGain
import com.civdevops.petcam.core.model.audio.PetSoundSource
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CameraLensCapabilities
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.RecordingState
import com.civdevops.petcam.core.model.camera.VideoQuality
import com.civdevops.petcam.core.model.settings.AppSettings
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.core.model.settings.CameraSettings
import com.civdevops.petcam.core.model.settings.ExperienceSettings
import com.civdevops.petcam.core.model.settings.PetSoundVolumeMode
import com.civdevops.petcam.core.model.settings.SharingSettings
import com.civdevops.petcam.core.testing.MainDispatcherRule
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackResult
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackState
import com.civdevops.petcam.domain.audio.AttentionSoundPlayer
import com.civdevops.petcam.domain.camera.CameraOperationResult
import com.civdevops.petcam.domain.camera.PhotoCaptureFailure
import com.civdevops.petcam.domain.camera.PhotoCaptureResult
import com.civdevops.petcam.domain.camera.RecordingCommandResult
import com.civdevops.petcam.domain.camera.VideoRecordingRequest
import com.civdevops.petcam.domain.camera.VideoRecordingResult
import com.civdevops.petcam.domain.repository.CameraRepository
import com.civdevops.petcam.domain.repository.PetSoundRepository
import com.civdevops.petcam.domain.repository.SettingsRepository
import com.civdevops.petcam.domain.usecase.audio.ObserveAttentionSoundPlaybackStateUseCase
import com.civdevops.petcam.domain.usecase.audio.ObservePlayablePetSoundsUseCase
import com.civdevops.petcam.domain.usecase.audio.PauseAttentionSoundUseCase
import com.civdevops.petcam.domain.usecase.audio.PlayAttentionSoundUseCase
import com.civdevops.petcam.domain.usecase.audio.ResolveEffectiveSoundGainUseCase
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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `unsupported flash falls back to off`() = runTest {
        val cameraRepository = FakeCameraRepository()
        val viewModel = createViewModel(cameraRepository)

        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onCapabilitiesChanged(testCapabilities())
        advanceUntilIdle()

        val ready = viewModel.uiState.value.configuration as CameraConfigurationState.Ready

        assertEquals(CameraLens.FRONT, ready.lens)
        assertEquals(FlashMode.OFF, ready.flashMode)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `switch lens selects another available lens`() = runTest {
        val viewModel = createViewModel(FakeCameraRepository())

        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onCapabilitiesChanged(testCapabilities())
        advanceUntilIdle()

        viewModel.onAction(CameraAction.SwitchLens)
        advanceUntilIdle()

        val ready = viewModel.uiState.value.configuration as CameraConfigurationState.Ready
        assertEquals(CameraLens.BACK, ready.lens)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `successful capture updates photo state`() = runTest {
        val mediaId = MediaId("content://petcam/test/photo")
        val cameraRepository = FakeCameraRepository(PhotoCaptureResult.Saved(mediaId))
        val viewModel = createViewModel(cameraRepository)

        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onAction(CameraAction.CapturePhoto)
        advanceUntilIdle()

        assertEquals(PhotoCaptureState.Saved(mediaId), viewModel.uiState.value.photoCapture)
        assertEquals(1, cameraRepository.captureCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `capture failure updates photo state`() = runTest {
        val failure = PhotoCaptureFailure.CAPTURE_FAILED
        val cameraRepository = FakeCameraRepository(PhotoCaptureResult.Failed(failure))
        val viewModel = createViewModel(cameraRepository)

        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onAction(CameraAction.CapturePhoto)
        advanceUntilIdle()

        assertEquals(PhotoCaptureState.Failed(failure), viewModel.uiState.value.photoCapture)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `video without audio starts without microphone permission`() = runTest {
        val repository = FakeCameraRepository()
        val viewModel = createViewModel(
            repository, sampleSettings(defaultMode = CaptureMode.VIDEO, recordAudio = false)
        )

        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onCapabilitiesChanged(testCapabilities())
        advanceUntilIdle()

        viewModel.onAction(CameraAction.StartVideoRecording)
        advanceUntilIdle()

        assertEquals(VideoQuality.FHD, repository.startRequest?.quality)
        assertEquals(false, repository.startRequest?.recordAudio)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `video with audio requests microphone permission before recording`() = runTest {
        val repository = FakeCameraRepository()
        val viewModel = createViewModel(
            repository, sampleSettings(defaultMode = CaptureMode.VIDEO, recordAudio = true)
        )

        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onCapabilitiesChanged(testCapabilities())
        advanceUntilIdle()

        val effect = async(start = CoroutineStart.UNDISPATCHED) {
            viewModel.effects.first()
        }

        viewModel.onAction(CameraAction.StartVideoRecording)

        assertEquals(CameraEffect.RequestMicrophonePermission, effect.await())
        assertEquals(null, repository.startRequest)

        viewModel.onMicrophonePermissionChanged(true)
        advanceUntilIdle()

        assertEquals(true, repository.startRequest?.recordAudio)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `mode and lens cannot change while recording`() = runTest {
        val repository = FakeCameraRepository()
        val viewModel = createViewModel(
            repository, sampleSettings(defaultMode = CaptureMode.VIDEO, recordAudio = false)
        )

        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onCapabilitiesChanged(testCapabilities())
        advanceUntilIdle()

        val initialLens =
            (viewModel.uiState.value.configuration as CameraConfigurationState.Ready).lens

        viewModel.onAction(CameraAction.StartVideoRecording)
        advanceUntilIdle()

        viewModel.onAction(CameraAction.SetCaptureMode(CaptureMode.PHOTO))
        viewModel.onAction(CameraAction.SwitchLens)
        advanceUntilIdle()

        assertEquals(CaptureMode.VIDEO, viewModel.uiState.value.captureMode)

        val currentLens =
            (viewModel.uiState.value.configuration as CameraConfigurationState.Ready).lens

        assertEquals(initialLens, currentLens)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `pause resume and stop recording follow valid lifecycle`() = runTest {
        val repository = FakeCameraRepository()
        val viewModel = createViewModel(
            repository, sampleSettings(defaultMode = CaptureMode.VIDEO, recordAudio = false)
        )

        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onCapabilitiesChanged(testCapabilities())
        advanceUntilIdle()

        viewModel.onAction(CameraAction.StartVideoRecording)
        advanceUntilIdle()

        viewModel.onAction(CameraAction.PauseVideoRecording)
        advanceUntilIdle()

        assertEquals(1, repository.pauseCount)
        assertTrue(repository.recordingState.value is RecordingState.Paused)

        viewModel.onAction(CameraAction.ResumeVideoRecording)
        advanceUntilIdle()

        assertEquals(1, repository.resumeCount)
        assertTrue(repository.recordingState.value is RecordingState.Recording)

        viewModel.onAction(CameraAction.StopVideoRecording)
        advanceUntilIdle()

        assertEquals(1, repository.stopCount)
        assertEquals(RecordingState.Idle, repository.recordingState.value)
        assert(viewModel.uiState.value.videoCapture is VideoCaptureState.Saved)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `video flash toggles torch`() = runTest {
        val repository = FakeCameraRepository()
        val viewModel = createViewModel(
            repository, sampleSettings(
                defaultMode = CaptureMode.VIDEO, recordAudio = false, defaultLens = CameraLens.BACK
            )
        )

        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onCapabilitiesChanged(testCapabilities())
        advanceUntilIdle()

        val ready = viewModel.uiState.value.configuration as CameraConfigurationState.Ready
        assertTrue(ready.flashSupported)

        viewModel.onAction(CameraAction.ToggleVideoTorch)
        advanceUntilIdle()

        assertTrue(repository.torchEnabled)
        assertTrue(viewModel.uiState.value.videoTorchEnabled)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `default audio category selects playable sound`() = runTest {
        val viewModel = createViewModel(
            cameraRepository = FakeCameraRepository(), settings = sampleSettings()
        )

        backgroundScope.launch { viewModel.uiState.collect() }
        viewModel.onCapabilitiesChanged(testCapabilities())
        advanceUntilIdle()

        assertEquals(
            PetSoundCategories.Dogs, viewModel.uiState.value.attentionSound.selectedCategory
        )

        assertEquals(
            PetSoundId("starter:dog_01"), viewModel.uiState.value.attentionSound.selectedSoundId
        )
    }

    private fun createViewModel(
        cameraRepository: CameraRepository, settings: AppSettings = sampleSettings()
    ): CameraViewModel {
        val settingsRepository = FakeSettingsRepository(settings)
        val soundRepository = FakePetSoundRepository(testSounds())
        val attentionPlayer = FakeAttentionSoundPlayer()
        val gainUseCase = ResolveEffectiveSoundGainUseCase()

        return CameraViewModel(
            observeSettingsUseCase = ObserveSettingsUseCase(settingsRepository),
            resolveAvailableCameraLensUseCase = ResolveAvailableCameraLensUseCase(),
            resolveEffectiveFlashModeUseCase = ResolveEffectiveFlashModeUseCase(),
            resolveSupportedVideoQualityUseCase = ResolveSupportedVideoQualityUseCase(),
            capturePhotoUseCase = CapturePhotoUseCase(cameraRepository),
            observeRecordingStateUseCase = ObserveRecordingStateUseCase(cameraRepository),
            startVideoRecordingUseCase = StartVideoRecordingUseCase(cameraRepository),
            pauseVideoRecordingUseCase = PauseVideoRecordingUseCase(cameraRepository),
            resumeVideoRecordingUseCase = ResumeVideoRecordingUseCase(cameraRepository),
            stopVideoRecordingUseCase = StopVideoRecordingUseCase(cameraRepository),
            setTorchEnabledUseCase = SetTorchEnabledUseCase(cameraRepository),
            observePlayablePetSoundsUseCase = ObservePlayablePetSoundsUseCase(soundRepository),
            playAttentionSoundUseCase = PlayAttentionSoundUseCase(attentionPlayer, gainUseCase),
            stopAttentionSoundUseCase = StopAttentionSoundUseCase(attentionPlayer),
            observeAttentionSoundPlaybackStateUseCase = ObserveAttentionSoundPlaybackStateUseCase(attentionPlayer),
            pauseAttentionSoundUseCase = PauseAttentionSoundUseCase(attentionPlayer),
            resumeAttentionSoundUseCase = ResumeAttentionSoundUseCase(attentionPlayer),
        )
    }

    private class FakePetSoundRepository(private val sounds: List<PetSound>) : PetSoundRepository {

        override fun observePetSounds(): Flow<List<PetSound>> = flowOf(sounds)

        override suspend fun getPetSound(id: PetSoundId): PetSound? {
            return sounds.firstOrNull { it.id == id }
        }
    }

    private class FakeAttentionSoundPlayer : AttentionSoundPlayer {

        val state = MutableStateFlow<AttentionSoundPlaybackState>(AttentionSoundPlaybackState.Idle)

        var lastSoundId: PetSoundId? = null
        var lastLoop = false
        var playCount = 0
        var pauseCount = 0
        var resumeCount = 0
        var stopCount = 0

        override fun observePlaybackState(): Flow<AttentionSoundPlaybackState> {
            return state
        }

        override suspend fun play(
            soundId: PetSoundId, gain: PetSoundGain, loop: Boolean
        ): AttentionSoundPlaybackResult {
            lastSoundId = soundId
            lastLoop = loop
            playCount++

            state.value = AttentionSoundPlaybackState.Playing(soundId, loop)
            return AttentionSoundPlaybackResult.Started
        }

        override suspend fun pause() {
            val current = state.value as? AttentionSoundPlaybackState.Playing ?: return
            pauseCount++
            state.value = AttentionSoundPlaybackState.Paused(current.soundId, current.looping)
        }

        override suspend fun resume() {
            val current = state.value as? AttentionSoundPlaybackState.Paused ?: return
            resumeCount++
            state.value = AttentionSoundPlaybackState.Playing(current.soundId, current.looping)
        }

        override suspend fun stop() {
            stopCount++
            state.value = AttentionSoundPlaybackState.Idle
        }
    }

    private class FakeCameraRepository(
        var captureResult: PhotoCaptureResult = PhotoCaptureResult.Failed(PhotoCaptureFailure.UNKNOWN)
    ) : CameraRepository {

        val recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)

        var startRequest: VideoRecordingRequest? = null
        var pauseCount = 0
        var resumeCount = 0
        var stopCount = 0

        var stopResult: VideoRecordingResult =
            VideoRecordingResult.Saved(MediaId("content://petcam/test/video"))
        var captureCount = 0

        var torchEnabled = false

        override fun observeCapabilities(): Flow<CameraCapabilities> = flowOf(testCapabilities())
        override fun observeRecordingState(): Flow<RecordingState> = recordingState
        override suspend fun setLens(lens: CameraLens) = CameraOperationResult.Success
        override suspend fun setFlashMode(flashMode: FlashMode) = CameraOperationResult.Success

        override suspend fun capturePhoto(): PhotoCaptureResult {
            captureCount++
            return captureResult
        }

        override suspend fun startVideoRecording(request: VideoRecordingRequest): RecordingCommandResult {
            startRequest = request
            recordingState.value = RecordingState.Recording(0)
            return RecordingCommandResult.Success
        }

        override suspend fun pauseVideoRecording(): RecordingCommandResult {
            pauseCount++
            val elapsed = (recordingState.value as? RecordingState.Recording)?.elapsedMillis ?: 0
            recordingState.value = RecordingState.Paused(elapsed)
            return RecordingCommandResult.Success
        }

        override suspend fun resumeVideoRecording(): RecordingCommandResult {
            resumeCount++
            val elapsed = (recordingState.value as? RecordingState.Paused)?.elapsedMillis ?: 0
            recordingState.value = RecordingState.Recording(elapsed)
            return RecordingCommandResult.Success
        }

        override suspend fun stopVideoRecording(): VideoRecordingResult {
            stopCount++
            recordingState.value = RecordingState.Idle
            return stopResult
        }

        override suspend fun setTorchEnabled(enabled: Boolean): CameraOperationResult {
            torchEnabled = enabled
            return CameraOperationResult.Success
        }
    }

    private class FakeSettingsRepository(initial: AppSettings) : SettingsRepository {

        private val settings = MutableStateFlow(initial)

        override fun observeSettings(): Flow<AppSettings> = settings

        override suspend fun updateCameraSettings(settings: CameraSettings) {
            this.settings.value = this.settings.value.copy(camera = settings)
        }

        override suspend fun updateAudioSettings(settings: AudioSettings) = Unit
        override suspend fun updateSharingSettings(settings: SharingSettings) = Unit
        override suspend fun updateExperienceSettings(settings: ExperienceSettings) = Unit
    }

    private companion object {

        fun testSounds(): List<PetSound> {
            val packId = SoundPackId("starter")

            return listOf(
                PetSound(
                    id = PetSoundId("starter:dog_01"),
                    packId = packId,
                    category = PetSoundCategories.Dogs,
                    name = "Dog",
                    source = PetSoundSource.Bundled
                ), PetSound(
                    id = PetSoundId("starter:cat_01"),
                    packId = packId,
                    category = PetSoundCategories.Cats,
                    name = "Cat",
                    source = PetSoundSource.Bundled
                )
            )
        }

        fun testCapabilities() = CameraCapabilities(
            mapOf(
                CameraLens.BACK to CameraLensCapabilities(
                    flashSupported = true,
                    supportedVideoQualities = setOf(VideoQuality.FHD, VideoQuality.HD)
                ), CameraLens.FRONT to CameraLensCapabilities(
                    flashSupported = false,
                    supportedVideoQualities = setOf(VideoQuality.FHD, VideoQuality.HD)
                )
            )
        )

        fun sampleSettings(
            defaultMode: CaptureMode = CaptureMode.PHOTO,
            recordAudio: Boolean = true,
            defaultLens: CameraLens = CameraLens.FRONT
        ) = AppSettings(
            camera = CameraSettings(
                defaultMode = defaultMode,
                defaultLens = defaultLens,
                flashMode = FlashMode.AUTO,
                videoQuality = VideoQuality.FHD,
                recordAudio = recordAudio
            ), audio = AudioSettings(
                defaultCategory = PetSoundCategories.Dogs,
                volumeMode = PetSoundVolumeMode.FollowDevice,
                customVolumePercent = 75,
                loopDuringRecording = false,
                playOnPhotoCapture = true
            ), sharing = SharingSettings(
                autoOpenShareAfterCapture = false, preferredQuickShareTarget = null
            ), experience = ExperienceSettings(
                keepScreenAwakeWhileRecording = true,
                hapticsEnabled = true,
                showOnlyAppMedia = true,
                confirmDelete = true
            )
        )
    }
}