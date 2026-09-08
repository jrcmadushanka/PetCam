package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens

class ResolveAvailableCameraLensUseCase {

    operator fun invoke(
        preferredLens: CameraLens,
        capabilities: CameraCapabilities,
    ): CameraLens =
        when {
            capabilities[preferredLens] != null ->
                preferredLens

            capabilities[CameraLens.BACK] != null ->
                CameraLens.BACK

            capabilities[CameraLens.FRONT] != null ->
                CameraLens.FRONT

            else ->
                error(
                    "CameraCapabilities must contain at least one lens.",
                )
        }
}