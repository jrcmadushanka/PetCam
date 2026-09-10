package com.civdevops.petcam.data.audio.playback

import android.content.Context
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSoundGain
import com.civdevops.petcam.data.audio.catalog.BundledPetSoundCatalog
import com.civdevops.petcam.domain.audio.AttentionSoundFailure
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackResult
import com.civdevops.petcam.domain.audio.AttentionSoundPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred

@Singleton
internal class SoundPoolAttentionSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: BundledPetSoundCatalog,
    private val audioFocusController: AudioFocusController
) : AttentionSoundPlayer {

    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(PetCamAudioAttributes.value)
        .build()

    private val loadResults = mutableMapOf<PetSoundId, CompletableDeferred<Int?>>()
    private val soundIdBySampleId = mutableMapOf<Int, PetSoundId>()

    private var activeStreamId = 0
    private var activeGain = 1f
    private var ducked = false

    private val abandonFocusRunnable = Runnable {
        synchronized(lock) {
            activeStreamId = 0
            ducked = false
            audioFocusController.abandon()
        }
    }

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            synchronized(lock) {
                val soundId = soundIdBySampleId[sampleId] ?: return@setOnLoadCompleteListener
                val result = loadResults[soundId] ?: return@setOnLoadCompleteListener

                if (!result.isCompleted) {
                    result.complete(sampleId.takeIf { status == LOAD_SUCCESS })
                }
            }
        }

        preloadSounds()
    }

    override suspend fun play(
        soundId: PetSoundId,
        gain: PetSoundGain,
        loop: Boolean
    ): AttentionSoundPlaybackResult {
        val entry = catalog.find(soundId)
            ?: return failed(AttentionSoundFailure.SOUND_UNAVAILABLE)

        val sampleId = loadResults[soundId]?.await()
            ?: return failed(AttentionSoundFailure.SOUND_UNAVAILABLE)

        return synchronized(lock) {
            stopPlaybackLocked()

            if (!audioFocusController.request(::onAudioFocusChanged)) {
                return@synchronized failed(AttentionSoundFailure.AUDIO_FOCUS_DENIED)
            }

            val volume = gain.percent / 100f
            val streamId = soundPool.play(
                sampleId,
                volume,
                volume,
                PLAYBACK_PRIORITY,
                if (loop) LOOP_FOREVER else NO_LOOP,
                NORMAL_RATE
            )

            if (streamId == PLAYBACK_FAILED_STREAM_ID) {
                audioFocusController.abandon()
                return@synchronized failed(AttentionSoundFailure.PLAYBACK_FAILED)
            }

            activeStreamId = streamId
            activeGain = volume
            ducked = false

            if (!loop) {
                handler.postDelayed(
                    abandonFocusRunnable,
                    entry.durationMillis + FOCUS_RELEASE_PADDING_MS
                )
            }

            AttentionSoundPlaybackResult.Started
        }
    }

    override suspend fun stop() {
        synchronized(lock) {
            stopPlaybackLocked()
        }
    }

    private fun preloadSounds() {
        catalog.entries.forEach { entry ->
            val deferred = CompletableDeferred<Int?>()
            loadResults[entry.sound.id] = deferred

            val sampleId = soundPool.load(context, entry.rawResourceId, LOAD_PRIORITY)

            if (sampleId == LOAD_FAILED_SAMPLE_ID) {
                deferred.complete(null)
            } else {
                soundIdBySampleId[sampleId] = entry.sound.id
            }
        }
    }

    private fun onAudioFocusChanged(change: AudioFocusChange) {
        synchronized(lock) {
            when (change) {
                AudioFocusChange.GAIN -> restoreVolumeLocked()
                AudioFocusChange.LOSS,
                AudioFocusChange.LOSS_TRANSIENT -> stopPlaybackLocked()
                AudioFocusChange.LOSS_TRANSIENT_CAN_DUCK -> duckLocked()
            }
        }
    }

    private fun duckLocked() {
        if (activeStreamId == 0 || ducked) return

        val duckVolume = activeGain * DUCK_MULTIPLIER
        soundPool.setVolume(activeStreamId, duckVolume, duckVolume)
        ducked = true
    }

    private fun restoreVolumeLocked() {
        if (activeStreamId == 0 || !ducked) return

        soundPool.setVolume(activeStreamId, activeGain, activeGain)
        ducked = false
    }

    private fun stopPlaybackLocked() {
        handler.removeCallbacks(abandonFocusRunnable)

        if (activeStreamId != 0) {
            soundPool.stop(activeStreamId)
        }

        activeStreamId = 0
        activeGain = 1f
        ducked = false
        audioFocusController.abandon()
    }

    private fun failed(
        failure: AttentionSoundFailure
    ): AttentionSoundPlaybackResult {
        return AttentionSoundPlaybackResult.Failed(failure)
    }

    private companion object {
        const val LOAD_SUCCESS = 0
        const val LOAD_PRIORITY = 1
        const val LOAD_FAILED_SAMPLE_ID = 0

        const val PLAYBACK_PRIORITY = 1
        const val PLAYBACK_FAILED_STREAM_ID = 0

        const val NO_LOOP = 0
        const val LOOP_FOREVER = -1

        const val NORMAL_RATE = 1f
        const val DUCK_MULTIPLIER = 0.2f

        const val FOCUS_RELEASE_PADDING_MS = 250L
    }
}