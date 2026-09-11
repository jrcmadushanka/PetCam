package com.civdevops.petcam.data.audio.playback

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.data.audio.catalog.BundledPetSoundCatalog
import javax.inject.Inject

internal class DefaultSoundAssetResolver @Inject constructor(
    private val bundledCatalog: BundledPetSoundCatalog
) : SoundAssetResolver {

    override suspend fun resolve(soundId: PetSoundId): SoundAsset? {
        val entry = bundledCatalog.find(soundId) ?: return null

        return SoundAsset.Bundled(
            rawResourceId = entry.rawResourceId,
            durationMillis = entry.durationMillis
        )
    }
}