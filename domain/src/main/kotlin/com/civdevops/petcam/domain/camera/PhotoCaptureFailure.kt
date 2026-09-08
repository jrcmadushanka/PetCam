package com.civdevops.petcam.domain.camera

enum class PhotoCaptureFailure {
    CAMERA_UNAVAILABLE,
    CAPTURE_FAILED,
    STORAGE_UNAVAILABLE,
    UNKNOWN,
}