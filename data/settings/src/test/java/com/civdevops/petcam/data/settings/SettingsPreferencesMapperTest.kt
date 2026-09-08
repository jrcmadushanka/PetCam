package com.civdevops.petcam.data.settings

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.VideoQuality
import com.civdevops.petcam.core.model.settings.PetSoundVolumeMode
import com.civdevops.petcam.core.model.share.QuickShareTarget
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsPreferencesMapperTest {

    @Test
    fun `empty preferences map to product defaults`() {
        val result =
            SettingsPreferencesMapper.fromPreferences(
                mutablePreferencesOf(),
            )

        assertEquals(
            SettingsDefaults.app,
            result,
        )
    }

    @Test
    fun `stored preferences map to typed settings`() {
        val preferences =
            mutablePreferencesOf().apply {
                this[
                    SettingsPreferencesKeys.defaultCaptureMode
                ] = "video"

                this[
                    SettingsPreferencesKeys.defaultCameraLens
                ] = "front"

                this[
                    SettingsPreferencesKeys.flashMode
                ] = "auto"

                this[
                    SettingsPreferencesKeys.videoQuality
                ] = "uhd"

                this[
                    SettingsPreferencesKeys.recordAudio
                ] = false

                this[
                    SettingsPreferencesKeys.volumeMode
                ] = "custom"

                this[
                    SettingsPreferencesKeys.customVolumePercent
                ] = 42

                this[
                    SettingsPreferencesKeys.preferredQuickShareTarget
                ] = "instagram"
            }

        val result =
            SettingsPreferencesMapper.fromPreferences(
                preferences,
            )

        assertEquals(
            CaptureMode.VIDEO,
            result.camera.defaultMode,
        )

        assertEquals(
            CameraLens.FRONT,
            result.camera.defaultLens,
        )

        assertEquals(
            FlashMode.AUTO,
            result.camera.flashMode,
        )

        assertEquals(
            VideoQuality.UHD,
            result.camera.videoQuality,
        )

        assertEquals(
            false,
            result.camera.recordAudio,
        )

        assertEquals(
            PetSoundVolumeMode.Custom,
            result.audio.volumeMode,
        )

        assertEquals(
            42,
            result.audio.customVolumePercent,
        )

        assertEquals(
            QuickShareTarget.INSTAGRAM,
            result.sharing.preferredQuickShareTarget,
        )
    }

    @Test
    fun `invalid persisted values fall back safely`() {
        val preferences =
            mutablePreferencesOf().apply {
                this[
                    SettingsPreferencesKeys.defaultCaptureMode
                ] = "banana"

                this[
                    SettingsPreferencesKeys.videoQuality
                ] = "16k"

                this[
                    SettingsPreferencesKeys.volumeMode
                ] = "explosion"

                this[
                    SettingsPreferencesKeys.customVolumePercent
                ] = 999

                this[
                    SettingsPreferencesKeys.preferredQuickShareTarget
                ] = "unknown_app"
            }

        val result =
            SettingsPreferencesMapper.fromPreferences(
                preferences,
            )

        assertEquals(
            SettingsDefaults.camera.defaultMode,
            result.camera.defaultMode,
        )

        assertEquals(
            SettingsDefaults.camera.videoQuality,
            result.camera.videoQuality,
        )

        assertEquals(
            SettingsDefaults.audio.volumeMode,
            result.audio.volumeMode,
        )

        assertEquals(
            SettingsDefaults.audio.customVolumePercent,
            result.audio.customVolumePercent,
        )

        assertNull(
            result.sharing.preferredQuickShareTarget,
        )
    }
}