package com.civdevops.petcam.core.model

import junit.framework.TestCase.assertEquals
import kotlin.test.Test
import kotlin.test.assertFailsWith


class IdentifierTest {

    @Test
    fun `media id preserves valid value`() {
        val id = MediaId("media-123")

        assertEquals("media-123", id.rawValue)
    }

    @Test
    fun `media id rejects blank value`() {
        assertFailsWith<IllegalArgumentException> {
            MediaId(" ")
        }
    }

    @Test
    fun `pet sound id rejects blank value`() {
        assertFailsWith<IllegalArgumentException> {
            PetSoundId("")
        }
    }

    @Test
    fun `sound pack id rejects blank value`() {
        assertFailsWith<IllegalArgumentException> {
            SoundPackId("\t")
        }
    }
}