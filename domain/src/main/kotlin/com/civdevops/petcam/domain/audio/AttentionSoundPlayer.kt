package com.civdevops.petcam.domain.audio

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSoundGain
import kotlinx.coroutines.flow.Flow

interface AttentionSoundPlayer {

    fun observePlaybackState(): Flow<AttentionSoundPlaybackState>

    suspend fun play(
        soundId: PetSoundId,
        gain: PetSoundGain,
        loop: Boolean
    ): AttentionSoundPlaybackResult

    suspend fun pause()

    suspend fun resume()

    suspend fun stop()
}