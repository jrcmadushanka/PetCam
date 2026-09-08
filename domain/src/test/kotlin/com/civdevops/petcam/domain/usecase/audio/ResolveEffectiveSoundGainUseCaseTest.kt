package com.civdevops.petcam.domain.usecase.audio

import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.core.model.settings.PetSoundVolumeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveEffectiveSoundGainUseCaseTest {

    private val useCase = ResolveEffectiveSoundGainUseCase()

    @Test
    fun `follow device uses full app gain`() {
        val result = useCase(
            createSettings(
                volumeMode = PetSoundVolumeMode.FollowDevice,
                customVolumePercent = 35,
            ),
        )

        assertEquals(100, result.percent)
    }

    @Test
    fun `maximum app output uses full app gain`() {
        val result = useCase(
            createSettings(
                volumeMode = PetSoundVolumeMode.MaximumAppOutput,
                customVolumePercent = 35,
            ),
        )

        assertEquals(100, result.percent)
    }

    @Test
    fun `custom mode uses configured percentage`() {
        val result = useCase(
            createSettings(
                volumeMode = PetSoundVolumeMode.Custom,
                customVolumePercent = 36,
            ),
        )

        assertEquals(36, result.percent)
    }

    private fun createSettings(
        volumeMode: PetSoundVolumeMode,
        customVolumePercent: Int,
    ) = AudioSettings(
        defaultCategory = PetSoundCategory("dogs"),
        volumeMode = volumeMode,
        customVolumePercent = customVolumePercent,
        loopDuringRecording = false,
        playOnPhotoCapture = true,
    )
}