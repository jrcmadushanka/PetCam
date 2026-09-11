package com.civdevops.petcam.domain.usecase.audio

import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackState
import com.civdevops.petcam.domain.audio.AttentionSoundPlayer
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAttentionSoundPlaybackStateUseCase @Inject constructor(
    private val player: AttentionSoundPlayer
) {
    operator fun invoke(): Flow<AttentionSoundPlaybackState> {
        return player.observePlaybackState()
    }
}