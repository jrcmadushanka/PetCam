package com.civdevops.petcam.domain.repository

import com.civdevops.petcam.core.model.SoundPackId
import com.civdevops.petcam.core.model.audio.SoundPack
import kotlinx.coroutines.flow.Flow

interface SoundPackRepository {

    fun observeSoundPacks(): Flow<List<SoundPack>>

    suspend fun getSoundPack(
        id: SoundPackId,
    ): SoundPack?
}