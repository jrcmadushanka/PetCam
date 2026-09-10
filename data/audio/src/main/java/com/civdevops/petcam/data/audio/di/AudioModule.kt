package com.civdevops.petcam.data.audio.di

import com.civdevops.petcam.data.audio.DefaultPetSoundRepository
import com.civdevops.petcam.data.audio.DefaultSoundPackRepository
import com.civdevops.petcam.data.audio.catalog.BundledPetSoundCatalog
import com.civdevops.petcam.data.audio.catalog.StarterBundledPetSoundCatalog
import com.civdevops.petcam.data.audio.playback.AndroidAudioFocusController
import com.civdevops.petcam.data.audio.playback.AudioFocusController
import com.civdevops.petcam.data.audio.playback.SoundPoolAttentionSoundPlayer
import com.civdevops.petcam.domain.audio.AttentionSoundPlayer
import com.civdevops.petcam.domain.repository.PetSoundRepository
import com.civdevops.petcam.domain.repository.SoundPackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindPetSoundRepository(
        implementation: DefaultPetSoundRepository
    ): PetSoundRepository

    @Binds
    @Singleton
    abstract fun bindSoundPackRepository(
        implementation: DefaultSoundPackRepository
    ): SoundPackRepository

    @Binds
    @Singleton
    abstract fun bindAttentionSoundPlayer(
        implementation: SoundPoolAttentionSoundPlayer
    ): AttentionSoundPlayer

    @Binds
    @Singleton
    abstract fun bindBundledPetSoundCatalog(
        implementation: StarterBundledPetSoundCatalog
    ): BundledPetSoundCatalog

    @Binds
    @Singleton
    abstract fun bindAudioFocusController(
        implementation: AndroidAudioFocusController
    ): AudioFocusController
}