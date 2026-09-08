package com.civdevops.petcam.core.model

@JvmInline
value class SoundPackId(
    val rawValue: String,
) {
    init {
        require(rawValue.isNotBlank()) {
            "SoundPackId must not be blank."
        }
    }
}