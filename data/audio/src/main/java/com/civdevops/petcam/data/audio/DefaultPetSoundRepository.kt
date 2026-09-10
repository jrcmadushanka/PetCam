package com.civdevops.petcam.data.audio

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSound
import com.civdevops.petcam.data.audio.catalog.BundledPetSoundCatalog
import com.civdevops.petcam.domain.repository.PetSoundRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class DefaultPetSoundRepository @Inject constructor(
    private val catalog: BundledPetSoundCatalog
) : PetSoundRepository {

    private val sounds by lazy {
        catalog.entries.map { it.sound }
    }

    override fun observePetSounds(): Flow<List<PetSound>> = flowOf(sounds)

    override suspend fun getPetSound(id: PetSoundId): PetSound? {
        return catalog.find(id)?.sound
    }
}