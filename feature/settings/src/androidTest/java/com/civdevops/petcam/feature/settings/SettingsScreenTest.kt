package com.civdevops.petcam.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.civdevops.petcam.core.designsystem.theme.PetCamTheme
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.VideoQuality
import com.civdevops.petcam.core.model.settings.AppSettings
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.core.model.settings.CameraSettings
import com.civdevops.petcam.core.model.settings.ExperienceSettings
import com.civdevops.petcam.core.model.settings.PetSoundVolumeMode
import com.civdevops.petcam.core.model.settings.SharingSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsCategoriesAreDisplayed() {
        composeRule.setContent {
            PetCamTheme {
                SettingsScreen(
                    uiState =
                        SettingsUiState.Content(
                            settings =
                                sampleSettings(),
                            selectedSection = null,
                            saveFailed = false,
                        ),
                    onAction = {},
                )
            }
        }

        composeRule
            .onNodeWithText("Camera")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Pet sounds")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Sharing")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Experience")
            .assertIsDisplayed()
    }

    @Test
    fun selectingAudioCategoryDispatchesAction() {
        var receivedAction: SettingsAction? = null

        composeRule.setContent {
            PetCamTheme {
                SettingsScreen(
                    uiState =
                        SettingsUiState.Content(
                            settings =
                                sampleSettings(),
                            selectedSection = null,
                            saveFailed = false,
                        ),
                    onAction = {
                        receivedAction = it
                    },
                )
            }
        }

        composeRule
            .onNodeWithText("Pet sounds")
            .performClick()

        assertEquals(SettingsAction.SelectSection(SettingsSection.AUDIO), receivedAction)
    }

    private fun sampleSettings() =
        AppSettings(
            camera = CameraSettings(
                defaultMode =
                    CaptureMode.PHOTO,
                defaultLens =
                    CameraLens.BACK,
                flashMode =
                    FlashMode.OFF,
                videoQuality =
                    VideoQuality.FHD,
                recordAudio = true,
            ),
            audio = AudioSettings(
                defaultCategory =
                    PetSoundCategory("dogs"),
                volumeMode =
                    PetSoundVolumeMode.FollowDevice,
                customVolumePercent = 75,
                loopDuringRecording = false,
                playOnPhotoCapture = true,
            ),
            sharing = SharingSettings(
                autoOpenShareAfterCapture = false,
                preferredQuickShareTarget = null,
            ),
            experience =
                ExperienceSettings(
                    keepScreenAwakeWhileRecording =
                        true,
                    hapticsEnabled = true,
                    showOnlyAppMedia = true,
                    confirmDelete = true,
                ),
        )
}