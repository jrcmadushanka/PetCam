package com.civdevops.petcam.feature.camera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.civdevops.petcam.core.designsystem.component.camera.CameraControlTokens
import com.civdevops.petcam.core.designsystem.component.camera.cameraControlColors
import com.civdevops.petcam.core.designsystem.theme.PetCamMotion

internal const val PHOTO_SHUTTER_TEST_TAG = "photo_shutter"

@Composable
internal fun PhotoShutterButton(
    enabled: Boolean,
    shutterPressed: Boolean,
    canCapturePhoto: Boolean,
    onAction: (CameraAction) -> Unit,
    onRequestCapturePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnAction by rememberUpdatedState(onAction)
    val currentPermissionRequest by rememberUpdatedState(onRequestCapturePermission)
    val colors = cameraControlColors()

    val shutterDescription = if (shutterPressed) {
        stringResource(R.string.camera_release_to_capture)
    } else {
        stringResource(R.string.camera_capture_photo)
    }

    val innerScale by animateFloatAsState(
        targetValue = if (shutterPressed) 0.84f else 1f,
        animationSpec = tween(
            durationMillis = PetCamMotion.durationShort,
            easing = PetCamMotion.standardEasing
        ),
        label = "photoShutterScale"
    )

    val interactionModifier = Modifier
        .testTag(PHOTO_SHUTTER_TEST_TAG)
        .semantics(mergeDescendants = true) {
            role = Role.Button
            contentDescription = shutterDescription

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

    Box(
        modifier = modifier
            .then(interactionModifier)
            .size(CameraControlTokens.photoShutterSize)
            .alpha(
                if (enabled) 1f
                else CameraControlTokens.disabledContentAlpha
            )
            .border(
                width = 4.dp,
                color = colors.content,
                shape = CircleShape
            )
            .padding(7.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = innerScale
                    scaleY = innerScale
                }
                .background(
                    color = if (shutterPressed) {
                        colors.selectedContainer
                    } else {
                        colors.content
                    },
                    shape = CircleShape
                )
        )
    }
}