package com.civdevops.petcam.core.model.audio

sealed interface PetSoundSource {

    data object Bundled : PetSoundSource

    data object Downloaded : PetSoundSource

    data object Remote : PetSoundSource
}