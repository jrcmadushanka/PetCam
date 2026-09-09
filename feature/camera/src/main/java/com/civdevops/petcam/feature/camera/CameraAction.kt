package com.civdevops.petcam.feature.camera

import com.civdevops.petcam.core.model.camera.CaptureMode

sealed interface CameraAction {
    data object SwitchLens : CameraAction
    data object CycleFlash : CameraAction
    data object CapturePhoto : CameraAction
    data class SetCaptureMode(val mode: CaptureMode) : CameraAction
    data object StartVideoRecording : CameraAction
    data object PauseVideoRecording : CameraAction
    data object ResumeVideoRecording : CameraAction
    data object StopVideoRecording : CameraAction
    data object ToggleVideoTorch : CameraAction
}