package com.civdevops.petcam.feature.camera

sealed interface CameraAction {

    data object SwitchLens : CameraAction

    data object CycleFlash : CameraAction
}