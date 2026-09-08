package com.civdevops.petcam.core.model.settings

import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.VideoQuality

data class CameraSettings(
    val defaultMode: CaptureMode,
    val defaultLens: CameraLens,
    val flashMode: FlashMode,
    val videoQuality: VideoQuality,
    val recordAudio: Boolean,
)