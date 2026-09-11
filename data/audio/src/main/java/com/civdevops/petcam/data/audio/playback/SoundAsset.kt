package com.civdevops.petcam.data.audio.playback

internal sealed interface SoundAsset {
    val durationMillis: Long

    data class Bundled(
        val rawResourceId: Int,
        override val durationMillis: Long
    ) : SoundAsset

    data class Downloaded(
        val filePath: String,
        override val durationMillis: Long
    ) : SoundAsset
}