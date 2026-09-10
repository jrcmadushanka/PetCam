package com.civdevops.petcam.data.audio.playback

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidAudioFocusController @Inject constructor(
    @ApplicationContext context: Context
) : AudioFocusController {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    private var focusListener: AudioManager.OnAudioFocusChangeListener? = null
    private var focusRequest: AudioFocusRequest? = null

    override fun request(onChange: (AudioFocusChange) -> Unit): Boolean {
        abandon()

        val listener = AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> onChange(AudioFocusChange.GAIN)
                AudioManager.AUDIOFOCUS_LOSS -> onChange(AudioFocusChange.LOSS)
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                    onChange(AudioFocusChange.LOSS_TRANSIENT)
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                    onChange(AudioFocusChange.LOSS_TRANSIENT_CAN_DUCK)
            }
        }

        focusListener = listener

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestModernFocus(listener)
        } else {
            requestLegacyFocus(listener)
        }

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            focusListener = null
            focusRequest = null
            return false
        }

        return true
    }

    override fun abandon() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let(audioManager::abandonAudioFocusRequest)
        } else {
            focusListener?.let(::abandonLegacyFocus)
        }

        focusRequest = null
        focusListener = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestModernFocus(
        listener: AudioManager.OnAudioFocusChangeListener
    ): Int {
        val request = AudioFocusRequest.Builder(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )
            .setAudioAttributes(PetCamAudioAttributes.value)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener(listener)
            .build()

        focusRequest = request

        return audioManager.requestAudioFocus(request)
    }

    @Suppress("DEPRECATION")
    private fun requestLegacyFocus(
        listener: AudioManager.OnAudioFocusChangeListener
    ): Int {
        return audioManager.requestAudioFocus(
            listener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )
    }

    @Suppress("DEPRECATION")
    private fun abandonLegacyFocus(
        listener: AudioManager.OnAudioFocusChangeListener
    ) {
        audioManager.abandonAudioFocus(listener)
    }
}