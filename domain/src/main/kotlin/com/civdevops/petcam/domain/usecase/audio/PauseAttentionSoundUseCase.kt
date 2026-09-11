package com.civdevops.petcam.domain.usecase.audio

import com.civdevops.petcam.domain.audio.AttentionSoundPlayer
import javax.inject.Inject

class PauseAttentionSoundUseCase @Inject constructor(
    private val player: AttentionSoundPlayer
) {
    suspend operator fun invoke() {
        player.pause()
    }
}