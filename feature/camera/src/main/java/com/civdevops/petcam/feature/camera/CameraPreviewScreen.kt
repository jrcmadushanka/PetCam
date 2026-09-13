package com.civdevops.petcam.feature.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.civdevops.petcam.core.designsystem.component.camera.CameraControlTokens
import com.civdevops.petcam.core.designsystem.component.camera.cameraControlColors
import com.civdevops.petcam.core.designsystem.theme.CameraPreviewBackground
import com.civdevops.petcam.core.designsystem.theme.PetCamMotion
import com.civdevops.petcam.core.designsystem.theme.PetCamSpacing
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.RecordingState

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
    CameraPreviewScreenContent(
        previewStatus = previewStatus,
        uiState = uiState,
        layoutMode = currentCameraLayoutMode(),
        onAction = onAction,
        onRequestCameraPermission = onRequestCameraPermission,
        onRetry = onRetry,
        previewContent = previewContent,
        modifier = modifier,
        canCapturePhoto = canCapturePhoto,
        onRequestCapturePermission = onRequestCapturePermission,
        onOpenSettings = onOpenSettings
    )
}

@Composable
internal fun CameraPreviewScreenContent(
    previewStatus: CameraPreviewStatus,
    uiState: CameraUiState,
    layoutMode: CameraLayoutMode,
    onAction: (CameraAction) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRetry: () -> Unit,
    previewContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    canCapturePhoto: Boolean,
    onRequestCapturePermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val screenModifier =
        if (
            uiState.keepScreenAwakeWhileRecording &&
            uiState.recordingInProgress
        ) {
            modifier.keepScreenOn()
        } else {
            modifier
        }

    when (previewStatus) {
        CameraPreviewStatus.PERMISSION_REQUIRED -> {
            CameraPermissionContent(
                onRequestCameraPermission = onRequestCameraPermission,
                modifier = modifier
            )
        }

        CameraPreviewStatus.FAILED -> {
            CameraFailedContent(
                onRetry = onRetry,
                modifier = modifier
            )
        }

        CameraPreviewStatus.STARTING,
        CameraPreviewStatus.READY -> {
            CameraSurfaceContent(
                status = previewStatus,
                uiState = uiState,
                layoutMode = layoutMode,
                canCapturePhoto = canCapturePhoto,
                onAction = onAction,
                onRequestCapturePermission =
                    onRequestCapturePermission,
                previewContent = previewContent,
                modifier = screenModifier,
                onOpenSettings = onOpenSettings
            )
        }
    }
}

@Composable
private fun CameraSurfaceContent(
    status: CameraPreviewStatus,
    uiState: CameraUiState,
    layoutMode: CameraLayoutMode,
    onAction: (CameraAction) -> Unit,
    previewContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier,
    canCapturePhoto: Boolean,
    onRequestCapturePermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val configuration =
        uiState.configuration as? CameraConfigurationState.Ready

    val showSupportingPane =
        layoutMode == CameraLayoutMode.EXPANDED &&
                status == CameraPreviewStatus.READY &&
                configuration != null

    Row(modifier = modifier.fillMaxSize()) {
        CameraPreviewPane(
            status = status,
            uiState = uiState,
            showAttentionSoundControl = !showSupportingPane,
            canCapturePhoto = canCapturePhoto,
            onAction = onAction,
            onRequestCapturePermission =
                onRequestCapturePermission,
            previewContent = previewContent,
            onOpenSettings = onOpenSettings,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        AnimatedVisibility(
            visible = showSupportingPane,
            enter = expandHorizontally(
                expandFrom = Alignment.End,
                animationSpec = tween(
                    durationMillis = PetCamMotion.durationLong,
                    easing = PetCamMotion.emphasizedEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = PetCamMotion.durationMedium
                )
            ),
            exit = shrinkHorizontally(
                shrinkTowards = Alignment.End,
                animationSpec = tween(
                    durationMillis = PetCamMotion.durationMedium,
                    easing = PetCamMotion.standardEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = PetCamMotion.durationShort
                )
            )
        ) {
            AttentionSoundSupportingPane(
                state = uiState.attentionSound,
                selectionEnabled =
                    !uiState.recordingInProgress &&
                            !uiState.photoShutterPressed,
                playbackEnabled =
                    uiState.recordingState != RecordingState.Preparing &&
                            uiState.recordingState != RecordingState.Finalizing &&
                            !uiState.photoShutterPressed,
                onAction = onAction,
                modifier = Modifier
                    .width(
                        CameraControlTokens.supportingPanePreferredWidth
                    )
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun CameraPreviewPane(
    status: CameraPreviewStatus,
    uiState: CameraUiState,
    showAttentionSoundControl: Boolean,
    onAction: (CameraAction) -> Unit,
    previewContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier,
    canCapturePhoto: Boolean,
    onRequestCapturePermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = CameraPreviewBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            previewContent()

            val configuration = uiState.configuration

            if (configuration is CameraConfigurationState.Ready) {
                CameraTopControls(
                    configuration = configuration,
                    captureMode = uiState.captureMode,
                    lensSwitchEnabled =
                        !uiState.recordingInProgress,
                    videoTorchEnabled =
                        uiState.videoTorchEnabled,
                    videoTorchControlEnabled =
                        uiState.recordingState !=
                                RecordingState.Preparing &&
                                uiState.recordingState !=
                                RecordingState.Finalizing,
                    onAction = onAction,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top
                            )
                        )
                        .padding(
                            top = PetCamSpacing.small,
                            start = PetCamSpacing.large,
                            end = PetCamSpacing.large
                        )
                )
            }

            if (status == CameraPreviewStatus.READY) {
                CameraCaptureControls(
                    uiState = uiState,
                    showAttentionSoundControl =
                        showAttentionSoundControl,
                    canCapturePhoto = canCapturePhoto,
                    onAction = onAction,
                    onRequestCapturePermission =
                        onRequestCapturePermission,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Bottom
                            )
                        )
                        .padding(bottom = PetCamSpacing.large)
                )
            }

            if (status == CameraPreviewStatus.STARTING) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.spacedBy(PetCamSpacing.medium)
                ) {
                    CircularProgressIndicator()

                    Text(
                        text = stringResource(
                            R.string.camera_starting
                        ),
                        color = cameraControlColors().content,
                        style = MaterialTheme.typography.bodyMedium
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
    showAttentionSoundControl: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showAttentionSoundControl) {
            AttentionSoundControls(
                state = uiState.attentionSound,
                selectionEnabled = !uiState.recordingInProgress && !uiState.photoShutterPressed,
                playbackEnabled = uiState.recordingState != RecordingState.Preparing &&
                        uiState.recordingState != RecordingState.Finalizing &&
                        !uiState.photoShutterPressed,
                onAction = onAction
            )
        }

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
                shutterPressed = uiState.photoShutterPressed,
            )

            CaptureMode.VIDEO -> CameraVideoControls(uiState, onAction)
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
