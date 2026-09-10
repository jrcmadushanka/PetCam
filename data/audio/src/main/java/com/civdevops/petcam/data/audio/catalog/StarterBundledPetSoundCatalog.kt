package com.civdevops.petcam.data.audio.catalog

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.SoundPackId
import com.civdevops.petcam.core.model.audio.PetSound
import com.civdevops.petcam.core.model.audio.PetSoundCategories
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.audio.PetSoundSource
import com.civdevops.petcam.core.model.audio.SoundPack
import com.civdevops.petcam.core.model.audio.SoundPackState
import com.civdevops.petcam.data.audio.R
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
        listOf(
            entry("dog_01", PetSoundCategories.Dogs, "Dog", R.raw.dog_angry_dog_1),
            entry("cat_01", PetSoundCategories.Cats, "Cat", R.raw.cat_angry_cat_1),
            entry("whistle_01", PetSoundCategories.Whistles, "Whistle", R.raw.other_whistle_1),
            entry("toy_01", PetSoundCategories.Toys, "Squeaky Toy", R.raw.other_cartoon_up_down),
            entry("other_01", PetSoundCategories.Other, "Attention", R.raw.other_squeaze_normal)
        )
    }

    private fun entry(
        id: String,
        category: PetSoundCategory,
        name: String,
        rawResourceId: Int
    ): BundledPetSoundEntry {
        return BundledPetSoundEntry(
            sound = PetSound(
                id = PetSoundId(id),
                packId = pack.id,
                category = category,
                name = name,
                source = PetSoundSource.Bundled
            ),
            rawResourceId = rawResourceId,
            durationMillis = durationReader.readMillis(rawResourceId)
        )
    }
}