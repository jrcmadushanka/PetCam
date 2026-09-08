package com.civdevops.petcam.core.model.audio

@JvmInline
value class PetSoundCategory(
    val rawValue: String,
) {
    init {
        require(rawValue.isNotBlank()) {
            "PetSoundCategory must not be blank."
        }
    }
}