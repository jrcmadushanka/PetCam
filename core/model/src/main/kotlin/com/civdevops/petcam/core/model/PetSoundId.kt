package com.civdevops.petcam.core.model

@JvmInline
value class PetSoundId(
    val rawValue: String,
) {
    init {
        require(rawValue.isNotBlank()) {
            "PetSoundId must not be blank."
        }
    }
}