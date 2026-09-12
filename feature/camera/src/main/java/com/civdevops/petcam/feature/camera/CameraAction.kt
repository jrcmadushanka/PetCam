package com.civdevops.petcam.feature.camera

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.camera.CaptureMode

sealed interface CameraAction {
    data object SwitchLens : CameraAction
    data object CycleFlash : CameraAction

    data object PhotoShutterPressed : CameraAction
    data object PhotoShutterReleased : CameraAction
    data object PhotoShutterCancelled : CameraAction
    data object CapturePhoto : CameraAction

    data class SetCaptureMode(val mode: CaptureMode) : CameraAction

    data object StartVideoRecording : CameraAction
    data object PauseVideoRecording : CameraAction
    data object ResumeVideoRecording : CameraAction
    data object StopVideoRecording : CameraAction
    data object ToggleVideoTorch : CameraAction

    data class SelectAttentionCategory(val category: PetSoundCategory) : CameraAction
    data class SelectAttentionSound(val soundId: PetSoundId) : CameraAction
    data object ToggleAttentionSoundPlayback : CameraAction
    data object CameraActive : CameraAction
    data object CameraInactive : CameraAction
}