package com.civdevops.petcam.feature.camera

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import com.civdevops.petcam.core.designsystem.theme.PetCamTheme
import com.civdevops.petcam.core.model.camera.CameraCapabilities
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CameraLensCapabilities
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.RecordingState
import com.civdevops.petcam.core.model.camera.VideoQuality
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
                    previewContent = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Capture photo")
            .assertIsDisplayed()
    }

    @Test
    fun shutterTouchDispatchesPressThenRelease() {
        val actions = mutableListOf<CameraAction>()

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyState(),
                    canCapturePhoto = true,
                    onAction = actions::add,
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    previewContent = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule.onNodeWithTag(PHOTO_SHUTTER_TEST_TAG).performTouchInput {
            down(center)
            advanceEventTime(500)
            up()
        }

        composeRule.waitForIdle()

        assertEquals(
            listOf(
                CameraAction.PhotoShutterPressed,
                CameraAction.PhotoShutterReleased
            ),
            actions
        )
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
                    previewContent = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Capture photo")
            .performClick()

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
                    previewContent = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("No flash")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    fun videoModeShowsRecordButton() {
        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyVideoState(),
                    canCapturePhoto = true,
                    onAction = {},
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    onOpenSettings = {},
                    previewContent = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Record")
            .assertIsDisplayed()
    }

    @Test
    fun recordingShowsPauseAndStopControls() {
        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyVideoState(RecordingState.Recording(5_000)),
                    canCapturePhoto = true,
                    onAction = {},
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    onOpenSettings = {},
                    previewContent = {}
                )
            }
        }

        composeRule.onNodeWithText("00:05").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Pause")
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Stop")
            .assertIsDisplayed()
    }

    @Test
    fun pausedRecordingShowsResumeAndStopControls() {
        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyVideoState(RecordingState.Paused(12_000)),
                    canCapturePhoto = true,
                    onAction = {},
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    onOpenSettings = {},
                    previewContent = {}
                )
            }
        }

        composeRule.onNodeWithText("00:12").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Resume")
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Stop")
            .assertIsDisplayed()
    }

    @Test
    fun shutterPressAndReleaseDispatchesLegacyPetCamSequence() {
        val actions = mutableListOf<CameraAction>()

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyState(),
                    canCapturePhoto = true,
                    onAction = actions::add,
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    previewContent = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule.onNodeWithTag(PHOTO_SHUTTER_TEST_TAG).performTouchInput {
            down(center)
            advanceEventTime(500)
            up()
        }

        composeRule.waitForIdle()

        assertEquals(
            listOf(
                CameraAction.PhotoShutterPressed,
                CameraAction.PhotoShutterReleased
            ),
            actions
        )
    }

    @Test
    fun shutterSemanticClickDispatchesCaptureFallback() {
        val actions = mutableListOf<CameraAction>()

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyState(),
                    canCapturePhoto = true,
                    onAction = actions::add,
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    previewContent = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule.onNodeWithTag(PHOTO_SHUTTER_TEST_TAG)
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.waitForIdle()

        assertEquals(
            listOf(CameraAction.CapturePhoto),
            actions
        )
    }

    @Test
    fun supportedPhotoFlashDispatchesCycleFlash() {
        val actions = mutableListOf<CameraAction>()

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyState(flashSupported = true),
                    canCapturePhoto = true,
                    onAction = actions::add,
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    previewContent = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Flash: Off")
            .performClick()

        assertEquals(
            listOf(CameraAction.CycleFlash),
            actions
        )
    }

    @Test
    fun settingsRemainAccessibleWhileRecording() {
        var settingsOpened = false

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyVideoState(
                        recordingState = RecordingState.Recording(5_000)
                    ),
                    canCapturePhoto = true,
                    onAction = {},
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    previewContent = {},
                    onOpenSettings = { settingsOpened = true }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        assertTrue(settingsOpened)
    }

    @Test
    fun switchLensControlDescribesCurrentLensAndDispatchesAction() {
        val actions = mutableListOf<CameraAction>()

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyState(
                        flashSupported = true,
                        canSwitchLens = true
                    ),
                    canCapturePhoto = true,
                    onAction = actions::add,
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    previewContent = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Switch camera")
            .performClick()

        assertEquals(
            listOf(CameraAction.SwitchLens),
            actions
        )
    }

    @Test
    fun videoModeSelectionDispatchesSetCaptureMode() {
        val actions = mutableListOf<CameraAction>()

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyState(),
                    canCapturePhoto = true,
                    onAction = actions::add,
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    previewContent = {},
                    onOpenSettings = {}
                )
            }
        }

        composeRule.onNodeWithText("Video").performClick()

        assertEquals(
            listOf(CameraAction.SetCaptureMode(CaptureMode.VIDEO)),
            actions
        )
    }

    @Test
    fun recordControlDispatchesStartRecording() {
        val actions = mutableListOf<CameraAction>()

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyVideoState(),
                    canCapturePhoto = true,
                    onAction = actions::add,
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    onOpenSettings = {},
                    previewContent = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Record")
            .performClick()

        assertEquals(
            listOf(CameraAction.StartVideoRecording),
            actions
        )
    }

    @Test
    fun recordingControlsDispatchPauseAndStop() {
        val actions = mutableListOf<CameraAction>()

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyVideoState(
                        recordingState = RecordingState.Recording(5_000)
                    ),
                    canCapturePhoto = true,
                    onAction = actions::add,
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    onOpenSettings = {},
                    previewContent = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Pause")
            .performClick()

        composeRule
            .onNodeWithContentDescription("Stop")
            .performClick()

        assertEquals(
            listOf(
                CameraAction.PauseVideoRecording,
                CameraAction.StopVideoRecording
            ),
            actions
        )
    }

    @Test
    fun pausedControlsDispatchResumeAndStop() {
        val actions = mutableListOf<CameraAction>()

        composeRule.setContent {
            PetCamTheme {
                CameraPreviewScreen(
                    previewStatus = CameraPreviewStatus.READY,
                    uiState = readyVideoState(
                        recordingState = RecordingState.Paused(12_000)
                    ),
                    canCapturePhoto = true,
                    onAction = actions::add,
                    onRequestCameraPermission = {},
                    onRequestCapturePermission = {},
                    onRetry = {},
                    onOpenSettings = {},
                    previewContent = {}
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Resume")
            .performClick()

        composeRule
            .onNodeWithContentDescription("Stop")
            .performClick()

        assertEquals(
            listOf(
                CameraAction.ResumeVideoRecording,
                CameraAction.StopVideoRecording
            ),
            actions
        )
    }

    private fun readyVideoState(
        recordingState: RecordingState = RecordingState.Idle
    ): CameraUiState {
        val capabilities = CameraCapabilities(
            mapOf(
                CameraLens.FRONT to CameraLensCapabilities(
                    flashSupported = false,
                    supportedVideoQualities = setOf(VideoQuality.FHD)
                )
            )
        )

        return CameraUiState(
            configuration = CameraConfigurationState.Ready(
                capabilities = capabilities,
                lens = CameraLens.FRONT,
                flashMode = FlashMode.OFF,
                videoQuality = VideoQuality.FHD
            ),
            captureMode = CaptureMode.VIDEO,
            recordAudio = true,
            recordingState = recordingState
        )
    }

    private fun readyState(
        flashSupported: Boolean = false,
        canSwitchLens: Boolean = false
    ): CameraUiState {
        val frontCapabilities = CameraLensCapabilities(
            flashSupported = flashSupported,
            supportedVideoQualities = emptySet()
        )

        val capabilities = if (canSwitchLens) {
            CameraCapabilities(
                mapOf(
                    CameraLens.FRONT to frontCapabilities,
                    CameraLens.BACK to CameraLensCapabilities(
                        flashSupported = flashSupported,
                        supportedVideoQualities = emptySet()
                    )
                )
            )
        } else {
            CameraCapabilities(
                mapOf(CameraLens.FRONT to frontCapabilities)
            )
        }

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