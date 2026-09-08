package com.civdevops.petcam.domain.usecase.settings

import com.civdevops.petcam.core.model.settings.ExperienceSettings
import com.civdevops.petcam.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateExperienceSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(
        settings: ExperienceSettings,
    ) {
        settingsRepository.updateExperienceSettings(settings)
    }
}