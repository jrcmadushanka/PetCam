package com.civdevops.petcam.domain.usecase.settings

import com.civdevops.petcam.core.model.settings.AppSettings
import com.civdevops.petcam.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke(): Flow<AppSettings> =
        settingsRepository.observeSettings()
}