package com.civdevops.petcam.core.model.audio

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.SoundPackId

data class PetSound(
    val id: PetSoundId,
    val packId: SoundPackId,
    val category: PetSoundCategory,
    val name: String,
    val source: PetSoundSource,
) {
    init {
        require(name.isNotBlank()) {
            "Pet sound name must not be blank."
        }
    }
}