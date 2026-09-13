package com.civdevops.petcam.feature.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.civdevops.petcam.core.designsystem.component.camera.CameraControlTokens
import com.civdevops.petcam.core.designsystem.component.camera.CameraIconButton
import com.civdevops.petcam.core.designsystem.component.camera.CameraIconButtonTone
import com.civdevops.petcam.core.designsystem.component.camera.cameraControlColors
import com.civdevops.petcam.core.designsystem.theme.PetCamSpacing
import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSoundCategories
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackState

internal const val ATTENTION_SOUND_SUPPORTING_PANE_TEST_TAG = "attention_sound_supporting_pane"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttentionSoundControls(
    state: AttentionSoundUiState,
    selectionEnabled: Boolean,
    playbackEnabled: Boolean,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectionEnabled) {
        if (!selectionEnabled) showPicker = false
    }

    if (state.categories.isEmpty()) {
        Text(
            text = stringResource(R.string.camera_attention_unavailable),
            color = cameraControlColors().content,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PetCamSpacing.small)
    ) {
        SoundSelectionRow(
            state = state,
            selectionEnabled = selectionEnabled,
            playbackEnabled = playbackEnabled,
            onOpenPicker = { showPicker = true },
            onTogglePlayback = {
                onAction(CameraAction.ToggleAttentionSoundPlayback)
            }
        )

        if (state.playbackState is AttentionSoundPlaybackState.Failed) {
            Text(
                text = stringResource(R.string.camera_attention_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            AttentionSoundPicker(
                state = state,
                selectionEnabled = selectionEnabled,
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
private fun SoundSelectionRow(
    state: AttentionSoundUiState,
    selectionEnabled: Boolean,
    playbackEnabled: Boolean,
    onOpenPicker: () -> Unit,
    onTogglePlayback: () -> Unit
) {
    val colors = cameraControlColors()
    val playbackLoading =
        state.playbackState is AttentionSoundPlaybackState.Loading
    val playbackActive =
        state.playbackState is AttentionSoundPlaybackState.Playing

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PetCamSpacing.small)
    ) {
        Button(
            onClick = onOpenPicker,
            enabled = selectionEnabled,
            modifier = Modifier.widthIn(max = CameraControlTokens.compactControlsMaxWidth),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.container,
                contentColor = colors.content,
                disabledContainerColor = colors.container.copy(
                    alpha = CameraControlTokens.disabledContentAlpha
                ),
                disabledContentColor = colors.content.copy(
                    alpha = CameraControlTokens.disabledContentAlpha
                )
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(CameraControlTokens.standardIconSize)
            )

            Text(
                text = state.selectedSound?.name
                    ?: stringResource(R.string.camera_attention_unavailable),
                modifier = Modifier.padding(start = PetCamSpacing.small),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        CameraIconButton(
            imageVector = playbackIcon(state.playbackState),
            contentDescription = attentionPlaybackLabel(state.playbackState),
            onClick = onTogglePlayback,
            enabled = playbackEnabled && state.canPlay && !playbackLoading,
            tone = if (playbackActive) {
                CameraIconButtonTone.Accent
            } else {
                CameraIconButtonTone.Neutral
            },
            isSelected = playbackActive,
            stateDescription = attentionPlaybackLabel(state.playbackState)
        )
    }
}

@Composable
internal fun AttentionSoundSupportingPane(
    state: AttentionSoundUiState,
    selectionEnabled: Boolean,
    playbackEnabled: Boolean,
    onAction: (CameraAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackLoading =
        state.playbackState is AttentionSoundPlaybackState.Loading
    val playbackActive =
        state.playbackState is AttentionSoundPlaybackState.Playing

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .testTag(ATTENTION_SOUND_SUPPORTING_PANE_TEST_TAG),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = PetCamSpacing.extraSmall
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top +
                                WindowInsetsSides.Bottom +
                                WindowInsetsSides.End
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(PetCamSpacing.small)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = PetCamSpacing.extraLarge,
                        end = PetCamSpacing.extraLarge,
                        top = PetCamSpacing.extraLarge
                    ),
                horizontalArrangement =
                    Arrangement.spacedBy(PetCamSpacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(PetCamSpacing.extraSmall)
                ) {
                    Text(
                        text = stringResource(R.string.camera_attention_sound),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = state.selectedSound?.name
                            ?: stringResource(
                                R.string.camera_attention_unavailable
                            ),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                CameraIconButton(
                    imageVector = playbackIcon(state.playbackState),
                    contentDescription =
                        attentionPlaybackLabel(state.playbackState),
                    onClick = {
                        onAction(CameraAction.ToggleAttentionSoundPlayback)
                    },
                    enabled = playbackEnabled &&
                            state.canPlay &&
                            !playbackLoading,
                    tone = if (playbackActive) {
                        CameraIconButtonTone.Accent
                    } else {
                        CameraIconButtonTone.Neutral
                    },
                    isSelected = playbackActive,
                    stateDescription =
                        attentionPlaybackLabel(state.playbackState)
                )
            }

            HorizontalDivider()

            AttentionSoundPicker(
                state = state,
                selectionEnabled = selectionEnabled,
                onCategorySelected = {
                    onAction(CameraAction.SelectAttentionCategory(it))
                },
                onSoundSelected = {
                    onAction(CameraAction.SelectAttentionSound(it))
                }
            )
        }
    }
}

@Composable
internal fun AttentionSoundPicker(
    state: AttentionSoundUiState,
    selectionEnabled: Boolean = true,
    onCategorySelected: (PetSoundCategory) -> Unit,
    onSoundSelected: (PetSoundId) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PetCamSpacing.extraLarge,
                vertical = PetCamSpacing.medium
            ),
        verticalArrangement = Arrangement.spacedBy(PetCamSpacing.medium)
    ) {
        Text(
            text = stringResource(R.string.camera_attention_picker_title),
            style = MaterialTheme.typography.titleLarge
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(PetCamSpacing.small)) {
            items(state.categories, key = { it.rawValue }) { category ->
                FilterChip(
                    enabled = selectionEnabled,
                    selected = state.selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    leadingIcon = {
                        Icon(
                            imageVector = attentionCategoryIcon(category),
                            contentDescription = null,
                            modifier = Modifier.size(CameraControlTokens.standardIconSize)
                        )
                    },
                    label = {
                        Text(attentionCategoryLabel(category))
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = CameraControlTokens.supportingPaneMaxWidth)
        ) {
            items(state.sounds, key = { it.id.rawValue }) { sound ->
                val selected = state.selectedSoundId == sound.id

                ListItem(
                    headlineContent = {
                        Text(
                            text = sound.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null
                        )
                    },
                    trailingContent = {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = selectionEnabled,
                            onClick = { onSoundSelected(sound.id) }
                        )
                )

                HorizontalDivider()
            }
        }
    }
}

private fun playbackIcon(
    state: AttentionSoundPlaybackState
): ImageVector {
    return when (state) {
        AttentionSoundPlaybackState.Idle,
        is AttentionSoundPlaybackState.Failed,
        is AttentionSoundPlaybackState.Paused -> Icons.Rounded.PlayArrow

        is AttentionSoundPlaybackState.Loading -> Icons.Rounded.HourglassTop
        is AttentionSoundPlaybackState.Playing -> Icons.Rounded.Pause
    }
}

private fun attentionCategoryIcon(
    category: PetSoundCategory
): ImageVector {
    return when (category) {
        PetSoundCategories.Dogs,
        PetSoundCategories.Cats -> Icons.Rounded.Pets

        PetSoundCategories.Whistles -> Icons.Rounded.GraphicEq
        PetSoundCategories.Toys -> Icons.Rounded.SmartToy
        PetSoundCategories.Other -> Icons.Rounded.MusicNote
        else -> Icons.Rounded.MusicNote
    }
}

@Composable
private fun attentionCategoryLabel(
    category: PetSoundCategory
): String {
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