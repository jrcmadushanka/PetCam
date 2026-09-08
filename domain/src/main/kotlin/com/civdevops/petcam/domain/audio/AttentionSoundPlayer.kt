package com.civdevops.petcam.domain.audio

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSoundGain

interface AttentionSoundPlayer {

    suspend fun play(
        soundId: PetSoundId,
        gain: PetSoundGain,
        loop: Boolean,
    ): AttentionSoundPlaybackResult

    suspend fun stop()
}