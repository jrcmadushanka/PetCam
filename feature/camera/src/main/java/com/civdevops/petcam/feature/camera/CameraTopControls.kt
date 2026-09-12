package com.civdevops.petcam.feature.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.civdevops.petcam.core.designsystem.component.camera.CameraIconButton
import com.civdevops.petcam.core.designsystem.component.camera.CameraIconButtonTone
import com.civdevops.petcam.core.designsystem.theme.PetCamSpacing
import com.civdevops.petcam.core.model.camera.CameraLens
import com.civdevops.petcam.core.model.camera.CaptureMode
import com.civdevops.petcam.core.model.camera.FlashMode

internal const val FLASH_CONTROL_TEST_TAG = "camera_flash_control"
internal const val LENS_CONTROL_TEST_TAG = "camera_lens_control"
internal const val SETTINGS_CONTROL_TEST_TAG = "camera_settings_control"

@Composable
internal fun CameraTopControls(
    configuration: CameraConfigurationState.Ready,
    captureMode: CaptureMode,
    lensSwitchEnabled: Boolean,
    videoTorchEnabled: Boolean,
    videoTorchControlEnabled: Boolean,
    onAction: (CameraAction) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PetCamSpacing.medium)
    ) {
        when (captureMode) {
            CaptureMode.PHOTO -> PhotoFlashControl(
                configuration = configuration,
                onAction = onAction
            )

            CaptureMode.VIDEO -> VideoTorchControl(
                configuration = configuration,
                torchEnabled = videoTorchEnabled,
                controlEnabled = videoTorchControlEnabled,
                onAction = onAction
            )
        }

        if (configuration.canSwitchLens) {
            LensControl(
                lens = configuration.lens,
                enabled = lensSwitchEnabled,
                onAction = onAction
            )
        }

        CameraIconButton(
            imageVector = Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.camera_settings),
            onClick = onOpenSettings,
            modifier = Modifier.testTag(SETTINGS_CONTROL_TEST_TAG)
        )
    }
}

@Composable
private fun PhotoFlashControl(
    configuration: CameraConfigurationState.Ready,
    onAction: (CameraAction) -> Unit
) {
    val supported = configuration.flashSupported
    val flashMode = configuration.flashMode

    val label = when {
        !supported -> stringResource(R.string.camera_flash_unavailable)
        flashMode == FlashMode.OFF -> stringResource(R.string.camera_flash_off)
        flashMode == FlashMode.ON -> stringResource(R.string.camera_flash_on)
        else -> stringResource(R.string.camera_flash_auto)
    }

    val icon = when (flashMode) {
        FlashMode.OFF -> Icons.Rounded.FlashOff
        FlashMode.ON -> Icons.Rounded.FlashOn
        FlashMode.AUTO -> Icons.Rounded.FlashAuto
    }

    val active = supported && flashMode != FlashMode.OFF

    CameraIconButton(
        imageVector = icon,
        contentDescription = label,
        onClick = { onAction(CameraAction.CycleFlash) },
        modifier = Modifier.testTag(FLASH_CONTROL_TEST_TAG),
        enabled = supported,
        tone = if (active) CameraIconButtonTone.Accent else CameraIconButtonTone.Neutral,
        isSelected = if (supported) active else null,
        stateDescription = label
    )
}

@Composable
private fun VideoTorchControl(
    configuration: CameraConfigurationState.Ready,
    torchEnabled: Boolean,
    controlEnabled: Boolean,
    onAction: (CameraAction) -> Unit
) {
    val supported = configuration.flashSupported

    val label = when {
        !supported -> stringResource(R.string.camera_torch_unavailable)
        torchEnabled -> stringResource(R.string.camera_torch_on)
        else -> stringResource(R.string.camera_torch_off)
    }

    CameraIconButton(
        imageVector = if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
        contentDescription = label,
        onClick = { onAction(CameraAction.ToggleVideoTorch) },
        modifier = Modifier.testTag(FLASH_CONTROL_TEST_TAG),
        enabled = supported && controlEnabled,
        tone = if (torchEnabled) CameraIconButtonTone.Accent else CameraIconButtonTone.Neutral,
        isSelected = if (supported) torchEnabled else null,
        stateDescription = label
    )
}

@Composable
private fun LensControl(
    lens: CameraLens,
    enabled: Boolean,
    onAction: (CameraAction) -> Unit
) {
    val lensDescription = when (lens) {
        CameraLens.BACK -> stringResource(R.string.camera_current_lens_back)
        CameraLens.FRONT -> stringResource(R.string.camera_current_lens_front)
    }

    CameraIconButton(
        imageVector = Icons.Rounded.Cameraswitch,
        contentDescription = stringResource(R.string.camera_switch_lens),
        onClick = { onAction(CameraAction.SwitchLens) },
        modifier = Modifier.testTag(LENS_CONTROL_TEST_TAG),
        enabled = enabled,
        stateDescription = lensDescription
    )
}