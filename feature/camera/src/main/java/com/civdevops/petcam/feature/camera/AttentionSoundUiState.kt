package com.civdevops.petcam.feature.camera

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSound
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackState

data class AttentionSoundUiState(
    val categories: List<PetSoundCategory> = emptyList(),
    val selectedCategory: PetSoundCategory? = null,
    val sounds: List<PetSound> = emptyList(),
    val selectedSoundId: PetSoundId? = null,
    val playbackState: AttentionSoundPlaybackState = AttentionSoundPlaybackState.Idle
) {
    val selectedSound: PetSound?
        get() = sounds.firstOrNull { it.id == selectedSoundId }

    val canPlay: Boolean
        get() = selectedSoundId != null
}