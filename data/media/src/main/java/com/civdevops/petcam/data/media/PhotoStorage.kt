package com.civdevops.petcam.data.media

import com.civdevops.petcam.core.model.MediaId
import java.io.OutputStream

interface PhotoStorage {
    suspend fun savePhoto(displayName: String, write: suspend (OutputStream) -> Unit): MediaId
}

class PhotoStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)