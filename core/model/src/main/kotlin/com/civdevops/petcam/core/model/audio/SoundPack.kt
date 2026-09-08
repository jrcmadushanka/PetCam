package com.civdevops.petcam.core.model.audio

import com.civdevops.petcam.core.model.SoundPackId

data class SoundPack(
    val id: SoundPackId,
    val name: String,
    val state: SoundPackState,
) {
    init {
        require(name.isNotBlank()) {
            "Sound pack name must not be blank."
        }
    }
}