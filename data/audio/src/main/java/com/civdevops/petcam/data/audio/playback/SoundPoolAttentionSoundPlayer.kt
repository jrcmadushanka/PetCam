package com.civdevops.petcam.data.audio.playback

import android.content.Context
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.civdevops.petcam.core.model.PetSoundId
import com.civdevops.petcam.core.model.audio.PetSoundGain
import com.civdevops.petcam.data.audio.catalog.BundledPetSoundCatalog
import com.civdevops.petcam.domain.audio.AttentionSoundFailure
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackResult
import com.civdevops.petcam.domain.audio.AttentionSoundPlaybackState
import com.civdevops.petcam.domain.audio.AttentionSoundPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
internal class SoundPoolAttentionSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: BundledPetSoundCatalog,
    private val assetResolver: SoundAssetResolver,
    private val audioFocusController: AudioFocusController
) : AttentionSoundPlayer {

    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(PetCamAudioAttributes.value)
        .build()

    private val _playbackState =
        MutableStateFlow<AttentionSoundPlaybackState>(AttentionSoundPlaybackState.Idle)

    private val loadedSamples = mutableMapOf<PetSoundId, Int>()
    private val pendingLoads = mutableMapOf<PetSoundId, CompletableDeferred<Int?>>()
    private val soundIdBySampleId = mutableMapOf<Int, PetSoundId>()
    private val durationBySoundId = mutableMapOf<PetSoundId, Long>()

    private var generation = 0L
    private var activeStreamId = 0
    private var activeSoundId: PetSoundId? = null
    private var activeLooping = false
    private var activeGain = 1f
    private var ducked = false

    private var oneShotRemainingMillis = 0L
    private var oneShotStartedAtMillis = 0L
    private var oneShotCompletion: Runnable? = null

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            synchronized(lock) {
                val soundId = soundIdBySampleId.remove(sampleId)
                    ?: return@setOnLoadCompleteListener

                val deferred = pendingLoads.remove(soundId)
                    ?: return@setOnLoadCompleteListener

                if (status == LOAD_SUCCESS) {
                    loadedSamples[soundId] = sampleId
                    deferred.complete(sampleId)
                } else {
                    deferred.complete(null)
                }
            }
        }

        preloadBundledSounds()
    }

    override fun observePlaybackState(): Flow<AttentionSoundPlaybackState> {
        return _playbackState.asStateFlow()
    }

    override suspend fun play(
        soundId: PetSoundId,
        gain: PetSoundGain,
        loop: Boolean
    ): AttentionSoundPlaybackResult {
        val token = synchronized(lock) {
            generation++
            stopActivePlaybackLocked(updateState = false)
            _playbackState.value = AttentionSoundPlaybackState.Loading(soundId)
            generation
        }

        val sampleId = resolveSample(soundId) ?: return failIfCurrent(
            token,
            AttentionSoundFailure.SOUND_UNAVAILABLE
        )

        return synchronized(lock) {
            if (token != generation) {
                return@synchronized AttentionSoundPlaybackResult.Cancelled
            }

            if (!audioFocusController.request(::onAudioFocusChanged)) {
                _playbackState.value =
                    AttentionSoundPlaybackState.Failed(AttentionSoundFailure.AUDIO_FOCUS_DENIED)

                return@synchronized AttentionSoundPlaybackResult.Failed(
                    AttentionSoundFailure.AUDIO_FOCUS_DENIED
                )
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
                _playbackState.value =
                    AttentionSoundPlaybackState.Failed(AttentionSoundFailure.PLAYBACK_FAILED)

                return@synchronized AttentionSoundPlaybackResult.Failed(
                    AttentionSoundFailure.PLAYBACK_FAILED
                )
            }

            activeStreamId = streamId
            activeSoundId = soundId
            activeLooping = loop
            activeGain = volume
            ducked = false

            if (!loop) {
                oneShotRemainingMillis =
                    durationBySoundId[soundId] ?: FALLBACK_DURATION_MS

                oneShotStartedAtMillis = SystemClock.elapsedRealtime()
                scheduleOneShotCompletionLocked(token)
            }

            _playbackState.value = AttentionSoundPlaybackState.Playing(soundId, loop)
            AttentionSoundPlaybackResult.Started
        }
    }

    override suspend fun pause() {
        synchronized(lock) {
            val state = _playbackState.value as? AttentionSoundPlaybackState.Playing
                ?: return

            if (activeStreamId == 0) return

            if (!activeLooping) {
                val elapsed =
                    (SystemClock.elapsedRealtime() - oneShotStartedAtMillis).coerceAtLeast(0L)

                oneShotRemainingMillis =
                    (oneShotRemainingMillis - elapsed).coerceAtLeast(1L)

                cancelOneShotCompletionLocked()
            }

            soundPool.pause(activeStreamId)
            audioFocusController.abandon()

            _playbackState.value =
                AttentionSoundPlaybackState.Paused(state.soundId, state.looping)
        }
    }

    override suspend fun resume() {
        synchronized(lock) {
            val state = _playbackState.value as? AttentionSoundPlaybackState.Paused
                ?: return

            if (activeStreamId == 0) return

            if (!audioFocusController.request(::onAudioFocusChanged)) {
                generation++
                stopActivePlaybackLocked(updateState = false)

                _playbackState.value =
                    AttentionSoundPlaybackState.Failed(AttentionSoundFailure.AUDIO_FOCUS_DENIED)

                return
            }

            soundPool.resume(activeStreamId)

            if (!activeLooping) {
                oneShotStartedAtMillis = SystemClock.elapsedRealtime()
                scheduleOneShotCompletionLocked(generation)
            }

            _playbackState.value =
                AttentionSoundPlaybackState.Playing(state.soundId, state.looping)
        }
    }

    override suspend fun stop() {
        synchronized(lock) {
            generation++
            stopActivePlaybackLocked(updateState = true)
        }
    }

    private fun preloadBundledSounds() {
        catalog.entries.forEach { entry ->
            durationBySoundId[entry.sound.id] = entry.durationMillis

            queueLoad(
                soundId = entry.sound.id,
                asset = SoundAsset.Bundled(entry.rawResourceId, entry.durationMillis)
            )
        }
    }

    private suspend fun resolveSample(soundId: PetSoundId): Int? {
        val existingSample = synchronized(lock) {
            loadedSamples[soundId]
        }

        if (existingSample != null) return existingSample

        val existingLoad = synchronized(lock) {
            pendingLoads[soundId]
        }

        if (existingLoad != null) return existingLoad.await()

        val asset = assetResolver.resolve(soundId) ?: return null

        val deferred = synchronized(lock) {
            loadedSamples[soundId]?.let { sampleId ->
                return sampleId
            }

            pendingLoads[soundId] ?: queueLoad(soundId, asset)
        }

        return deferred.await()
    }

    private fun queueLoad(
        soundId: PetSoundId,
        asset: SoundAsset
    ): CompletableDeferred<Int?> {
        pendingLoads[soundId]?.let { return it }

        val deferred = CompletableDeferred<Int?>()
        pendingLoads[soundId] = deferred
        durationBySoundId[soundId] = asset.durationMillis

        val sampleId = when (asset) {
            is SoundAsset.Bundled ->
                soundPool.load(context, asset.rawResourceId, LOAD_PRIORITY)

            is SoundAsset.Downloaded ->
                soundPool.load(asset.filePath, LOAD_PRIORITY)
        }

        if (sampleId == LOAD_FAILED_SAMPLE_ID) {
            pendingLoads.remove(soundId)
            deferred.complete(null)
        } else {
            soundIdBySampleId[sampleId] = soundId
        }

        return deferred
    }

    private fun onAudioFocusChanged(change: AudioFocusChange) {
        synchronized(lock) {
            when (change) {
                AudioFocusChange.GAIN -> restoreVolumeLocked()

                AudioFocusChange.LOSS,
                AudioFocusChange.LOSS_TRANSIENT -> {
                    generation++
                    stopActivePlaybackLocked(updateState = true)
                }

                AudioFocusChange.LOSS_TRANSIENT_CAN_DUCK -> duckLocked()
            }
        }
    }

    private fun duckLocked() {
        if (activeStreamId == 0 || ducked) return

        val volume = activeGain * DUCK_MULTIPLIER
        soundPool.setVolume(activeStreamId, volume, volume)
        ducked = true
    }

    private fun restoreVolumeLocked() {
        if (activeStreamId == 0 || !ducked) return

        soundPool.setVolume(activeStreamId, activeGain, activeGain)
        ducked = false
    }

    private fun scheduleOneShotCompletionLocked(token: Long) {
        cancelOneShotCompletionLocked()

        val completion = Runnable {
            synchronized(lock) {
                if (generation != token || activeLooping) {
                    return@synchronized
                }

                clearCompletedStreamLocked()
                _playbackState.value = AttentionSoundPlaybackState.Idle
            }
        }

        oneShotCompletion = completion
        handler.postDelayed(completion, oneShotRemainingMillis)
    }

    private fun cancelOneShotCompletionLocked() {
        oneShotCompletion?.let(handler::removeCallbacks)
        oneShotCompletion = null
    }

    private fun clearCompletedStreamLocked() {
        cancelOneShotCompletionLocked()

        activeStreamId = 0
        activeSoundId = null
        activeLooping = false
        activeGain = 1f
        ducked = false
        oneShotRemainingMillis = 0L
        oneShotStartedAtMillis = 0L

        audioFocusController.abandon()
    }

    private fun stopActivePlaybackLocked(updateState: Boolean) {
        cancelOneShotCompletionLocked()

        if (activeStreamId != 0) {
            soundPool.stop(activeStreamId)
        }

        activeStreamId = 0
        activeSoundId = null
        activeLooping = false
        activeGain = 1f
        ducked = false
        oneShotRemainingMillis = 0L
        oneShotStartedAtMillis = 0L

        audioFocusController.abandon()

        if (updateState) {
            _playbackState.value = AttentionSoundPlaybackState.Idle
        }
    }

    private fun failIfCurrent(
        token: Long,
        failure: AttentionSoundFailure
    ): AttentionSoundPlaybackResult {
        return synchronized(lock) {
            if (token != generation) {
                AttentionSoundPlaybackResult.Cancelled
            } else {
                _playbackState.value = AttentionSoundPlaybackState.Failed(failure)
                AttentionSoundPlaybackResult.Failed(failure)
            }
        }
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

        const val FALLBACK_DURATION_MS = 5_000L
    }
}