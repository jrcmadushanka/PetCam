package com.civdevops.petcam.core.model.settings

import com.civdevops.petcam.core.model.audio.PetSoundCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AudioSettingsTest {

    @Test
    fun `custom volume accepts minimum value`() {
        val settings = createSettings(
            customVolumePercent = 0,
        )

        assertEquals(0, settings.customVolumePercent)
    }

    @Test
    fun `custom volume accepts maximum value`() {
        val settings = createSettings(
            customVolumePercent = 100,
        )

        assertEquals(100, settings.customVolumePercent)
    }

    @Test
    fun `custom volume rejects value below minimum`() {
        assertFailsWith<IllegalArgumentException> {
            createSettings(
                customVolumePercent = -1,
            )
        }
    }

    @Test
    fun `custom volume rejects value above maximum`() {
        assertFailsWith<IllegalArgumentException> {
            createSettings(
                customVolumePercent = 101,
            )
        }
    }

    private fun createSettings(
        customVolumePercent: Int,
    ) = AudioSettings(
        defaultCategory = PetSoundCategory("dogs"),
        volumeMode = PetSoundVolumeMode.Custom,
        customVolumePercent = customVolumePercent,
        loopDuringRecording = false,
        playOnPhotoCapture = true,
    )
}