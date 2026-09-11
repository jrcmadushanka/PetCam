package com.civdevops.petcam.feature.camera

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSoundCategories
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode
import com.civdevops.petcam.core.model.camera.RecordingState
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackState

internal const val PHOTO_SHUTTER_TEST_TAG = "photo_shutter"

@Composable
fun CameraPreviewScreen(
    previewStatus: CameraPreviewStatus,
    uiState: CameraUiState,
    onAction: (CameraAction) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRetry: () -> Unit,
    previewContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    canCapturePhoto: Boolean,
    onRequestCapturePermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val screenModifier = if (uiState.keepScreenAwakeWhileRecording && uiState.recordingInProgress) {
        modifier.keepScreenOn()
    } else {
        modifier
    }

    when (previewStatus) {
        CameraPreviewStatus.PERMISSION_REQUIRED ->
            CameraPermissionContent(
                onRequestCameraPermission =
                    onRequestCameraPermission,
                modifier = modifier,
            )

        CameraPreviewStatus.FAILED ->
            CameraFailedContent(
                onRetry = onRetry,
                modifier = modifier,
            )

        CameraPreviewStatus.STARTING,
        CameraPreviewStatus.READY,
            ->
            CameraSurfaceContent(
                status = previewStatus,
                uiState = uiState,
                canCapturePhoto = canCapturePhoto,
                onAction = onAction,
                onRequestCapturePermission = onRequestCapturePermission,
                previewContent = previewContent,
                modifier = screenModifier,
                onOpenSettings = onOpenSettings
            )
    }
}

@Composable
private fun CameraSurfaceContent(
    status: CameraPreviewStatus,
    uiState: CameraUiState,
    onAction: (CameraAction) -> Unit,
    previewContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier,
    canCapturePhoto: Boolean,
    onRequestCapturePermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            previewContent()

            val configuration =
                uiState.configuration

            if (configuration is CameraConfigurationState.Ready) {
                CameraControlBar(
                    configuration = configuration,
                    captureMode = uiState.captureMode,
                    lensSwitchEnabled = !uiState.recordingInProgress,
                    onAction = onAction,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                    videoTorchEnabled = uiState.videoTorchEnabled,
                    videoTorchControlEnabled =
                        uiState.recordingState != RecordingState.Preparing &&
                                uiState.recordingState != RecordingState.Finalizing
                )
            }

            if (status == CameraPreviewStatus.READY) {
                CameraCaptureControls(
                    uiState = uiState,
                    canCapturePhoto = canCapturePhoto,
                    onAction = onAction,
                    onRequestCapturePermission = onRequestCapturePermission,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )
            }

            if (status == CameraPreviewStatus.STARTING) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()

                    Text(
                        text = stringResource(
                            R.string.camera_starting,
                        ),
                        color = Color.White,
                        style = MaterialTheme
                            .typography
                            .bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraCaptureControls(
    uiState: CameraUiState,
    canCapturePhoto: Boolean,
    onAction: (CameraAction) -> Unit,
    onRequestCapturePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AttentionSoundControls(
            state = uiState.attentionSound,
            selectionEnabled = !uiState.recordingInProgress && !uiState.photoShutterPressed,
            playbackEnabled = uiState.recordingState != RecordingState.Preparing &&
                    uiState.recordingState != RecordingState.Finalizing &&
                    !uiState.photoShutterPressed,
            onAction = onAction
        )

        CameraModeSelector(
            selectedMode = uiState.captureMode,
            enabled = !uiState.recordingInProgress,
            onSelected = { onAction(CameraAction.SetCaptureMode(it)) }
        )

        when (uiState.captureMode) {
            CaptureMode.PHOTO -> CameraShutterControls(
                captureState = uiState.photoCapture,
                canCapturePhoto = canCapturePhoto,
                onAction = onAction,
                onRequestCapturePermission = onRequestCapturePermission,
                shutterPressed =  uiState.photoShutterPressed,
            )

            CaptureMode.VIDEO -> CameraVideoControls(uiState, onAction)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttentionSoundControls(
    state: AttentionSoundUiState,
    selectionEnabled: Boolean,
    playbackEnabled: Boolean,
    onAction: (CameraAction) -> Unit
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectionEnabled) {
        if (!selectionEnabled) showPicker = false
    }

    if (state.categories.isEmpty()) {
        Text(
            text = stringResource(R.string.camera_attention_unavailable),
            color = Color.White
        )
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(
            enabled = selectionEnabled,
            onClick = { showPicker = true }
        ) {
            Text(
                text = state.selectedSound?.name
                    ?: stringResource(R.string.camera_attention_unavailable),
                color = Color.White
            )
        }

        Button(
            enabled = playbackEnabled && state.canPlay &&
                    state.playbackState !is AttentionSoundPlaybackState.Loading,
            onClick = { onAction(CameraAction.ToggleAttentionSoundPlayback) }
        ) {
            Text(attentionPlaybackLabel(state.playbackState))
        }
    }

    if (state.playbackState is AttentionSoundPlaybackState.Failed) {
        Text(
            text = stringResource(R.string.camera_attention_failed),
            color = MaterialTheme.colorScheme.error
        )
    }

    if (showPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPicker = false }
        ) {
            AttentionSoundPicker(
                state = state,
                onCategorySelected = {
                    onAction(CameraAction.SelectAttentionCategory(it))
                },
                onSoundSelected = {
                    onAction(CameraAction.SelectAttentionSound(it))
                    showPicker = false
                }
            )
        }
    }
}

@Composable
private fun AttentionSoundPicker(
    state: AttentionSoundUiState,
    onCategorySelected: (PetSoundCategory) -> Unit,
    onSoundSelected: (PetSoundId) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.camera_attention_picker_title),
            style = MaterialTheme.typography.titleLarge
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.categories, key = { it.rawValue }) { category ->
                FilterChip(
                    selected = state.selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(attentionCategoryLabel(category)) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.sounds, key = { it.id.rawValue }) { sound ->
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSoundSelected(sound.id) }
                ) {
                    Text(
                        text = if (state.selectedSoundId == sound.id) {
                            "✓ ${sound.name}"
                        } else {
                            sound.name
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun attentionCategoryLabel(category: PetSoundCategory): String {
    return when (category) {
        PetSoundCategories.Dogs -> stringResource(R.string.camera_attention_dogs)
        PetSoundCategories.Cats -> stringResource(R.string.camera_attention_cats)
        PetSoundCategories.Whistles -> stringResource(R.string.camera_attention_whistles)
        PetSoundCategories.Toys -> stringResource(R.string.camera_attention_toys)
        PetSoundCategories.Other -> stringResource(R.string.camera_attention_other)
        else -> category.rawValue
    }
}

@Composable
private fun attentionPlaybackLabel(
    state: AttentionSoundPlaybackState
): String {
    return when (state) {
        AttentionSoundPlaybackState.Idle,
        is AttentionSoundPlaybackState.Failed ->
            stringResource(R.string.camera_attention_play)

        is AttentionSoundPlaybackState.Loading ->
            stringResource(R.string.camera_attention_loading)

        is AttentionSoundPlaybackState.Playing ->
            stringResource(R.string.camera_attention_pause)

        is AttentionSoundPlaybackState.Paused ->
            stringResource(R.string.camera_attention_resume)
    }
}

@Composable
private fun CameraModeSelector(
    selectedMode: CaptureMode,
    enabled: Boolean,
    onSelected: (CaptureMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selectedMode == CaptureMode.PHOTO,
            onClick = { onSelected(CaptureMode.PHOTO) },
            enabled = enabled,
            label = { Text(stringResource(R.string.camera_mode_photo)) }
        )

        FilterChip(
            selected = selectedMode == CaptureMode.VIDEO,
            onClick = { onSelected(CaptureMode.VIDEO) },
            enabled = enabled,
            label = { Text(stringResource(R.string.camera_mode_video)) }
        )
    }
}

@Composable
private fun CameraVideoControls(
    uiState: CameraUiState,
    onAction: (CameraAction) -> Unit
) {
    val configuration = uiState.configuration as? CameraConfigurationState.Ready ?: return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RecordingStatus(uiState.recordingState)

        when (uiState.videoCapture) {
            is VideoCaptureState.Saved -> Text(
                stringResource(R.string.camera_video_saved),
                color = Color.White
            )

            is VideoCaptureState.Failed -> Text(
                stringResource(R.string.camera_video_failed),
                color = MaterialTheme.colorScheme.error
            )

            VideoCaptureState.Idle -> Unit
        }

        if (uiState.recordingCommandFailure != null) {
            Text(
                stringResource(R.string.camera_recording_action_failed),
                color = MaterialTheme.colorScheme.error
            )
        }

        when (uiState.recordingState) {
            RecordingState.Idle,
            is RecordingState.Failed -> {
                Button(
                    enabled = configuration.videoSupported,
                    onClick = { onAction(CameraAction.StartVideoRecording) }
                ) {
                    Text(
                        if (configuration.videoSupported) {
                            stringResource(R.string.camera_start_recording)
                        } else {
                            stringResource(R.string.camera_video_unavailable)
                        }
                    )
                }
            }

            RecordingState.Preparing -> {
                Button(enabled = false, onClick = {}) {
                    Text(stringResource(R.string.camera_recording_starting))
                }
            }

            is RecordingState.Recording -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAction(CameraAction.PauseVideoRecording) }) {
                        Text(stringResource(R.string.camera_pause_recording))
                    }

                    Button(onClick = { onAction(CameraAction.StopVideoRecording) }) {
                        Text(stringResource(R.string.camera_stop_recording))
                    }
                }
            }

            is RecordingState.Paused -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onAction(CameraAction.ResumeVideoRecording) }) {
                        Text(stringResource(R.string.camera_resume_recording))
                    }

                    Button(onClick = { onAction(CameraAction.StopVideoRecording) }) {
                        Text(stringResource(R.string.camera_stop_recording))
                    }
                }
            }

            RecordingState.Finalizing -> {
                Button(enabled = false, onClick = {}) {
                    Text(stringResource(R.string.camera_recording_saving))
                }
            }
        }
    }
}

@Composable
private fun RecordingStatus(state: RecordingState) {
    val elapsedMillis = when (state) {
        is RecordingState.Recording -> state.elapsedMillis
        is RecordingState.Paused -> state.elapsedMillis
        else -> return
    }

    Text(
        text = formatRecordingDuration(elapsedMillis),
        color = Color.White,
        style = MaterialTheme.typography.titleLarge
    )
}

private fun formatRecordingDuration(elapsedMillis: Long): String {
    val totalSeconds = elapsedMillis / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

@Composable
private fun CameraControlBar(
    configuration: CameraConfigurationState.Ready,
    captureMode: CaptureMode,
    lensSwitchEnabled: Boolean,
    videoTorchEnabled: Boolean,
    videoTorchControlEnabled: Boolean,
    onAction: (CameraAction) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
){
    Row(
        modifier = modifier,
        horizontalArrangement =
            Arrangement.spacedBy(
                12.dp,
            ),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        if (configuration.canSwitchLens) {
            TextButton(
                enabled = lensSwitchEnabled,
                onClick = { onAction(CameraAction.SwitchLens) }
            ) {
                Text(
                    text = if (configuration.lens == CameraLens.BACK) {
                        stringResource(R.string.camera_lens_back)
                    } else {
                        stringResource(R.string.camera_lens_front)
                    },
                    color = Color.White
                )
            }
        }

        when (captureMode) {
            CaptureMode.PHOTO -> {
                TextButton(
                    enabled = configuration.flashSupported,
                    onClick = { onAction(CameraAction.CycleFlash) }
                ) {
                    Text(
                        text = if (!configuration.flashSupported) {
                            stringResource(R.string.camera_flash_unavailable)
                        } else {
                            when (configuration.flashMode) {
                                FlashMode.OFF -> stringResource(R.string.camera_flash_off)
                                FlashMode.ON -> stringResource(R.string.camera_flash_on)
                                FlashMode.AUTO -> stringResource(R.string.camera_flash_auto)
                            }
                        },
                        color = Color.White
                    )
                }
            }

            CaptureMode.VIDEO -> {
                TextButton(
                    enabled = configuration.flashSupported && videoTorchControlEnabled,
                    onClick = { onAction(CameraAction.ToggleVideoTorch) }
                ) {
                    Text(
                        text = if (!configuration.flashSupported) {
                            stringResource(R.string.camera_flash_unavailable)
                        } else if (videoTorchEnabled) {
                            stringResource(R.string.camera_flash_on)
                        } else {
                            stringResource(R.string.camera_flash_off)
                        },
                        color = Color.White
                    )
                }
            }
        }

        TextButton(
            enabled = lensSwitchEnabled,
            onClick = onOpenSettings
        ) {
            Text(
                text = stringResource(R.string.camera_settings),
                color = Color.White
            )
        }
    }
}

@Composable
private fun CameraPermissionContent(
    onRequestCameraPermission: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center,
        ) {
            Text(
                text = stringResource(
                    R.string.camera_permission_title,
                ),
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
            )

            Text(
                text = stringResource(
                    R.string.camera_permission_message,
                ),
                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,
                modifier = Modifier.padding(
                    top = 12.dp,
                    bottom = 24.dp,
                ),
            )

            Button(
                onClick =
                    onRequestCameraPermission,
            ) {
                Text(
                    text = stringResource(
                        R.string.camera_permission_allow,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CameraFailedContent(
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center,
        ) {
            Text(
                text = stringResource(
                    R.string.camera_unavailable_title,
                ),
                style =
                    MaterialTheme
                        .typography
                        .headlineSmall,
            )

            Text(
                text = stringResource(
                    R.string.camera_unavailable_message,
                ),
                modifier = Modifier.padding(
                    top = 12.dp,
                    bottom = 24.dp,
                ),
            )

            Button(
                onClick = onRetry,
            ) {
                Text(
                    text = stringResource(
                        R.string.camera_retry,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CameraShutterControls(
    captureState: PhotoCaptureState,
    shutterPressed: Boolean,
    canCapturePhoto: Boolean,
    onAction: (CameraAction) -> Unit,
    onRequestCapturePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = captureState != PhotoCaptureState.Capturing

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (captureState) {
            is PhotoCaptureState.Saved ->
                Text(stringResource(R.string.camera_photo_saved), color = Color.White)

            is PhotoCaptureState.Failed ->
                Text(
                    stringResource(R.string.camera_photo_failed),
                    color = MaterialTheme.colorScheme.error
                )

            else -> Unit
        }

        PhotoShutterButton(
            enabled = enabled,
            shutterPressed = shutterPressed,
            canCapturePhoto = canCapturePhoto,
            onAction = onAction,
            onRequestCapturePermission = onRequestCapturePermission
        )
    }
}

@Composable
private fun PhotoShutterButton(
    enabled: Boolean,
    shutterPressed: Boolean,
    canCapturePhoto: Boolean,
    onAction: (CameraAction) -> Unit,
    onRequestCapturePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnAction by rememberUpdatedState(onAction)
    val currentPermissionRequest by rememberUpdatedState(onRequestCapturePermission)

    val interactionModifier = Modifier
        .testTag(PHOTO_SHUTTER_TEST_TAG)
        .semantics(mergeDescendants = true) {
            role = Role.Button

            if (!enabled) disabled()

            onClick {
                if (!enabled) return@onClick false

                if (canCapturePhoto) {
                    currentOnAction(CameraAction.CapturePhoto)
                } else {
                    currentPermissionRequest()
                }

                true
            }
        }
        .pointerInput(enabled, canCapturePhoto) {
            if (!enabled) return@pointerInput

            detectTapGestures(
                onPress = {
                    if (!canCapturePhoto) {
                        if (tryAwaitRelease()) {
                            currentPermissionRequest()
                        }

                        return@detectTapGestures
                    }

                    currentOnAction(CameraAction.PhotoShutterPressed)

                    if (tryAwaitRelease()) {
                        currentOnAction(CameraAction.PhotoShutterReleased)
                    } else {
                        currentOnAction(CameraAction.PhotoShutterCancelled)
                    }
                }
            )
        }

    Surface(
        modifier = modifier.then(interactionModifier),
        shape = CircleShape,
        color = if (shutterPressed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (shutterPressed) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (shutterPressed) {
                    stringResource(R.string.camera_release_to_capture)
                } else {
                    stringResource(R.string.camera_capture_photo)
                },
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}