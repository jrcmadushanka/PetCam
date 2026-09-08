package com.civdevops.petcam.domain.usecase.settings

import com.civdevops.petcam.core.model.settings.SharingSettings
import com.civdevops.petcam.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateSharingSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(
        settings: SharingSettings,
    ) {
        settingsRepository.updateSharingSettings(settings)
    }
}