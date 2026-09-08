package com.civdevops.petcam.core.model.settings

data class ExperienceSettings(
    val keepScreenAwakeWhileRecording: Boolean,
    val hapticsEnabled: Boolean,
    val showOnlyAppMedia: Boolean,
    val confirmDelete: Boolean,
)