package com.civdevops.petcam.domain.repository

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSound
import kotlinx.coroutines.flow.Flow

interface PetSoundRepository {

    fun observePetSounds(): Flow<List<PetSound>>

    suspend fun getPetSound(
        id: PetSoundId,
    ): PetSound?
}