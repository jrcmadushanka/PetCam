package com.civdevops.petcam.domain.camera

import com.civdevops.petcam.core.model.camera.VideoQuality

data class VideoRecordingRequest(
    val quality: VideoQuality,
    val recordAudio: Boolean,
)