package com.civdevops.petcam.domain.usecase.settings

import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateAudioSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(
        settings: AudioSettings,
    ) {
        settingsRepository.updateAudioSettings(settings)
    }
}