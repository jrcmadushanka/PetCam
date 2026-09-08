package com.civdevops.petcam.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.VideoQuality
import com.civdevops.petcam.core.model.settings.CameraSettings
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultSettingsRepositoryTest {

    @Test
    fun `empty store emits product defaults`() =
        runTest {
            val repository =
                createRepository()

            val result =
                repository
                    .observeSettings()
                    .first()

            assertEquals(
                SettingsDefaults.app,
                result,
            )
        }

    @Test
    fun `camera settings update is persisted`() =
        runTest {
            val repository =
                createRepository()

            val updatedCameraSettings =
                CameraSettings(
                    defaultMode = CaptureMode.VIDEO,
                    defaultLens = CameraLens.FRONT,
                    flashMode = FlashMode.AUTO,
                    videoQuality = VideoQuality.UHD,
                    recordAudio = false,
                )

            repository.updateCameraSettings(
                updatedCameraSettings,
            )

            val result =
                repository
                    .observeSettings()
                    .first()

            assertEquals(
                updatedCameraSettings,
                result.camera,
            )
        }

    @Test
    fun `camera update does not overwrite audio settings`() =
        runTest {
            val repository =
                createRepository()

            val originalAudio =
                repository
                    .observeSettings()
                    .first()
                    .audio

            repository.updateCameraSettings(
                CameraSettings(
                    defaultMode = CaptureMode.VIDEO,
                    defaultLens = CameraLens.FRONT,
                    flashMode = FlashMode.OFF,
                    videoQuality = VideoQuality.HD,
                    recordAudio = false,
                ),
            )

            val result =
                repository
                    .observeSettings()
                    .first()

            assertEquals(
                originalAudio,
                result.audio,
            )
        }

    private fun TestScope.createRepository():
            DefaultSettingsRepository {

        val directory =
            createTempDirectory(
                prefix = "petcam-settings-",
            ).toFile()

        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = {
                    File(
                        directory,
                        "settings.preferences_pb",
                    )
                },
            )

        return DefaultSettingsRepository(
            dataStore = dataStore,
        )
    }
}