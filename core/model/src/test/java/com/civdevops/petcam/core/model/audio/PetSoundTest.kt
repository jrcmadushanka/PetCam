package com.civdevops.petcam.core.model.audio

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.SoundPackId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PetSoundTest {

    @Test
    fun `pet sound preserves source`() {
        val sound = PetSound(
            id = PetSoundId("sound-1"),
            packId = SoundPackId("starter"),
            category = PetSoundCategory("dogs"),
            name = "Dog Whistle",
            source = PetSoundSource.Bundled,
        )

        assertEquals(
            PetSoundSource.Bundled,
            sound.source,
        )
    }

    @Test
    fun `pet sound rejects blank name`() {
        assertFailsWith<IllegalArgumentException> {
            PetSound(
                id = PetSoundId("sound-1"),
                packId = SoundPackId("starter"),
                category = PetSoundCategory("dogs"),
                name = " ",
                source = PetSoundSource.Bundled,
            )
        }
    }
}