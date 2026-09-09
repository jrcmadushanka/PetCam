package com.civdevops.petcam.feature.camera

sealed interface CameraEffect {
    data object RequestMicrophonePermission : CameraEffect
}