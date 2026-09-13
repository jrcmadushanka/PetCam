package com.civdevops.petcam.data.audio.playback

internal enum class AudioFocusChange {
    GAIN,
    LOSS,
    LOSS_TRANSIENT,
    LOSS_TRANSIENT_CAN_DUCK
}