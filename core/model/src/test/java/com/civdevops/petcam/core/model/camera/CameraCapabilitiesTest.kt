package com.civdevops.petcam.core.model.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CameraCapabilitiesTest {

    @Test
    fun `capabilities returns configuration for available lens`() {
        val backCapabilities = CameraLensCapabilities(
            flashSupported = true,
            supportedVideoQualities = setOf(
                VideoQuality.UHD,
                VideoQuality.FHD,
            ),
        )

        val capabilities = CameraCapabilities(
            lenses = mapOf(
                CameraLens.BACK to backCapabilities,
            ),
        )

        assertEquals(
            backCapabilities,
            capabilities[CameraLens.BACK],
        )
    }

    @Test
    fun `capabilities returns null for unavailable lens`() {
        val capabilities = CameraCapabilities(
            lenses = mapOf(
                CameraLens.BACK to CameraLensCapabilities(
                    flashSupported = true,
                    supportedVideoQualities = setOf(VideoQuality.FHD),
                ),
            ),
        )

        assertNull(
            capabilities[CameraLens.FRONT],
        )
    }

    @Test
    fun `capabilities rejects camera with no lenses`() {
        assertFailsWith<IllegalArgumentException> {
            CameraCapabilities(
                lenses = emptyMap(),
            )
        }
    }
}