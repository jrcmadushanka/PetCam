package com.civdevops.petcam.data.audio.catalog

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BundledAudioDurationReader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun readMillis(rawResourceId: Int): Long {
        val uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath(rawResourceId.toString())
            .build()

        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(1L)
                ?: FALLBACK_DURATION_MS
        } catch (_: Exception) {
            FALLBACK_DURATION_MS
        } finally {
            retriever.release()
        }
    }

    private companion object {
        const val FALLBACK_DURATION_MS = 5_000L
    }
}