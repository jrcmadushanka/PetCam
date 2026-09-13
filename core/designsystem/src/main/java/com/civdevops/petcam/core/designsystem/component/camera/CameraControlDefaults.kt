package com.civdevops.petcam.core.designsystem.component.camera

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.civdevops.petcam.core.designsystem.theme.CameraControlSurface
import com.civdevops.petcam.core.designsystem.theme.CameraRecording
import com.civdevops.petcam.core.designsystem.theme.CameraScrim
import com.civdevops.petcam.core.designsystem.theme.OnCameraControl
import com.civdevops.petcam.core.designsystem.theme.OnCameraRecording

object CameraControlTokens {
    val minimumTouchTarget: Dp = 48.dp
    val controlSize: Dp = 48.dp
    val prominentControlSize: Dp = 56.dp
    val photoShutterSize: Dp = 80.dp
    val videoShutterSize: Dp = 72.dp
    val standardIconSize: Dp = 24.dp
    val prominentIconSize: Dp = 28.dp
    val supportingPaneMinWidth: Dp = 320.dp
    val supportingPaneMaxWidth: Dp = 400.dp
    val compactControlsMaxWidth: Dp = 480.dp
    val overlayContentMaxWidth: Dp = 560.dp
    val supportingPanePreferredWidth: Dp = 360.dp

    const val disabledContentAlpha = 0.38f
    const val secondaryContentAlpha = 0.72f
}

@Immutable
data class CameraControlColors(
    val scrim: Color,
    val container: Color,
    val content: Color,
    val selectedContainer: Color,
    val selectedContent: Color,
    val recordingContainer: Color,
    val recordingContent: Color,
    val errorContainer: Color,
    val errorContent: Color
)

@Composable
fun cameraControlColors(): CameraControlColors {
    val colorScheme = MaterialTheme.colorScheme

    return CameraControlColors(
        scrim = CameraScrim,
        container = CameraControlSurface,
        content = OnCameraControl,
        selectedContainer = colorScheme.primaryContainer,
        selectedContent = colorScheme.onPrimaryContainer,
        recordingContainer = CameraRecording,
        recordingContent = OnCameraRecording,
        errorContainer = colorScheme.errorContainer,
        errorContent = colorScheme.onErrorContainer
    )
}