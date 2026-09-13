@file:OptIn(ExperimentalMaterial3Api::class)

package com.civdevops.petcam.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.civdevops.petcam.core.designsystem.theme.PetCamSpacing
import com.civdevops.petcam.core.model.audio.PetSoundCategories
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.VideoQuality
import com.civdevops.petcam.core.model.settings.AppSettings
import com.civdevops.petcam.core.model.settings.PetSoundVolumeMode
import com.civdevops.petcam.core.model.share.QuickShareTarget
import kotlin.math.roundToInt

private const val TWO_PANE_MIN_WIDTH_DP = 600

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        SettingsUiState.Loading ->
            SettingsLoading(
                modifier = modifier,
            )

        SettingsUiState.LoadFailed ->
            SettingsLoadFailed(
                onRetry = {
                    onAction(
                        SettingsAction.RetryLoad,
                    )
                },
                modifier = modifier,
            )

        is SettingsUiState.Content ->
            SettingsContent(
                state = uiState,
                onAction = onAction,
                modifier = modifier,
            )
    }
}

@Composable
private fun SettingsLoading(
    modifier: Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = androidx.compose.ui.res.stringResource(
                            R.string.settings_title,
                        ),
                    )
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    }
}

@Composable
private fun SettingsLoadFailed(
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = androidx.compose.ui.res.stringResource(
                            R.string.settings_title,
                        ),
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(PetCamSpacing.large),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.settings_load_failed,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(
                modifier = Modifier.width(
                    PetCamSpacing.medium,
                ),
            )

            Button(
                onClick = onRetry,
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        R.string.settings_retry,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState.Content,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier,
) {
    val adaptiveInfo =
        currentWindowAdaptiveInfoV2()

    val useTwoPaneLayout =
        adaptiveInfo
            .windowSizeClass
            .minWidthDp >= TWO_PANE_MIN_WIDTH_DP

    if (useTwoPaneLayout) {
        ExpandedSettingsContent(
            state = state,
            onAction = onAction,
            modifier = modifier,
        )
    } else {
        CompactSettingsContent(
            state = state,
            onAction = onAction,
            modifier = modifier,
        )
    }
}

@Composable
private fun CompactSettingsContent(
    state: SettingsUiState.Content,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier,
) {
    val selectedSection =
        state.selectedSection

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =
                            selectedSection
                                ?.title()
                                ?: androidx.compose.ui.res.stringResource(
                                    R.string.settings_title,
                                ),
                    )
                },
                navigationIcon = {
                    if (selectedSection != null) {
                        TextButton(
                            onClick = {
                                onAction(
                                    SettingsAction.ShowSectionList,
                                )
                            },
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(
                                    R.string.settings_back,
                                ),
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (state.saveFailed) {
                SaveFailureBanner(
                    onDismiss = {
                        onAction(
                            SettingsAction.DismissSaveError,
                        )
                    },
                )
            }

            if (selectedSection == null) {
                SettingsCategoryList(
                    selectedSection = null,
                    onSectionSelected = { section ->
                        onAction(
                            SettingsAction.SelectSection(
                                section,
                            ),
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal =
                                PetCamSpacing.large,
                        ),
                )
            } else {
                SettingsDetailPane(
                    section = selectedSection,
                    settings = state.settings,
                    onAction = onAction,
                    modifier = Modifier
                        .fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ExpandedSettingsContent(
    state: SettingsUiState.Content,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier,
) {
    val selectedSection =
        state.selectedSection
            ?: SettingsSection.CAMERA

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = androidx.compose.ui.res.stringResource(
                            R.string.settings_title,
                        ),
                    )
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (state.saveFailed) {
                SaveFailureBanner(
                    onDismiss = {
                        onAction(
                            SettingsAction.DismissSaveError,
                        )
                    },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                SettingsCategoryList(
                    selectedSection =
                        selectedSection,
                    onSectionSelected = { section ->
                        onAction(
                            SettingsAction.SelectSection(
                                section,
                            ),
                        )
                    },
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .padding(
                            horizontal =
                                PetCamSpacing.large,
                        ),
                )

                VerticalDivider()

                SettingsDetailPane(
                    section = selectedSection,
                    settings = state.settings,
                    onAction = onAction,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun SettingsCategoryList(
    selectedSection: SettingsSection?,
    onSectionSelected: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(
                rememberScrollState(),
            ),
        verticalArrangement = Arrangement.spacedBy(
            PetCamSpacing.small,
        ),
    ) {
        SettingsSection.entries.forEach { section ->
            val selected =
                section == selectedSection

            Card(
                onClick = {
                    onSectionSelected(section)
                },
                colors = CardDefaults.cardColors(
                    containerColor =
                        if (selected) {
                            MaterialTheme
                                .colorScheme
                                .secondaryContainer
                        } else {
                            MaterialTheme
                                .colorScheme
                                .surfaceContainer
                        },
                ),
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(
                    text = section.title(),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            PetCamSpacing.large,
                        ),
                )
            }
        }
    }
}

@Composable
private fun SettingsDetailPane(
    section: SettingsSection,
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(
                    max = 720.dp,
                )
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState(),
                )
                .padding(
                    PetCamSpacing.large,
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    PetCamSpacing.large,
                ),
        ) {
            when (section) {
                SettingsSection.CAMERA ->
                    CameraSettingsContent(
                        settings = settings,
                        onAction = onAction,
                    )

                SettingsSection.AUDIO ->
                    AudioSettingsContent(
                        settings = settings,
                        onAction = onAction,
                    )

                SettingsSection.SHARING ->
                    SharingSettingsContent(
                        settings = settings,
                        onAction = onAction,
                    )

                SettingsSection.EXPERIENCE ->
                    ExperienceSettingsContent(
                        settings = settings,
                        onAction = onAction,
                    )
            }
        }
    }
}

@Composable
private fun CameraSettingsContent(
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit,
) {
    val camera =
        settings.camera

    SettingsChoiceGroup(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_default_capture_mode,
        ),
        selectedValue = camera.defaultMode,
        choices = listOf(
            SettingsChoice(
                value = CaptureMode.PHOTO,
                label = androidx.compose.ui.res.stringResource(
                    R.string.settings_photo,
                ),
            ),
            SettingsChoice(
                value = CaptureMode.VIDEO,
                label = androidx.compose.ui.res.stringResource(
                    R.string.settings_video,
                ),
            ),
        ),
        onSelected = {
            onAction(
                SettingsAction.SetDefaultCaptureMode(it),
            )
        },
    )

    HorizontalDivider()

    SettingsChoiceGroup(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_default_lens,
        ),
        selectedValue = camera.defaultLens,
        choices = listOf(
            SettingsChoice(
                CameraLens.BACK,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_back_camera,
                ),
            ),
            SettingsChoice(
                CameraLens.FRONT,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_front_camera,
                ),
            ),
        ),
        onSelected = {
            onAction(
                SettingsAction.SetDefaultLens(it),
            )
        },
    )

    HorizontalDivider()

    SettingsChoiceGroup(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_flash,
        ),
        selectedValue = camera.flashMode,
        choices = listOf(
            SettingsChoice(
                FlashMode.OFF,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_flash_off,
                ),
            ),
            SettingsChoice(
                FlashMode.ON,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_flash_on,
                ),
            ),
            SettingsChoice(
                FlashMode.AUTO,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_flash_auto,
                ),
            ),
        ),
        onSelected = {
            onAction(
                SettingsAction.SetFlashMode(it),
            )
        },
    )

    HorizontalDivider()

    SettingsChoiceGroup(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_video_quality,
        ),
        selectedValue = camera.videoQuality,
        choices = listOf(
            SettingsChoice(
                VideoQuality.UHD,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_quality_uhd,
                ),
            ),
            SettingsChoice(
                VideoQuality.FHD,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_quality_fhd,
                ),
            ),
            SettingsChoice(
                VideoQuality.HD,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_quality_hd,
                ),
            ),
            SettingsChoice(
                VideoQuality.SD,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_quality_sd,
                ),
            ),
        ),
        onSelected = {
            onAction(
                SettingsAction.SetVideoQuality(it),
            )
        },
    )

    HorizontalDivider()

    SettingsSwitchRow(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_record_audio,
        ),
        checked = camera.recordAudio,
        onCheckedChange = {
            onAction(
                SettingsAction.SetRecordAudio(it),
            )
        },
    )
}

@Composable
private fun AudioSettingsContent(
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit,
) {
    val audio =
        settings.audio

    SettingsChoiceGroup(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_default_sound_category,
        ),
        selectedValue = audio.defaultCategory,
        choices = listOf(
            SettingsChoice(
                PetSoundCategories.Dogs,
                androidx.compose.ui.res.stringResource(R.string.settings_category_dogs)
            ),
            SettingsChoice(
                PetSoundCategories.Cats,
                androidx.compose.ui.res.stringResource(R.string.settings_category_cats)
            ),
            SettingsChoice(
                PetSoundCategories.Whistles,
                androidx.compose.ui.res.stringResource(R.string.settings_category_whistles)
            ),
            SettingsChoice(
                PetSoundCategories.Toys,
                androidx.compose.ui.res.stringResource(R.string.settings_category_toys)
            ),
            SettingsChoice(
                PetSoundCategories.Other,
                androidx.compose.ui.res.stringResource(R.string.settings_category_other)
            )
        ),
        onSelected = {
            onAction(
                SettingsAction.SetDefaultSoundCategory(
                    it,
                ),
            )
        },
    )

    HorizontalDivider()

    SettingsChoiceGroup(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_volume_mode,
        ),
        selectedValue = audio.volumeMode,
        choices = listOf(
            SettingsChoice(
                PetSoundVolumeMode.FollowDevice,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_follow_device,
                ),
            ),
            SettingsChoice(
                PetSoundVolumeMode.MaximumAppOutput,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_maximum_app_output,
                ),
            ),
            SettingsChoice(
                PetSoundVolumeMode.Custom,
                androidx.compose.ui.res.stringResource(
                    R.string.settings_custom_volume,
                ),
            ),
        ),
        onSelected = {
            onAction(
                SettingsAction.SetVolumeMode(it),
            )
        },
    )

    SettingsVolumeSlider(
        value = audio.customVolumePercent,
        enabled =
            audio.volumeMode ==
                    PetSoundVolumeMode.Custom,
        onValueCommitted = {
            onAction(
                SettingsAction.SetCustomVolume(it),
            )
        },
    )

    HorizontalDivider()

    SettingsSwitchRow(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_loop_during_recording,
        ),
        checked = audio.loopDuringRecording,
        onCheckedChange = {
            onAction(
                SettingsAction.SetLoopDuringRecording(
                    it,
                ),
            )
        },
    )

    SettingsSwitchRow(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_play_on_photo_capture,
        ),
        checked = audio.playOnPhotoCapture,
        onCheckedChange = {
            onAction(
                SettingsAction.SetPlayOnPhotoCapture(
                    it,
                ),
            )
        },
    )
}

@Composable
private fun SharingSettingsContent(
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit,
) {
    val sharing =
        settings.sharing

    SettingsSwitchRow(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_auto_open_share,
        ),
        checked =
            sharing.autoOpenShareAfterCapture,
        onCheckedChange = {
            onAction(
                SettingsAction
                    .SetAutoOpenShareAfterCapture(it),
            )
        },
    )

    HorizontalDivider()

    SettingsChoiceGroup(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_preferred_share_target,
        ),
        selectedValue =
            sharing.preferredQuickShareTarget,
        choices =
            listOf<
                    SettingsChoice<QuickShareTarget?>
                    >(
                SettingsChoice(
                    null,
                    androidx.compose.ui.res.stringResource(
                        R.string.settings_share_none,
                    ),
                ),
                SettingsChoice(
                    QuickShareTarget.TIKTOK,
                    androidx.compose.ui.res.stringResource(
                        R.string.settings_share_tiktok,
                    ),
                ),
                SettingsChoice(
                    QuickShareTarget.CAPCUT,
                    androidx.compose.ui.res.stringResource(
                        R.string.settings_share_capcut,
                    ),
                ),
                SettingsChoice(
                    QuickShareTarget.INSTAGRAM,
                    androidx.compose.ui.res.stringResource(
                        R.string.settings_share_instagram,
                    ),
                ),
                SettingsChoice(
                    QuickShareTarget.WHATSAPP,
                    androidx.compose.ui.res.stringResource(
                        R.string.settings_share_whatsapp,
                    ),
                ),
            ),
        onSelected = {
            onAction(
                SettingsAction
                    .SetPreferredQuickShareTarget(
                        it,
                    ),
            )
        },
    )
}

@Composable
private fun ExperienceSettingsContent(
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit,
) {
    val experience =
        settings.experience

    SettingsSwitchRow(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_keep_screen_awake,
        ),
        checked =
            experience.keepScreenAwakeWhileRecording,
        onCheckedChange = {
            onAction(
                SettingsAction.SetKeepScreenAwake(
                    it,
                ),
            )
        },
    )

    SettingsSwitchRow(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_haptics,
        ),
        checked = experience.hapticsEnabled,
        onCheckedChange = {
            onAction(
                SettingsAction.SetHapticsEnabled(
                    it,
                ),
            )
        },
    )

    SettingsSwitchRow(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_show_only_app_media,
        ),
        checked = experience.showOnlyAppMedia,
        onCheckedChange = {
            onAction(
                SettingsAction.SetShowOnlyAppMedia(
                    it,
                ),
            )
        },
    )

    SettingsSwitchRow(
        title = androidx.compose.ui.res.stringResource(
            R.string.settings_confirm_delete,
        ),
        checked = experience.confirmDelete,
        onCheckedChange = {
            onAction(
                SettingsAction.SetConfirmDelete(
                    it,
                ),
            )
        },
    )
}

@Composable
private fun <T> SettingsChoiceGroup(
    title: String,
    selectedValue: T,
    choices: List<SettingsChoice<T>>,
    onSelected: (T) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                PetCamSpacing.small,
            ),
    ) {
        Text(
            text = title,
            style =
                MaterialTheme
                    .typography
                    .titleMedium,
        )

        Column(
            modifier = Modifier
                .selectableGroup(),
        ) {
            choices.forEach { choice ->
                val selected =
                    choice.value == selectedValue

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = {
                                onSelected(
                                    choice.value,
                                )
                            },
                        )
                        .padding(
                            vertical =
                                PetCamSpacing.small,
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                    )

                    Text(
                        text = choice.label,
                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge,
                        modifier = Modifier
                            .padding(
                                start =
                                    PetCamSpacing.small,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(
                vertical = PetCamSpacing.medium,
            ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style =
                MaterialTheme
                    .typography
                    .bodyLarge,
            modifier = Modifier
                .weight(1f),
        )

        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun SettingsVolumeSlider(
    value: Int,
    enabled: Boolean,
    onValueCommitted: (Int) -> Unit,
) {
    var sliderValue by remember(value) {
        mutableFloatStateOf(
            value.toFloat(),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(
                R.string.settings_custom_volume_percent,
                sliderValue.roundToInt(),
            ),
            style =
                MaterialTheme
                    .typography
                    .bodyLarge,
        )

        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
            },
            onValueChangeFinished = {
                onValueCommitted(
                    sliderValue
                        .roundToInt()
                        .coerceIn(0, 100),
                )
            },
            enabled = enabled,
            valueRange = 0f..100f,
        )
    }
}

@Composable
private fun SaveFailureBanner(
    onDismiss: () -> Unit,
) {
    Surface(
        color =
            MaterialTheme
                .colorScheme
                .errorContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        PetCamSpacing.large,
                    vertical =
                        PetCamSpacing.small,
                ),
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.settings_save_failed,
                ),
                color =
                    MaterialTheme
                        .colorScheme
                        .onErrorContainer,
                modifier = Modifier
                    .weight(1f),
            )

            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        R.string.settings_dismiss,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SettingsSection.title(): String =
    when (this) {
        SettingsSection.CAMERA ->
            androidx.compose.ui.res.stringResource(
                R.string.settings_camera,
            )

        SettingsSection.AUDIO ->
            androidx.compose.ui.res.stringResource(
                R.string.settings_audio,
            )

        SettingsSection.SHARING ->
            androidx.compose.ui.res.stringResource(
                R.string.settings_sharing,
            )

        SettingsSection.EXPERIENCE ->
            androidx.compose.ui.res.stringResource(
                R.string.settings_experience,
            )
    }

private data class SettingsChoice<T>(
    val value: T,
    val label: String,
)