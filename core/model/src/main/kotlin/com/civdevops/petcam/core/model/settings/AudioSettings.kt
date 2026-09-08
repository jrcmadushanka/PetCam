package com.civdevops.petcam.core.model.settings

import com.civdevops.petcam.core.model.audio.PetSoundCategory

data class AudioSettings(
    val defaultCategory: PetSoundCategory,
    val volumeMode: PetSoundVolumeMode,
    val customVolumePercent: Int,
    val loopDuringRecording: Boolean,
    val playOnPhotoCapture: Boolean,
) {
    init {
        require(customVolumePercent in 0..100) {
            "Custom volume percentage must be between 0 and 100."
        }
    }
}