package com.civdevops.petcam.domain.camera

enum class CameraOperationFailure {
    CAMERA_UNAVAILABLE,
    LENS_UNAVAILABLE,
    FLASH_UNAVAILABLE,
    INVALID_STATE,
    UNKNOWN,
}