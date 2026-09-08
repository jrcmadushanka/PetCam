package com.civdevops.petcam.core.model.audio

@JvmInline
value class PetSoundGain(
    val percent: Int,
) {
    init {
        require(percent in 0..100) {
            "Pet sound gain must be between 0 and 100."
        }
    }
}