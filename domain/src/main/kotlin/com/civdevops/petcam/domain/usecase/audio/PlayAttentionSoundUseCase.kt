package com.civdevops.petcam.domain.usecase.audio

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackResult
import com.civdevops.petcam.domain.audio.AttentionSoundPlayer

class PlayAttentionSoundUseCase(
    private val attentionSoundPlayer: AttentionSoundPlayer,
    private val resolveEffectiveSoundGainUseCase:
    ResolveEffectiveSoundGainUseCase,
) {

    suspend operator fun invoke(
        soundId: PetSoundId,
        settings: AudioSettings,
        loop: Boolean,
    ): AttentionSoundPlaybackResult =
        attentionSoundPlayer.play(
            soundId = soundId,
            gain = resolveEffectiveSoundGainUseCase(settings),
            loop = loop,
        )
}