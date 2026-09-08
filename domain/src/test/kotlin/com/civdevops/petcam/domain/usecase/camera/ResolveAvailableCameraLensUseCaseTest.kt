package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CameraLensCapabilities
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveAvailableCameraLensUseCaseTest {

    private val lensCapabilities =
        CameraLensCapabilities(
            flashSupported = false,
            supportedVideoQualities = emptySet(),
        )

    private val useCase =
        ResolveAvailableCameraLensUseCase()

    @Test
    fun `preferred lens is used when available`() {
        val capabilities = CameraCapabilities(
            lenses = mapOf(
                CameraLens.BACK to lensCapabilities,
                CameraLens.FRONT to lensCapabilities,
            ),
        )

        assertEquals(
            CameraLens.FRONT,
            useCase(
                preferredLens = CameraLens.FRONT,
                capabilities = capabilities,
            ),
        )
    }

    @Test
    fun `back lens is fallback when preferred lens is unavailable`() {
        val capabilities = CameraCapabilities(
            lenses = mapOf(
                CameraLens.BACK to lensCapabilities,
            ),
        )

        assertEquals(
            CameraLens.BACK,
            useCase(
                preferredLens = CameraLens.FRONT,
                capabilities = capabilities,
            ),
        )
    }

    @Test
    fun `front lens is used when it is the only available lens`() {
        val capabilities = CameraCapabilities(
            lenses = mapOf(
                CameraLens.FRONT to lensCapabilities,
            ),
        )

        assertEquals(
            CameraLens.FRONT,
            useCase(
                preferredLens = CameraLens.BACK,
                capabilities = capabilities,
            ),
        )
    }
}