package com.civdevops.petcam.domain.camera

sealed interface CameraOperationResult {

    data object Success : CameraOperationResult

    data class Failed(
        val failure: CameraOperationFailure,
    ) : CameraOperationResult
}