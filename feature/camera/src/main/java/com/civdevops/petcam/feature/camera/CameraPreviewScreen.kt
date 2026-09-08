package com.civdevops.petcam.feature.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.FlashMode

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
) {
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
                modifier = modifier
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
                    onAction = onAction,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp),
                )
            }

            if (status == CameraPreviewStatus.READY) {
                CameraShutterControls(
                    captureState = uiState.photoCapture,
                    canCapturePhoto = canCapturePhoto,
                    onAction = onAction,
                    onRequestCapturePermission = onRequestCapturePermission,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
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
private fun CameraControlBar(
    configuration:
    CameraConfigurationState.Ready,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                onClick = {
                    onAction(
                        CameraAction.SwitchLens,
                    )
                },
            ) {
                Text(
                    text =
                        when (
                            configuration.lens
                        ) {
                            CameraLens.BACK ->
                                stringResource(
                                    R.string
                                        .camera_lens_back,
                                )

                            CameraLens.FRONT ->
                                stringResource(
                                    R.string
                                        .camera_lens_front,
                                )
                        },
                    color = Color.White,
                )
            }
        }

        TextButton(
            enabled =
                configuration.flashSupported,
            onClick = {
                onAction(
                    CameraAction.CycleFlash,
                )
            },
        ) {
            Text(
                text =
                    if (
                        !configuration
                            .flashSupported
                    ) {
                        stringResource(
                            R.string
                                .camera_flash_unavailable,
                        )
                    } else {
                        when (
                            configuration.flashMode
                        ) {
                            FlashMode.OFF ->
                                stringResource(
                                    R.string
                                        .camera_flash_off,
                                )

                            FlashMode.ON ->
                                stringResource(
                                    R.string
                                        .camera_flash_on,
                                )

                            FlashMode.AUTO ->
                                stringResource(
                                    R.string
                                        .camera_flash_auto,
                                )
                        }
                    },
                color = Color.White,
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
    canCapturePhoto: Boolean,
    onAction: (CameraAction) -> Unit,
    onRequestCapturePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (captureState) {
            is PhotoCaptureState.Saved ->
                Text(stringResource(R.string.camera_photo_saved), color = Color.White)

            is PhotoCaptureState.Failed ->
                Text(stringResource(R.string.camera_photo_failed), color = MaterialTheme.colorScheme.error)

            else -> Unit
        }

        Button(
            enabled = captureState != PhotoCaptureState.Capturing,
            onClick = {
                if (canCapturePhoto) onAction(CameraAction.CapturePhoto)
                else onRequestCapturePermission()
            }
        ) {
            Text(
                if (captureState == PhotoCaptureState.Capturing) {
                    stringResource(R.string.camera_capturing)
                } else {
                    stringResource(R.string.camera_capture_photo)
                }
            )
        }
    }
}