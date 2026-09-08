package com.civdevops.petcam.feature.camera

import com.civdevops.petcam.core.model.MediaId
import com.civdevops.petcam.domain.camera.PhotoCaptureFailure

sealed interface PhotoCaptureState {
    data object Idle : PhotoCaptureState
    data object Capturing : PhotoCaptureState
    data class Saved(val mediaId: MediaId) : PhotoCaptureState
    data class Failed(val failure: PhotoCaptureFailure) : PhotoCaptureState
}