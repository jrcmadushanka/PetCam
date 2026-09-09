package com.civdevops.petcam.data.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.civdevops.petcam.core.model.MediaId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class MediaStoreVideoStorage @Inject constructor(
    @ApplicationContext context: Context
) : VideoStorage {

    private val resolver = context.contentResolver

    override fun createDestination(displayName: String): MediaStoreVideoDestination {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, VIDEO_MIME_TYPE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/PetCam")
            }
        }

        return MediaStoreVideoDestination(
            contentResolver = resolver,
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues = values
        )
    }

    override fun delete(mediaId: MediaId) {
        try {
            resolver.delete(mediaId.rawValue.toUri(), null, null)
        } catch (_: Exception) {
            // Best-effort cleanup.
        }
    }

    private companion object {
        const val VIDEO_MIME_TYPE = "video/mp4"
    }
}