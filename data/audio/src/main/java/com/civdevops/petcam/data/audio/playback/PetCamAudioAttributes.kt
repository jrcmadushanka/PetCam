package com.civdevops.petcam.data.audio.playback

import android.media.AudioAttributes

internal object PetCamAudioAttributes {

    val value: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
}