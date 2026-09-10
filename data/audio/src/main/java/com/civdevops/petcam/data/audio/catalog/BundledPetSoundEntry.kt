package com.civdevops.petcam.data.audio.catalog

import com.civdevops.petcam.core.model.audio.PetSound

internal data class BundledPetSoundEntry(
    val sound: PetSound,
    val rawResourceId: Int,
    val durationMillis: Long
) {
    init {
        require(rawResourceId != 0) {
            "Bundled sound resource ID must be valid."
        }

        require(durationMillis > 0) {
            "Bundled sound duration must be positive."
        }
    }
}