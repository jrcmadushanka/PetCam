package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.core.model.camera.VideoQuality
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolveSupportedVideoQualityUseCaseTest {

    private val useCase =
        ResolveSupportedVideoQualityUseCase()

    @Test
    fun `preferred quality is returned when supported`() {
        val result = useCase(
            preferredQuality = VideoQuality.FHD,
            supportedQualities = setOf(
                VideoQuality.FHD,
                VideoQuality.HD,
            ),
        )

        assertEquals(
            VideoQuality.FHD,
            result,
        )
    }

    @Test
    fun `nearest lower quality is preferred as fallback`() {
        val result = useCase(
            preferredQuality = VideoQuality.UHD,
            supportedQualities = setOf(
                VideoQuality.HD,
                VideoQuality.SD,
            ),
        )

        assertEquals(
            VideoQuality.HD,
            result,
        )
    }

    @Test
    fun `nearest higher quality is used when no lower quality exists`() {
        val result = useCase(
            preferredQuality = VideoQuality.SD,
            supportedQualities = setOf(
                VideoQuality.UHD,
                VideoQuality.FHD,
            ),
        )

        assertEquals(
            VideoQuality.FHD,
            result,
        )
    }

    @Test
    fun `null is returned when no video quality is supported`() {
        val result = useCase(
            preferredQuality = VideoQuality.FHD,
            supportedQualities = emptySet(),
        )

        assertNull(result)
    }
}