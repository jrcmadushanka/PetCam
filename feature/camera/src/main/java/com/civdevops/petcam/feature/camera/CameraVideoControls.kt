package com.civdevops.petcam.feature.camera

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.civdevops.petcam.core.designsystem.component.camera.CameraControlTokens
import com.civdevops.petcam.core.designsystem.component.camera.CameraIconButton
import com.civdevops.petcam.core.designsystem.component.camera.CameraIconButtonTone
import com.civdevops.petcam.core.designsystem.component.camera.cameraControlColors
import com.civdevops.petcam.core.designsystem.theme.PetCamMotion
import com.civdevops.petcam.core.designsystem.theme.PetCamSpacing
import com.civdevops.petcam.core.model.camera.RecordingState

@Composable
internal fun CameraVideoControls(
    uiState: CameraUiState,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration =
        uiState.configuration as? CameraConfigurationState.Ready ?: return
    val hapticFeedback = LocalHapticFeedback.current

    fun dispatchWithHaptic(action: CameraAction) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
        onAction(action)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PetCamSpacing.small)
    ) {
        RecordingStatus(uiState.recordingState)

        when (uiState.videoCapture) {
            is VideoCaptureState.Saved -> CameraStatusMessage(
                message = stringResource(R.string.camera_video_saved),
                error = false
            )

            is VideoCaptureState.Failed -> CameraStatusMessage(
                message = stringResource(R.string.camera_video_failed),
                error = true
            )

            VideoCaptureState.Idle -> Unit
        }

        if (uiState.recordingCommandFailure != null) {
            CameraStatusMessage(
                message = stringResource(R.string.camera_recording_action_failed),
                error = true
            )
        }

        RecordingControlTransition(
            state = uiState.recordingState,
            videoSupported = configuration.videoSupported,
            onStart = {
                dispatchWithHaptic(CameraAction.StartVideoRecording)
            },
            onPause = {
                dispatchWithHaptic(CameraAction.PauseVideoRecording)
            },
            onResume = {
                dispatchWithHaptic(CameraAction.ResumeVideoRecording)
            },
            onStop = {
                dispatchWithHaptic(CameraAction.StopVideoRecording)
            }
        )
    }
}

@Composable
private fun RecordingControlTransition(
    state: RecordingState,
    videoSupported: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    AnimatedContent(
        targetState = state.toControlState(),
        transitionSpec = {
            (
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = PetCamMotion.durationMedium,
                            easing = PetCamMotion.standardEasing
                        )
                    ) + scaleIn(initialScale = 0.92f)
                    ) togetherWith (
                    fadeOut(
                        animationSpec = tween(
                            durationMillis = PetCamMotion.durationShort,
                            easing = PetCamMotion.standardEasing
                        )
                    ) + scaleOut(targetScale = 0.92f)
                    )
        },
        contentAlignment = Alignment.Center,
        label = "recordingControlTransition"
    ) { controlState ->
        when (controlState) {
            RecordingControlState.IDLE,
            RecordingControlState.FAILED -> {
                CameraIconButton(
                    imageVector = Icons.Rounded.FiberManualRecord,
                    contentDescription = if (videoSupported) {
                        stringResource(R.string.camera_start_recording)
                    } else {
                        stringResource(R.string.camera_video_unavailable)
                    },
                    onClick = onStart,
                    enabled = videoSupported,
                    tone = CameraIconButtonTone.Recording,
                    size = CameraControlTokens.videoShutterSize,
                    iconSize = CameraControlTokens.prominentIconSize
                )
            }

            RecordingControlState.PREPARING -> {
                CameraIconButton(
                    imageVector = Icons.Rounded.HourglassTop,
                    contentDescription = stringResource(
                        R.string.camera_recording_starting
                    ),
                    onClick = {},
                    enabled = false,
                    tone = CameraIconButtonTone.Recording,
                    size = CameraControlTokens.videoShutterSize,
                    iconSize = CameraControlTokens.prominentIconSize
                )
            }

            RecordingControlState.RECORDING -> {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(PetCamSpacing.large),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CameraIconButton(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = stringResource(
                            R.string.camera_pause_recording
                        ),
                        onClick = onPause,
                        tone = CameraIconButtonTone.Accent,
                        size = CameraControlTokens.prominentControlSize,
                        iconSize = CameraControlTokens.prominentIconSize
                    )

                    CameraIconButton(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = stringResource(
                            R.string.camera_stop_recording
                        ),
                        onClick = onStop,
                        tone = CameraIconButtonTone.Recording,
                        size = CameraControlTokens.prominentControlSize,
                        iconSize = CameraControlTokens.prominentIconSize
                    )
                }
            }

            RecordingControlState.PAUSED -> {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(PetCamSpacing.large),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CameraIconButton(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(
                            R.string.camera_resume_recording
                        ),
                        onClick = onResume,
                        tone = CameraIconButtonTone.Accent,
                        size = CameraControlTokens.prominentControlSize,
                        iconSize = CameraControlTokens.prominentIconSize
                    )

                    CameraIconButton(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = stringResource(
                            R.string.camera_stop_recording
                        ),
                        onClick = onStop,
                        tone = CameraIconButtonTone.Recording,
                        size = CameraControlTokens.prominentControlSize,
                        iconSize = CameraControlTokens.prominentIconSize
                    )
                }
            }

            RecordingControlState.FINALIZING -> {
                CameraIconButton(
                    imageVector = Icons.Rounded.HourglassTop,
                    contentDescription = stringResource(
                        R.string.camera_recording_saving
                    ),
                    onClick = {},
                    enabled = false,
                    tone = CameraIconButtonTone.Recording,
                    size = CameraControlTokens.videoShutterSize,
                    iconSize = CameraControlTokens.prominentIconSize
                )
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

    val colors = cameraControlColors()
    val activelyRecording = state is RecordingState.Recording

    Surface(
        shape = CircleShape,
        color = colors.container,
        contentColor = colors.content
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = PetCamSpacing.medium,
                vertical = PetCamSpacing.small
            ),
            horizontalArrangement = Arrangement.spacedBy(PetCamSpacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(PetCamSpacing.small)
                    .background(
                        color = if (activelyRecording) {
                            colors.recordingContainer
                        } else {
                            colors.content.copy(
                                alpha = CameraControlTokens.secondaryContentAlpha
                            )
                        },
                        shape = CircleShape
                    )
            )

            Text(
                text = formatRecordingDuration(elapsedMillis),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun CameraStatusMessage(
    message: String,
    error: Boolean
) {
    val colors = cameraControlColors()

    Surface(
        shape = CircleShape,
        color = if (error) colors.errorContainer else colors.container,
        contentColor = if (error) colors.errorContent else colors.content
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(
                horizontal = PetCamSpacing.medium,
                vertical = PetCamSpacing.small
            ),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private enum class RecordingControlState {
    IDLE,
    PREPARING,
    RECORDING,
    PAUSED,
    FINALIZING,
    FAILED
}

private fun RecordingState.toControlState(): RecordingControlState {
    return when (this) {
        RecordingState.Idle -> RecordingControlState.IDLE
        RecordingState.Preparing -> RecordingControlState.PREPARING
        is RecordingState.Recording -> RecordingControlState.RECORDING
        is RecordingState.Paused -> RecordingControlState.PAUSED
        RecordingState.Finalizing -> RecordingControlState.FINALIZING
        is RecordingState.Failed -> RecordingControlState.FAILED
    }
}

private fun formatRecordingDuration(
    elapsedMillis: Long
): String {
    val totalSeconds = elapsedMillis / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "${minutes.toString().padStart(2, '0')}:" +
            seconds.toString().padStart(2, '0')
}