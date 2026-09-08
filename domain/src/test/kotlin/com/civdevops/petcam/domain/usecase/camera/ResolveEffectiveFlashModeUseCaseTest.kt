package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.core.model.camera.CameraLensCapabilities
import com.civdevops.petcam.core.model.camera.FlashMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveEffectiveFlashModeUseCaseTest {

    private val useCase =
        ResolveEffectiveFlashModeUseCase()

    @Test
    fun `preferred flash mode is preserved when flash is supported`() {
        val result = useCase(
            preferredFlashMode = FlashMode.AUTO,
            capabilities = CameraLensCapabilities(
                flashSupported = true,
                supportedVideoQualities = emptySet(),
            ),
        )

        assertEquals(
            FlashMode.AUTO,
            result,
        )
    }

    @Test
    fun `flash resolves to off when flash is unavailable`() {
        val result = useCase(
            preferredFlashMode = FlashMode.ON,
            capabilities = CameraLensCapabilities(
                flashSupported = false,
                supportedVideoQualities = emptySet(),
            ),
        )

        assertEquals(
            FlashMode.OFF,
            result,
        )
    }
}