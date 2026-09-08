package com.civdevops.petcam.data.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.civdevops.petcam.core.model.MediaId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStorePhotoStorage @Inject constructor(
    @ApplicationContext context: Context
) : PhotoStorage {

    private val resolver = context.contentResolver

    override suspend fun savePhoto(
        displayName: String,
        write: suspend (java.io.OutputStream) -> Unit
    ): MediaId = withContext(Dispatchers.IO) {
        val uri = createMediaUri(displayName)

        try {
            val outputStream = resolver.openOutputStream(uri, "w")
                ?: throw PhotoStorageException("Unable to open MediaStore output stream.")

            outputStream.use { write(it) }

            publish(uri)

            MediaId(uri.toString())
        } catch (cancellation: CancellationException) {
            deleteSafely(uri)
            throw cancellation
        } catch (error: Throwable) {
            deleteSafely(uri)
            throw error
        }
    }

    private fun createMediaUri(displayName: String): Uri {
        val values = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            createScopedStorageValues(displayName)
        } else {
            createLegacyValues(displayName)
        }

        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw PhotoStorageException("MediaStore failed to create an image entry.")
    }

    private fun createScopedStorageValues(displayName: String) = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, JPEG_MIME_TYPE)
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PetCam")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    private fun createLegacyValues(displayName: String): ContentValues {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "PetCam"
        )

        if (!directory.exists() && !directory.mkdirs()) {
            throw PhotoStorageException("Unable to create the Pet Cam pictures directory.")
        }

        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, JPEG_MIME_TYPE)
            put(MediaStore.Images.Media.DATA, File(directory, displayName).absolutePath)
        }
    }

    private fun publish(uri: Uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val updated = resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
            null,
            null
        )

        if (updated <= 0) {
            throw PhotoStorageException("MediaStore failed to publish the captured photo.")
        }
    }

    private fun deleteSafely(uri: Uri) {
        try {
            resolver.delete(uri, null, null)
        } catch (_: Exception) {
            // Best-effort cleanup only.
        }
    }

    private companion object {
        const val JPEG_MIME_TYPE = "image/jpeg"
    }
}