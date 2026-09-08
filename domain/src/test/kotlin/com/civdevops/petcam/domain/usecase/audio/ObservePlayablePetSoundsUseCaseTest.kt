package com.civdevops.petcam.domain.usecase.audio

import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.SoundPackId
import com.civdevops.petcam.core.model.audio.PetSound
import com.civdevops.petcam.core.model.audio.PetSoundCategory
import com.civdevops.petcam.core.model.audio.PetSoundSource
import com.civdevops.petcam.domain.repository.PetSoundRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import kotlin.test.Test

class ObservePlayablePetSoundsUseCaseTest {

    @Test
    fun `remote sounds are excluded from playable sounds`() = runBlocking {
        val bundled = createSound(
            id = "bundled",
            source = PetSoundSource.Bundled,
        )

        val downloaded = createSound(
            id = "downloaded",
            source = PetSoundSource.Downloaded,
        )

        val remote = createSound(
            id = "remote",
            source = PetSoundSource.Remote,
        )

        val useCase = ObservePlayablePetSoundsUseCase(
            petSoundRepository = FakePetSoundRepository(
                sounds = listOf(
                    bundled,
                    downloaded,
                    remote,
                ),
            ),
        )

        val result = useCase().first()

        assertEquals(
            listOf(bundled, downloaded),
            result,
        )
    }

    private fun createSound(
        id: String,
        source: PetSoundSource,
    ) = PetSound(
        id = PetSoundId(id),
        packId = SoundPackId("starter"),
        category = PetSoundCategory("dogs"),
        name = id,
        source = source,
    )

    private class FakePetSoundRepository(
        private val sounds: List<PetSound>,
    ) : PetSoundRepository {

        override fun observePetSounds(): Flow<List<PetSound>> =
            flowOf(sounds)

        override suspend fun getPetSound(
            id: PetSoundId,
        ): PetSound? =
            sounds.firstOrNull { sound ->
                sound.id == id
            }
    }
}