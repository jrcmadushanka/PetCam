package com.civdevops.petcam.feature.settings

import androidx.lifecycle.SavedStateHandle
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.VideoQuality
import com.civdevops.petcam.core.model.settings.AppSettings
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.core.model.settings.CameraSettings
import com.civdevops.petcam.core.model.settings.ExperienceSettings
import com.civdevops.petcam.core.model.settings.PetSoundVolumeMode
import com.civdevops.petcam.core.model.settings.SharingSettings
import com.civdevops.petcam.core.testing.MainDispatcherRule
import com.civdevops.petcam.domain.repository.SettingsRepository
import com.civdevops.petcam.domain.usecase.settings.ObserveSettingsUseCase
import com.civdevops.petcam.domain.usecase.settings.UpdateAudioSettingsUseCase
import com.civdevops.petcam.domain.usecase.settings.UpdateCameraSettingsUseCase
import com.civdevops.petcam.domain.usecase.settings.UpdateExperienceSettingsUseCase
import com.civdevops.petcam.domain.usecase.settings.UpdateSharingSettingsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `settings flow becomes content state`() =
        runTest {
            val initialSettings =
                sampleSettings()

            val repository =
                FakeSettingsRepository(
                    initialSettings,
                )

            val viewModel =
                createViewModel(repository)

            advanceUntilIdle()

            val state =
                viewModel.uiState.first {
                    it is SettingsUiState.Content
                } as SettingsUiState.Content

            assertEquals(
                initialSettings,
                state.settings,
            )
        }

    @Test
    fun `record audio action updates camera settings`() =
        runTest {
            val repository =
                FakeSettingsRepository(
                    sampleSettings(),
                )

            val viewModel =
                createViewModel(repository)

            viewModel.uiState.first {
                it is SettingsUiState.Content
            }

            viewModel.onAction(
                SettingsAction.SetRecordAudio(
                    enabled = false,
                ),
            )

            advanceUntilIdle()

            assertFalse(
                repository
                    .current
                    .value
                    .camera
                    .recordAudio,
            )
        }

    @Test
    fun `selected section is stored in saved state`() =
        runTest {

            val repository =
                FakeSettingsRepository(
                    sampleSettings(),
                )

            val savedStateHandle =
                SavedStateHandle()

            val viewModel =
                createViewModel(
                    repository = repository,
                    savedStateHandle =
                        savedStateHandle,
                )

            viewModel.onAction(
                SettingsAction.SelectSection(
                    SettingsSection.AUDIO,
                ),
            )

            assertEquals(
                SettingsSection.AUDIO.name,
                savedStateHandle[
                    "selected_settings_section"
                ],
            )
        }

    @Test
    fun `failed update restores previous value and exposes error`() =
        runTest {

            val repository =
                FakeSettingsRepository(
                    initialSettings =
                        sampleSettings(),
                    failCameraUpdates = true,
                )

            val viewModel =
                createViewModel(repository)

            viewModel.uiState.first {
                it is SettingsUiState.Content
            }

            viewModel.onAction(
                SettingsAction.SetRecordAudio(
                    enabled = false,
                ),
            )

            advanceUntilIdle()

            val state =
                viewModel.uiState.first {
                    it is SettingsUiState.Content &&
                            it.saveFailed
                } as SettingsUiState.Content

            assertTrue(
                state.settings
                    .camera
                    .recordAudio,
            )

            assertTrue(
                state.saveFailed,
            )

        }

    private fun createViewModel(
        repository: SettingsRepository,
        savedStateHandle: SavedStateHandle =
            SavedStateHandle(),
    ): SettingsViewModel =
        SettingsViewModel(
            observeSettingsUseCase =
                ObserveSettingsUseCase(
                    repository,
                ),
            updateCameraSettingsUseCase =
                UpdateCameraSettingsUseCase(
                    repository,
                ),
            updateAudioSettingsUseCase =
                UpdateAudioSettingsUseCase(
                    repository,
                ),
            updateSharingSettingsUseCase =
                UpdateSharingSettingsUseCase(
                    repository,
                ),
            updateExperienceSettingsUseCase =
                UpdateExperienceSettingsUseCase(
                    repository,
                ),
            savedStateHandle =
                savedStateHandle,
        )

    private class FakeSettingsRepository(
        initialSettings: AppSettings,
        private val failCameraUpdates:
        Boolean = false,
    ) : SettingsRepository {

        val current =
            MutableStateFlow(
                initialSettings,
            )

        override fun observeSettings():
                Flow<AppSettings> =
            current

        override suspend fun updateCameraSettings(
            settings: CameraSettings,
        ) {
            if (failCameraUpdates) {
                error("Test failure")
            }

            current.value =
                current.value.copy(
                    camera = settings,
                )
        }

        override suspend fun updateAudioSettings(
            settings: AudioSettings,
        ) {
            current.value =
                current.value.copy(
                    audio = settings,
                )
        }

        override suspend fun updateSharingSettings(
            settings: SharingSettings,
        ) {
            current.value =
                current.value.copy(
                    sharing = settings,
                )
        }

        override suspend fun updateExperienceSettings(
            settings: ExperienceSettings,
        ) {
            current.value =
                current.value.copy(
                    experience = settings,
                )
        }
    }

    private fun sampleSettings() =
        AppSettings(
            camera = CameraSettings(
                defaultMode =
                    CaptureMode.PHOTO,
                defaultLens =
                    CameraLens.BACK,
                flashMode =
                    FlashMode.OFF,
                videoQuality =
                    VideoQuality.FHD,
                recordAudio = true,
            ),
            audio = AudioSettings(
                defaultCategory =
                    PetSoundCategory("dogs"),
                volumeMode =
                    PetSoundVolumeMode.FollowDevice,
                customVolumePercent = 75,
                loopDuringRecording = false,
                playOnPhotoCapture = true,
            ),
            sharing = SharingSettings(
                autoOpenShareAfterCapture = false,
                preferredQuickShareTarget = null,
            ),
            experience =
                ExperienceSettings(
                    keepScreenAwakeWhileRecording =
                        true,
                    hapticsEnabled = true,
                    showOnlyAppMedia = true,
                    confirmDelete = true,
                ),
        )
}