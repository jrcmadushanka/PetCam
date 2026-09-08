package com.civdevops.petcam.core.model.settings

sealed interface PetSoundVolumeMode {

    data object FollowDevice : PetSoundVolumeMode

    data object MaximumAppOutput : PetSoundVolumeMode

    data object Custom : PetSoundVolumeMode
}