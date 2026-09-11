package com.civdevops.petcam.domain.audio

import com.civdevops.petcam.core.model.PetSoundId

sealed interface AttentionSoundPlaybackState {

    data object Idle : AttentionSoundPlaybackState

    data class Loading(
        val soundId: PetSoundId
    ) : AttentionSoundPlaybackState

    data class Playing(
        val soundId: PetSoundId,
        val looping: Boolean
    ) : AttentionSoundPlaybackState

    data class Paused(
        val soundId: PetSoundId,
        val looping: Boolean
    ) : AttentionSoundPlaybackState

    data class Failed(
        val failure: AttentionSoundFailure
    ) : AttentionSoundPlaybackState
}