package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.core.model.camera.CameraLensCapabilities
import com.civdevops.petcam.core.model.camera.FlashMode
import javax.inject.Inject

class ResolveEffectiveFlashModeUseCase @Inject constructor() {

    operator fun invoke(
        preferredFlashMode: FlashMode,
        capabilities: CameraLensCapabilities
    ): FlashMode =
        if (capabilities.flashSupported) {
            preferredFlashMode
        } else {
            FlashMode.OFF
        }
}