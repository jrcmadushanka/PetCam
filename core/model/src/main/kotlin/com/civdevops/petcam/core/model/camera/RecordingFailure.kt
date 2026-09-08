package com.civdevops.petcam.core.model.camera

enum class RecordingFailure {
    CAMERA_UNAVAILABLE,
    AUDIO_PERMISSION_DENIED,
    QUALITY_UNAVAILABLE,
    INVALID_STATE,
    START_FAILED,
    FINALIZATION_FAILED,
    STORAGE_UNAVAILABLE,
    UNKNOWN,
}