package com.civdevops.petcam.domain.repository

import com.civdevops.petcam.core.model.settings.AppSettings
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.core.model.settings.CameraSettings
import com.civdevops.petcam.core.model.settings.ExperienceSettings
import com.civdevops.petcam.core.model.settings.SharingSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    fun observeSettings(): Flow<AppSettings>

    suspend fun updateCameraSettings(
        settings: CameraSettings,
    )

    suspend fun updateAudioSettings(
        settings: AudioSettings,
    )

    suspend fun updateSharingSettings(
        settings: SharingSettings,
    )

    suspend fun updateExperienceSettings(
        settings: ExperienceSettings,
    )
}