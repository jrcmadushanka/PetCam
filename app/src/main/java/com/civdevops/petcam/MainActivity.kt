package com.civdevops.petcam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.civdevops.petcam.camera.CameraPreviewHost
import com.civdevops.petcam.core.designsystem.theme.PetCamTheme
import com.civdevops.petcam.data.camera.CameraXSession
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var cameraXSession: CameraXSession

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(
            savedInstanceState,
        )

        enableEdgeToEdge()

        setContent {
            PetCamTheme {
                CameraPreviewHost(cameraXSession = cameraXSession)
            }
        }
    }
}