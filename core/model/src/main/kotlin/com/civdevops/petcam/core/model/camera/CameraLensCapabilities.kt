package com.civdevops.petcam.core.model.camera

data class CameraLensCapabilities(
    val flashSupported: Boolean,
    val supportedVideoQualities: Set<VideoQuality>,
)