package com.civdevops.petcam.core.model.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PetSoundGainTest {

    @Test
    fun `gain accepts minimum value`() {
        val gain = PetSoundGain(0)

        assertEquals(0, gain.percent)
    }

    @Test
    fun `gain accepts maximum value`() {
        val gain = PetSoundGain(100)

        assertEquals(100, gain.percent)
    }

    @Test
    fun `gain rejects value below minimum`() {
        assertFailsWith<IllegalArgumentException> {
            PetSoundGain(-1)
        }
    }

    @Test
    fun `gain rejects value above maximum`() {
        assertFailsWith<IllegalArgumentException> {
            PetSoundGain(101)
        }
    }
}