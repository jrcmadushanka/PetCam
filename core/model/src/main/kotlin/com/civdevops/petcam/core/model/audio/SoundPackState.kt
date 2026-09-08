package com.civdevops.petcam.core.model.audio

sealed interface SoundPackState {

    data object Bundled : SoundPackState

    data object NotInstalled : SoundPackState

    data class Downloading(
        val progressPercent: Int,
    ) : SoundPackState {
        init {
            require(progressPercent in 0..100) {
                "Download progress must be between 0 and 100."
            }
        }
    }

    data object Installed : SoundPackState

    data class Failed(
        val failure: SoundPackFailure,
    ) : SoundPackState
}