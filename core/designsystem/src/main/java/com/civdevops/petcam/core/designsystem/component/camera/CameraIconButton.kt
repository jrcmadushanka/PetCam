package com.civdevops.petcam.core.designsystem.component.camera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import com.civdevops.petcam.core.designsystem.theme.PetCamMotion

enum class CameraIconButtonTone {
    Neutral,
    Accent,
    Recording,
    Error
}

@Composable
fun CameraIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: CameraIconButtonTone = CameraIconButtonTone.Neutral,
    isSelected: Boolean? = null,
    stateDescription: String? = null,
    size: Dp = CameraControlTokens.controlSize,
    iconSize: Dp = CameraControlTokens.standardIconSize
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(
            durationMillis = PetCamMotion.durationShort,
            easing = PetCamMotion.standardEasing
        ),
        label = "cameraIconButtonScale"
    )

    val colors = cameraControlColors()
    val resolvedColors = resolveCameraIconButtonColors(
        tone = tone,
        colors = colors
    )

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                color = if (enabled) {
                    resolvedColors.container
                } else {
                    resolvedColors.container.copy(
                        alpha = CameraControlTokens.disabledContentAlpha
                    )
                }
            )
            .semantics {
                isSelected?.let { selected = it }
                stateDescription?.let { this.stateDescription = it }
            },
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = resolvedColors.content,
            disabledContentColor = resolvedColors.content.copy(
                alpha = CameraControlTokens.disabledContentAlpha
            )
        ),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}

private data class ResolvedCameraIconButtonColors(
    val container: Color,
    val content: Color
)

private fun resolveCameraIconButtonColors(
    tone: CameraIconButtonTone,
    colors: CameraControlColors
): ResolvedCameraIconButtonColors {
    return when (tone) {
        CameraIconButtonTone.Neutral -> ResolvedCameraIconButtonColors(
            container = colors.container,
            content = colors.content
        )

        CameraIconButtonTone.Accent -> ResolvedCameraIconButtonColors(
            container = colors.selectedContainer,
            content = colors.selectedContent
        )

        CameraIconButtonTone.Recording -> ResolvedCameraIconButtonColors(
            container = colors.recordingContainer,
            content = colors.recordingContent
        )

        CameraIconButtonTone.Error -> ResolvedCameraIconButtonColors(
            container = colors.errorContainer,
            content = colors.errorContent
        )
    }
}