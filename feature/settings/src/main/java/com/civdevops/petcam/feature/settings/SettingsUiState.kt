package com.civdevops.petcam.feature.settings

import com.civdevops.petcam.core.model.settings.AppSettings

sealed interface SettingsUiState {

    data object Loading : SettingsUiState

    data object LoadFailed : SettingsUiState

    data class Content(
        val settings: AppSettings,
        val selectedSection: SettingsSection?,
        val saveFailed: Boolean,
    ) : SettingsUiState
}