package com.civdevops.petcam.data.audio.playback

import com.civdevops.petcam.core.model.PetSoundId

internal interface SoundAssetResolver {
    suspend fun resolve(soundId: PetSoundId): SoundAsset?
}