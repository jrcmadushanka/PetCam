package com.civdevops.petcam.core.model.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SoundPackStateTest {

    @Test
    fun `downloading accepts zero percent`() {
        val state = SoundPackState.Downloading(
            progressPercent = 0,
        )

        assertEquals(0, state.progressPercent)
    }

    @Test
    fun `downloading accepts one hundred percent`() {
        val state = SoundPackState.Downloading(
            progressPercent = 100,
        )

        assertEquals(100, state.progressPercent)
    }

    @Test
    fun `downloading rejects progress below zero`() {
        assertFailsWith<IllegalArgumentException> {
            SoundPackState.Downloading(
                progressPercent = -1,
            )
        }
    }

    @Test
    fun `downloading rejects progress above one hundred`() {
        assertFailsWith<IllegalArgumentException> {
            SoundPackState.Downloading(
                progressPercent = 101,
            )
        }
    }
}