package com.civdevops.petcam.data.audio

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.SoundPackId
import com.civdevops.petcam.core.model.audio.PetSound
import com.civdevops.petcam.core.model.audio.PetSoundCategories
import com.civdevops.petcam.core.model.audio.PetSoundSource
import com.civdevops.petcam.core.model.audio.SoundPack
import com.civdevops.petcam.core.model.audio.SoundPackState
import com.civdevops.petcam.data.audio.catalog.BundledPetSoundCatalog
import com.civdevops.petcam.data.audio.catalog.BundledPetSoundEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioRepositoryTest {

    private val pack = SoundPack(
        id = SoundPackId("starter"),
        name = "Starter",
        state = SoundPackState.Bundled
    )

    private val dogSound = PetSound(
        id = PetSoundId("dog_01"),
        packId = pack.id,
        category = PetSoundCategories.Dogs,
        name = "Dog 1",
        source = PetSoundSource.Bundled
    )

    private val catSound = PetSound(
        id = PetSoundId("cat_01"),
        packId = pack.id,
        category = PetSoundCategories.Cats,
        name = "Cat 1",
        source = PetSoundSource.Bundled
    )

    private val catalog = FakeBundledPetSoundCatalog(
        pack = pack,
        entries = listOf(
            BundledPetSoundEntry(dogSound, rawResourceId = 1, durationMillis = 1_000),
            BundledPetSoundEntry(catSound, rawResourceId = 2, durationMillis = 1_500)
        )
    )

    @Test
    fun `pet sound repository exposes bundled sounds`() = runTest {
        val repository = DefaultPetSoundRepository(catalog)

        assertEquals(listOf(dogSound, catSound), repository.observePetSounds().first())
    }

    @Test
    fun `pet sound repository finds sound by id`() = runTest {
        val repository = DefaultPetSoundRepository(catalog)

        assertEquals(catSound, repository.getPetSound(catSound.id))
        assertNull(repository.getPetSound(PetSoundId("missing")))
    }

    @Test
    fun `sound pack repository exposes starter pack`() = runTest {
        val repository = DefaultSoundPackRepository(catalog)

        assertEquals(listOf(pack), repository.observeSoundPacks().first())
        assertEquals(pack, repository.getSoundPack(pack.id))
    }

    private class FakeBundledPetSoundCatalog(
        override val pack: SoundPack,
        override val entries: List<BundledPetSoundEntry>
    ) : BundledPetSoundCatalog
}