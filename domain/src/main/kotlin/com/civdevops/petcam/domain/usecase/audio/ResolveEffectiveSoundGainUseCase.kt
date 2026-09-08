package com.civdevops.petcam.domain.usecase.audio

import com.civdevops.petcam.core.model.audio.PetSoundGain
import com.civdevops.petcam.core.model.settings.AudioSettings
import com.civdevops.petcam.core.model.settings.PetSoundVolumeMode

class ResolveEffectiveSoundGainUseCase {

    operator fun invoke(settings: AudioSettings): PetSoundGain =
        when (settings.volumeMode) {
            PetSoundVolumeMode.FollowDevice ->
                PetSoundGain(100)

            PetSoundVolumeMode.MaximumAppOutput ->
                PetSoundGain(100)

            PetSoundVolumeMode.Custom ->
                PetSoundGain(settings.customVolumePercent)
        }
}