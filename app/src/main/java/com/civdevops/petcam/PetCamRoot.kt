package com.civdevops.petcam

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.civdevops.petcam.camera.CameraPreviewHost
import com.civdevops.petcam.data.camera.CameraXSession
import com.civdevops.petcam.feature.settings.SettingsRoute

private enum class AppScreen {
    CAMERA,
    SETTINGS
}

@Composable
fun PetCamRoot(cameraXSession: CameraXSession) {
    var screen by rememberSaveable { mutableStateOf(AppScreen.CAMERA) }

    BackHandler(enabled = screen == AppScreen.SETTINGS) {
        screen = AppScreen.CAMERA
    }

    when (screen) {
        AppScreen.CAMERA -> CameraPreviewHost(
            cameraXSession = cameraXSession,
            onOpenSettings = { screen = AppScreen.SETTINGS }
        )

        AppScreen.SETTINGS -> SettingsRoute()
    }
}