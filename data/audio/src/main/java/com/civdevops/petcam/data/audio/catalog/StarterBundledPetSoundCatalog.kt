package com.civdevops.petcam.data.audio.catalog

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.SoundPackId
import com.civdevops.petcam.core.model.audio.PetSound
import com.civdevops.petcam.core.model.audio.PetSoundCategories
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.audio.PetSoundSource
import com.civdevops.petcam.core.model.audio.SoundPack
import com.civdevops.petcam.core.model.audio.SoundPackState
import com.civdevops.petcam.data.audio.generated.GeneratedBundledPetSounds
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StarterBundledPetSoundCatalog @Inject constructor(
    private val durationReader: BundledAudioDurationReader
) : BundledPetSoundCatalog {

    override val pack = SoundPack(
        id = SoundPackId("starter"),
        name = "Starter",
        state = SoundPackState.Bundled
    )

    override val entries: List<BundledPetSoundEntry> by lazy {
        GeneratedBundledPetSounds.entries.map { generated ->
            BundledPetSoundEntry(
                sound = PetSound(
                    id = PetSoundId("${pack.id.rawValue}:${generated.assetKey}"),
                    packId = pack.id,
                    category = generated.category.toPetSoundCategory(),
                    name = generated.displayName,
                    source = PetSoundSource.Bundled
                ),
                rawResourceId = generated.rawResourceId,
                durationMillis = durationReader.readMillis(generated.rawResourceId)
            )
        }
    }

    private fun String.toPetSoundCategory(): PetSoundCategory {
        return when (this) {
            PetSoundCategories.Dogs.rawValue -> PetSoundCategories.Dogs
            PetSoundCategories.Cats.rawValue -> PetSoundCategories.Cats
            PetSoundCategories.Whistles.rawValue -> PetSoundCategories.Whistles
            PetSoundCategories.Toys.rawValue -> PetSoundCategories.Toys
            PetSoundCategories.Other.rawValue -> PetSoundCategories.Other
            else -> error("Unsupported generated pet sound category: $this")
        }
    }
}