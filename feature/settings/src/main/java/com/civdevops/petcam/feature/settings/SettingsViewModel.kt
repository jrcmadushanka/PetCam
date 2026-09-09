package com.civdevops.petcam.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.civdevops.petcam.core.model.settings.AppSettings
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.core.model.settings.CameraSettings
import com.civdevops.petcam.core.model.settings.ExperienceSettings
import com.civdevops.petcam.core.model.settings.SharingSettings
import com.civdevops.petcam.domain.usecase.settings.ObserveSettingsUseCase
import com.civdevops.petcam.domain.usecase.settings.UpdateAudioSettingsUseCase
import com.civdevops.petcam.domain.usecase.settings.UpdateCameraSettingsUseCase
import com.civdevops.petcam.domain.usecase.settings.UpdateExperienceSettingsUseCase
import com.civdevops.petcam.domain.usecase.settings.UpdateSharingSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val observeSettingsUseCase: ObserveSettingsUseCase,
    private val updateCameraSettingsUseCase:
    UpdateCameraSettingsUseCase,
    private val updateAudioSettingsUseCase:
    UpdateAudioSettingsUseCase,
    private val updateSharingSettingsUseCase:
    UpdateSharingSettingsUseCase,
    private val updateExperienceSettingsUseCase:
    UpdateExperienceSettingsUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val settings =
        MutableStateFlow<AppSettings?>(null)

    private val selectedSection =
        MutableStateFlow(
            savedStateHandle
                .get<String>(KEY_SELECTED_SECTION)
                ?.let(::restoreSection),
        )

    private val loadFailed =
        MutableStateFlow(false)

    private val saveFailed =
        MutableStateFlow(false)

    private val writeMutex =
        Mutex()

    private var observeJob: Job? = null

    val uiState =
        combine(
            settings,
            selectedSection,
            loadFailed,
            saveFailed,
        ) {
                settingsValue,
                section,
                loadFailedValue,
                saveFailedValue,
            ->
            when {
                settingsValue != null ->
                    SettingsUiState.Content(
                        settings = settingsValue,
                        selectedSection = section,
                        saveFailed = saveFailedValue,
                    )

                loadFailedValue ->
                    SettingsUiState.LoadFailed

                else ->
                    SettingsUiState.Loading
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5_000,
            ),
            initialValue = SettingsUiState.Loading,
        )

    init {
        observeSettings()
    }

    fun onAction(
        action: SettingsAction,
    ) {
        when (action) {
            is SettingsAction.SelectSection ->
                selectSection(action.section)

            SettingsAction.ShowSectionList ->
                selectSection(null)

            SettingsAction.RetryLoad ->
                observeSettings()

            SettingsAction.DismissSaveError ->
                saveFailed.value = false

            is SettingsAction.SetDefaultCaptureMode ->
                updateCamera {
                    it.copy(
                        defaultMode = action.mode,
                    )
                }

            is SettingsAction.SetDefaultLens ->
                updateCamera {
                    it.copy(
                        defaultLens = action.lens,
                    )
                }

            is SettingsAction.SetFlashMode ->
                updateCamera {
                    it.copy(
                        flashMode = action.mode,
                    )
                }

            is SettingsAction.SetVideoQuality ->
                updateCamera {
                    it.copy(
                        videoQuality = action.quality,
                    )
                }

            is SettingsAction.SetRecordAudio ->
                updateCamera {
                    it.copy(
                        recordAudio = action.enabled,
                    )
                }

            is SettingsAction.SetDefaultSoundCategory ->
                updateAudio {
                    it.copy(
                        defaultCategory = action.category,
                    )
                }

            is SettingsAction.SetVolumeMode ->
                updateAudio {
                    it.copy(
                        volumeMode = action.mode,
                    )
                }

            is SettingsAction.SetCustomVolume ->
                updateAudio {
                    it.copy(
                        customVolumePercent =
                            action.percent.coerceIn(
                                minimumValue = 0,
                                maximumValue = 100,
                            ),
                    )
                }

            is SettingsAction.SetLoopDuringRecording ->
                updateAudio {
                    it.copy(
                        loopDuringRecording =
                            action.enabled,
                    )
                }

            is SettingsAction.SetPlayOnPhotoCapture ->
                updateAudio {
                    it.copy(
                        playOnPhotoCapture =
                            action.enabled,
                    )
                }

            is SettingsAction.SetAutoOpenShareAfterCapture ->
                updateSharing {
                    it.copy(
                        autoOpenShareAfterCapture =
                            action.enabled,
                    )
                }

            is SettingsAction.SetPreferredQuickShareTarget ->
                updateSharing {
                    it.copy(
                        preferredQuickShareTarget =
                            action.target,
                    )
                }

            is SettingsAction.SetKeepScreenAwake ->
                updateExperience {
                    it.copy(
                        keepScreenAwakeWhileRecording =
                            action.enabled,
                    )
                }

            is SettingsAction.SetHapticsEnabled ->
                updateExperience {
                    it.copy(
                        hapticsEnabled =
                            action.enabled,
                    )
                }

            is SettingsAction.SetShowOnlyAppMedia ->
                updateExperience {
                    it.copy(
                        showOnlyAppMedia =
                            action.enabled,
                    )
                }

            is SettingsAction.SetConfirmDelete ->
                updateExperience {
                    it.copy(
                        confirmDelete =
                            action.enabled,
                    )
                }
        }
    }

    private fun observeSettings() {
        observeJob?.cancel()

        loadFailed.value = false

        observeJob =
            observeSettingsUseCase()
                .onEach { appSettings ->
                    settings.value = appSettings
                    loadFailed.value = false
                }
                .catch {
                    settings.value = null
                    loadFailed.value = true
                }
                .launchIn(viewModelScope)
    }

    private fun selectSection(
        section: SettingsSection?,
    ) {
        selectedSection.value = section

        if (section == null) {
            savedStateHandle.remove<String>(
                KEY_SELECTED_SECTION,
            )
        } else {
            savedStateHandle[
                KEY_SELECTED_SECTION
            ] = section.name
        }
    }

    private fun updateCamera(
        transform: (CameraSettings) -> CameraSettings,
    ) {
        updateSection(
            current = AppSettings::camera,
            replace = { appSettings, camera ->
                appSettings.copy(
                    camera = camera,
                )
            },
            transform = transform,
            persist = {
                updateCameraSettingsUseCase(it)
            },
        )
    }

    private fun updateAudio(
        transform: (AudioSettings) -> AudioSettings,
    ) {
        updateSection(
            current = AppSettings::audio,
            replace = { appSettings, audio ->
                appSettings.copy(
                    audio = audio,
                )
            },
            transform = transform,
            persist = {
                updateAudioSettingsUseCase(it)
            },
        )
    }

    private fun updateSharing(
        transform: (SharingSettings) -> SharingSettings,
    ) {
        updateSection(
            current = AppSettings::sharing,
            replace = { appSettings, sharing ->
                appSettings.copy(
                    sharing = sharing,
                )
            },
            transform = transform,
            persist = {
                updateSharingSettingsUseCase(it)
            },
        )
    }

    private fun updateExperience(
        transform:
            (ExperienceSettings) -> ExperienceSettings,
    ) {
        updateSection(
            current = AppSettings::experience,
            replace = { appSettings, experience ->
                appSettings.copy(
                    experience = experience,
                )
            },
            transform = transform,
            persist = {
                updateExperienceSettingsUseCase(it)
            },
        )
    }

    private fun <T> updateSection(
        current: (AppSettings) -> T,
        replace: (AppSettings, T) -> AppSettings,
        transform: (T) -> T,
        persist: suspend (T) -> Unit,
    ) {
        viewModelScope.launch {
            writeMutex.withLock {
                val previousSettings =
                    settings.value
                        ?: return@withLock

                val updatedSection =
                    transform(
                        current(previousSettings),
                    )

                val updatedSettings =
                    replace(
                        previousSettings,
                        updatedSection,
                    )

                saveFailed.value = false

                // Optimistic UI update.
                settings.value = updatedSettings

                try {
                    persist(updatedSection)
                } catch (
                    cancellation:
                    CancellationException
                ) {
                    settings.value =
                        previousSettings

                    throw cancellation
                } catch (_: Exception) {
                    settings.value =
                        previousSettings

                    saveFailed.value = true
                }
            }
        }
    }

    private fun restoreSection(
        value: String,
    ): SettingsSection? =
        SettingsSection.entries
            .firstOrNull { section ->
                section.name == value
            }

    private companion object {
        const val KEY_SELECTED_SECTION =
            "selected_settings_section"
    }
}