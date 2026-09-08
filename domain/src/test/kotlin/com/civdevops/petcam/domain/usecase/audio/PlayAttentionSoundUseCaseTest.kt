package com.civdevops.petcam.domain.usecase.audio

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.audio.PetSoundGain
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.core.model.settings.PetSoundVolumeMode
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackResult
import com.civdevops.petcam.domain.audio.AttentionSoundPlayer
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayAttentionSoundUseCaseTest {

    @Test
    fun `custom volume is resolved before playback`() = runBlocking {
        val player = FakeAttentionSoundPlayer()

        val useCase = PlayAttentionSoundUseCase(
            attentionSoundPlayer = player,
            resolveEffectiveSoundGainUseCase =
                ResolveEffectiveSoundGainUseCase(),
        )

        val result = useCase(
            soundId = PetSoundId("whistle"),
            settings = AudioSettings(
                defaultCategory = PetSoundCategory("dogs"),
                volumeMode = PetSoundVolumeMode.Custom,
                customVolumePercent = 35,
                loopDuringRecording = false,
                playOnPhotoCapture = true,
            ),
            loop = true,
        )

        assertEquals(35, player.lastGain?.percent)
        assertTrue(player.lastLoop)
        assertEquals(
            AttentionSoundPlaybackResult.Started,
            result,
        )
    }

    private class FakeAttentionSoundPlayer :
        AttentionSoundPlayer {

        var lastGain: PetSoundGain? = null
        var lastLoop: Boolean = false

        override suspend fun play(
            soundId: PetSoundId,
            gain: PetSoundGain,
            loop: Boolean,
        ): AttentionSoundPlaybackResult {
            lastGain = gain
            lastLoop = loop

            return AttentionSoundPlaybackResult.Started
        }

        override suspend fun stop() = Unit
    }
}