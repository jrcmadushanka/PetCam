package com.civdevops.petcam.core.model.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PetSoundCategoryTest {

    @Test
    fun `category preserves valid value`() {
        val category = PetSoundCategory("dogs")

        assertEquals("dogs", category.rawValue)
    }

    @Test
    fun `category rejects blank value`() {
        assertFailsWith<IllegalArgumentException> {
            PetSoundCategory(" ")
        }
    }
}