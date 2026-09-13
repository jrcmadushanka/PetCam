package com.civdevops.petcam.data.audio.catalog

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.SoundPack

internal interface BundledPetSoundCatalog {
    val pack: SoundPack
    val entries: List<BundledPetSoundEntry>

    fun find(soundId: PetSoundId): BundledPetSoundEntry? {
        return entries.firstOrNull { it.sound.id == soundId }
    }
}