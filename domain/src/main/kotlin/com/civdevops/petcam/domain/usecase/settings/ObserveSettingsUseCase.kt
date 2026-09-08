package com.civdevops.petcam.domain.usecase.settings

import com.civdevops.petcam.core.model.settings.AppSettings
import com.civdevops.petcam.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke(): Flow<AppSettings> =
        settingsRepository.observeSettings()
}