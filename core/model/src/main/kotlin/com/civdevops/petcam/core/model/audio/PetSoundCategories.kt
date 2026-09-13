package com.civdevops.petcam.core.model.audio

object PetSoundCategories {
    val Dogs = PetSoundCategory("dogs")
    val Cats = PetSoundCategory("cats")
    val Whistles = PetSoundCategory("whistles")
    val Toys = PetSoundCategory("toys")
    val Other = PetSoundCategory("other")

    val values = listOf(Dogs, Cats, Whistles, Toys, Other)
}