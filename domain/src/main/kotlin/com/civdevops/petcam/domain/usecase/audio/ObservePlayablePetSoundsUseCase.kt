package com.civdevops.petcam.domain.usecase.audio

import com.civdevops.petcam.core.model.audio.PetSound
import com.civdevops.petcam.core.model.audio.PetSoundSource
import com.civdevops.petcam.domain.repository.PetSoundRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ObservePlayablePetSoundsUseCase(
    private val petSoundRepository: PetSoundRepository,
) {

    operator fun invoke(): Flow<List<PetSound>> =
        petSoundRepository
            .observePetSounds()
            .map { sounds ->
                sounds.filter { sound ->
                    sound.source != PetSoundSource.Remote
                }
            }
            .distinctUntilChanged()
}