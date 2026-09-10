package com.civdevops.petcam.data.audio.playback

internal interface AudioFocusController {
    fun request(onChange: (AudioFocusChange) -> Unit): Boolean
    fun abandon()
}