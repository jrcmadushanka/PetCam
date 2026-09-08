package com.civdevops.petcam.core.model.camera

data class CameraCapabilities(
    val lenses: Map<CameraLens, CameraLensCapabilities>,
) {
    init {
        require(lenses.isNotEmpty()) {
            "Camera capabilities must contain at least one lens."
        }
    }

    operator fun get(
        lens: CameraLens,
    ): CameraLensCapabilities? = lenses[lens]
}