package com.civdevops.petcam.feature.camera

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.civdevops.petcam.core.model.camera.CaptureMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CameraModeSelector(
    selectedMode: CaptureMode,
    enabled: Boolean,
    onSelected: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(CaptureMode.PHOTO, CaptureMode.VIDEO)

    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        modes.forEachIndexed { index, mode ->
            val selected = selectedMode == mode

            SegmentedButton(
                selected = selected,
                onClick = { onSelected(mode) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = modes.size
                ),
                icon = {
                    Icon(
                        imageVector = when (mode) {
                            CaptureMode.PHOTO -> Icons.Rounded.PhotoCamera
                            CaptureMode.VIDEO -> Icons.Rounded.Videocam
                        },
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = when (mode) {
                            CaptureMode.PHOTO -> stringResource(R.string.camera_mode_photo)
                            CaptureMode.VIDEO -> stringResource(R.string.camera_mode_video)
                        }
                    )
                }
            )
        }
    }
}