package com.civdevops.petcam.core.model.settings

data class AppSettings(
    val camera: CameraSettings,
    val audio: AudioSettings,
    val sharing: SharingSettings,
    val experience: ExperienceSettings,
)