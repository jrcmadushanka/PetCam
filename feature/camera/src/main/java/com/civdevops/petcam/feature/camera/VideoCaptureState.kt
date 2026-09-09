package com.civdevops.petcam.feature.camera

import com.civdevops.petcam.core.model.MediaId
import com.civdevops.petcam.core.model.camera.RecordingFailure

sealed interface VideoCaptureState {
    data object Idle : VideoCaptureState
    data class Saved(val mediaId: MediaId) : VideoCaptureState
    data class Failed(val failure: RecordingFailure) : VideoCaptureState
}