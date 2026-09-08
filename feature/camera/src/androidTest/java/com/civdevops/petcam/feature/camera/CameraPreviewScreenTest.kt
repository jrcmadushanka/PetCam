package com.civdevops.petcam.feature.camera

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.civdevops.petcam.core.designsystem.theme.PetCamTheme
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CameraLensCapabilities
import com.civdevops.petcam.core.model.camera.FlashMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraPreviewScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readyCameraShowsCaptureButton() {
        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyState(),
                    canCapturePhoto = true,
                    onAction = {},
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    previewContent = {}
                )
            }
        }

        composeRule.onNodeWithText("Capture photo").assertIsDisplayed()
    }

    @Test
    fun captureButtonDispatchesCaptureAction() {
        var action: CameraAction? = null

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyState(),
                    canCapturePhoto = true,
                    onAction = { action = it },
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    previewContent = {}
                )
            }
        }

        composeRule.onNodeWithText("Capture photo").performClick()

        assertEquals(CameraAction.CapturePhoto, action)
    }

    @Test
    fun missingStoragePermissionRequestsPermissionInsteadOfCapture() {
        var permissionRequested = false
        var action: CameraAction? = null

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyState(),
                    canCapturePhoto = false,
                    onAction = { action = it },
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = { permissionRequested = true },
                    onRetry = {},
                    previewContent = {}
                )
            }
        }

        composeRule.onNodeWithText("Capture photo").performClick()

        assertTrue(permissionRequested)
        assertEquals(null, action)
    }

    @Test
    fun unsupportedFlashShowsNoFlash() {
        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyState(),
                    canCapturePhoto = true,
                    onAction = {},
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    previewContent = {}
                )
            }
        }

        composeRule.onNodeWithText("No flash").assertIsDisplayed()
    }

    private fun readyState(): CameraUiState {
        val capabilities = CameraCapabilities(
            mapOf(
                CameraLens.FRONT to CameraLensCapabilities(
                    flashSupported = false,
                    supportedVideoQualities = emptySet()
                )
            )
        )

        return CameraUiState(
            configuration = CameraConfigurationState.Ready(
                capabilities = capabilities,
                lens = CameraLens.FRONT,
                flashMode = FlashMode.OFF
            ),
            photoCapture = PhotoCaptureState.Idle
        )
    }
}