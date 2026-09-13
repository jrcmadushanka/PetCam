package com.civdevops.petcam.domain.usecase.audio

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.audio.PetSoundGain
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.core.model.settings.PetSoundVolumeMode
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackResult
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackState
import com.civdevops.petcam.domain.audio.AttentionSoundPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayAttentionSoundUseCaseTest {

    @Test
    fun `custom volume is resolved before playback`() = runTest {
        val player = FakeAttentionSoundPlayer()

        val useCase = PlayAttentionSoundUseCase(
            attentionSoundPlayer = player,
            resolveEffectiveSoundGainUseCase = ResolveEffectiveSoundGainUseCase()
        )

        val result = useCase(
            soundId = PetSoundId("starter:whistle_01"),
            settings = AudioSettings(
                defaultCategory = PetSoundCategory("dogs"),
                volumeMode = PetSoundVolumeMode.Custom,
                customVolumePercent = 35,
                loopDuringRecording = false,
                playOnPhotoCapture = true
            ),
            loop = true
        )

        assertEquals(35, player.lastGain?.percent)
        assertTrue(player.lastLoop)
        assertEquals(
            AttentionSoundPlaybackResult.Started,
            result
        )
    }

    private class FakeAttentionSoundPlayer : AttentionSoundPlayer {

        private val playbackState =
            MutableStateFlow<AttentionSoundPlaybackState>(
                AttentionSoundPlaybackState.Idle
            )

        var lastGain: PetSoundGain? = null
        var lastLoop: Boolean = false

        override fun observePlaybackState(): Flow<AttentionSoundPlaybackState> {
            return playbackState.asStateFlow()
        }

        override suspend fun play(
            soundId: PetSoundId,
            gain: PetSoundGain,
            loop: Boolean
        ): AttentionSoundPlaybackResult {
            lastGain = gain
            lastLoop = loop
            playbackState.value =
                AttentionSoundPlaybackState.Playing(soundId, loop)

            return AttentionSoundPlaybackResult.Started
        }

        override suspend fun pause() {
            val current =
                playbackState.value as? AttentionSoundPlaybackState.Playing
                    ?: return

            playbackState.value =
                AttentionSoundPlaybackState.Paused(
                    soundId = current.soundId,
                    looping = current.looping
                )
        }

        override suspend fun resume() {
            val current =
                playbackState.value as? AttentionSoundPlaybackState.Paused
                    ?: return

            playbackState.value =
                AttentionSoundPlaybackState.Playing(
                    soundId = current.soundId,
                    looping = current.looping
                )
        }

        override suspend fun stop() {
            playbackState.value = AttentionSoundPlaybackState.Idle
        }
    }
}