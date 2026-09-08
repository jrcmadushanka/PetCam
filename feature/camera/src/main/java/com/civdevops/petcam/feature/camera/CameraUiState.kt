package com.civdevops.petcam.feature.camera

data class CameraUiState(
    val configuration: CameraConfigurationState = CameraConfigurationState.Loading,
    val photoCapture: PhotoCaptureState = PhotoCaptureState.Idle
)