package com.civdevops.petcam.feature.camera

data class CameraUiState(
    val configuration: CameraConfigurationState = CameraConfigurationState.Loading,
)