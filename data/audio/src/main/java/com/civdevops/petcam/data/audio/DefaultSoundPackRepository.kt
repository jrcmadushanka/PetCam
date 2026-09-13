package com.civdevops.petcam.data.audio

import com.civdevops.petcam.core.model.SoundPackId
import com.civdevops.petcam.core.model.audio.SoundPack
import com.civdevops.petcam.data.audio.catalog.BundledPetSoundCatalog
import com.civdevops.petcam.domain.repository.SoundPackRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class DefaultSoundPackRepository @Inject constructor(
    private val catalog: BundledPetSoundCatalog
) : SoundPackRepository {

    override fun observeSoundPacks(): Flow<List<SoundPack>> {
        return flowOf(listOf(catalog.pack))
    }

    override suspend fun getSoundPack(id: SoundPackId): SoundPack? {
        return catalog.pack.takeIf { it.id == id }
    }
}