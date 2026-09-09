package com.civdevops.petcam.data.media

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import com.civdevops.petcam.core.model.MediaId

data class MediaStoreVideoDestination(
    val contentResolver: ContentResolver,
    val collectionUri: Uri,
    val contentValues: ContentValues
)

interface VideoStorage {
    fun createDestination(displayName: String): MediaStoreVideoDestination
    fun delete(mediaId: MediaId)
}