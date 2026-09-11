package com.civdevops.petcam.domain.audio

sealed interface AttentionSoundPlaybackResult {

    data object Started : AttentionSoundPlaybackResult

    data object Cancelled : AttentionSoundPlaybackResult

    data class Failed(
        val failure: AttentionSoundFailure
    ) : AttentionSoundPlaybackResult
}