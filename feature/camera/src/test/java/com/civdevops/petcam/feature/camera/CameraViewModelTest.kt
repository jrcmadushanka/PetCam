package com.civdevops.petcam.feature.camera

import com.civdevops.petcam.core.model.MediaId
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.camera.*
import com.civdevops.petcam.core.model.settings.*
import com.civdevops.petcam.core.testing.MainDispatcherRule
import com.civdevops.petcam.domain.camera.*
import com.civdevops.petcam.domain.repository.CameraRepository
import com.civdevops.petcam.domain.repository.SettingsRepository
import com.civdevops.petcam.domain.usecase.camera.CapturePhotoUseCase
import com.civdevops.petcam.domain.usecase.camera.ResolveAvailableCameraLensUseCase
import com.civdevops.petcam.domain.usecase.camera.ResolveEffectiveFlashModeUseCase
import com.civdevops.petcam.domain.usecase.settings.ObserveSettingsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private fun createViewModel(cameraRepository: CameraRepository): CameraViewModel {
        val settingsRepository = FakeSettingsRepository(sampleSettings())

        return CameraViewModel(
            observeSettingsUseCase = ObserveSettingsUseCase(settingsRepository),
            resolveAvailableCameraLensUseCase = ResolveAvailableCameraLensUseCase(),
            resolveEffectiveFlashModeUseCase = ResolveEffectiveFlashModeUseCase(),
            capturePhotoUseCase = CapturePhotoUseCase(cameraRepository)
        )
    }

    private class FakeCameraRepository(
        var captureResult: PhotoCaptureResult = PhotoCaptureResult.Failed(PhotoCaptureFailure.UNKNOWN)
    ) : CameraRepository {

        var captureCount = 0

        override fun observeCapabilities(): Flow<CameraCapabilities> = flowOf(testCapabilities())
        override fun observeRecordingState(): Flow<RecordingState> = flowOf(RecordingState.Idle)
        override suspend fun setLens(lens: CameraLens) = CameraOperationResult.Success
        override suspend fun setFlashMode(flashMode: FlashMode) = CameraOperationResult.Success

        override suspend fun capturePhoto(): PhotoCaptureResult {
            captureCount++
            return captureResult
        }

        override suspend fun startVideoRecording(request: VideoRecordingRequest) =
            RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)

        override suspend fun pauseVideoRecording() =
            RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)

        override suspend fun resumeVideoRecording() =
            RecordingCommandResult.Failed(RecordingFailure.INVALID_STATE)

        override suspend fun stopVideoRecording() =
            VideoRecordingResult.Failed(RecordingFailure.INVALID_STATE)
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

        fun testCapabilities() = CameraCapabilities(
            mapOf(
                CameraLens.BACK to CameraLensCapabilities(
                    flashSupported = true,
                    supportedVideoQualities = emptySet()
                ),
                CameraLens.FRONT to CameraLensCapabilities(
                    flashSupported = false,
                    supportedVideoQualities = emptySet()
                )
            )
        )

        fun sampleSettings() = AppSettings(
            camera = CameraSettings(
                defaultMode = CaptureMode.PHOTO,
                defaultLens = CameraLens.FRONT,
                flashMode = FlashMode.AUTO,
                videoQuality = VideoQuality.FHD,
                recordAudio = true
            ),
            audio = AudioSettings(
                defaultCategory = PetSoundCategory("dogs"),
                volumeMode = PetSoundVolumeMode.FollowDevice,
                customVolumePercent = 75,
                loopDuringRecording = false,
                playOnPhotoCapture = true
            ),
            sharing = SharingSettings(
                autoOpenShareAfterCapture = false,
                preferredQuickShareTarget = null
            ),
            experience = ExperienceSettings(
                keepScreenAwakeWhileRecording = true,
                hapticsEnabled = true,
                showOnlyAppMedia = true,
                confirmDelete = true
            )
        )
    }
}