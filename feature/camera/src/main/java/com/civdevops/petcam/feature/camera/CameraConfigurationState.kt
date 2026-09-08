package com.civdevops.petcam.feature.camera

import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.FlashMode

sealed interface CameraConfigurationState {

    data object Loading :
        CameraConfigurationState

    data class Ready(
        val capabilities: CameraCapabilities,
        val lens: CameraLens,
        val flashMode: FlashMode,
    ) : CameraConfigurationState {

        val canSwitchLens: Boolean
            get() = capabilities.lenses.size > 1

        val flashSupported: Boolean
            get() =
                capabilities[lens]
                    ?.flashSupported
                    ?: false
    }
}