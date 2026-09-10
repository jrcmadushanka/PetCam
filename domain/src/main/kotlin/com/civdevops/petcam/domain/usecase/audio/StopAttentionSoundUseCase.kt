package com.civdevops.petcam.domain.usecase.audio

import com.civdevops.petcam.domain.audio.AttentionSoundPlayer
import javax.inject.Inject

class StopAttentionSoundUseCase @Inject constructor(
    private val attentionSoundPlayer: AttentionSoundPlayer
) {
    suspend operator fun invoke() {
        attentionSoundPlayer.stop()
    }
}