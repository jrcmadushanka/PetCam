package com.civdevops.petcam.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.civdevops.petcam.core.model.settings.AppSettings
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.core.model.settings.CameraSettings
import com.civdevops.petcam.core.model.settings.ExperienceSettings
import com.civdevops.petcam.core.model.settings.SharingSettings
import com.civdevops.petcam.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class DefaultSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map(SettingsPreferencesMapper::fromPreferences)
            .distinctUntilChanged()

    override suspend fun updateCameraSettings(
        settings: CameraSettings,
    ) {
        dataStore.edit { preferences ->
            SettingsPreferencesMapper.writeCameraSettings(
                preferences = preferences,
                settings = settings,
            )
        }
    }

    override suspend fun updateAudioSettings(
        settings: AudioSettings,
    ) {
        dataStore.edit { preferences ->
            SettingsPreferencesMapper.writeAudioSettings(
                preferences = preferences,
                settings = settings,
            )
        }
    }

    override suspend fun updateSharingSettings(
        settings: SharingSettings,
    ) {
        dataStore.edit { preferences ->
            SettingsPreferencesMapper.writeSharingSettings(
                preferences = preferences,
                settings = settings,
            )
        }
    }

    override suspend fun updateExperienceSettings(
        settings: ExperienceSettings,
    ) {
        dataStore.edit { preferences ->
            SettingsPreferencesMapper.writeExperienceSettings(
                preferences = preferences,
                settings = settings,
            )
        }
    }
}