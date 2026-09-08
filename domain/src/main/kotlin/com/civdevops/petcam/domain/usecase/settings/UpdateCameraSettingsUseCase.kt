package com.civdevops.petcam.domain.usecase.settings

import com.civdevops.petcam.core.model.settings.CameraSettings
import com.civdevops.petcam.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateCameraSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(
        settings: CameraSettings,
    ) {
        settingsRepository.updateCameraSettings(settings)
    }
}