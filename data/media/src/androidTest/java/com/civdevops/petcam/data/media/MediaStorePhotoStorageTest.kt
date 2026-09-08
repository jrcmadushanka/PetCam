package com.civdevops.petcam.data.media

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaStorePhotoStorageTest {

    @Test
    fun savesAndPublishesPhoto() = runTest {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = MediaStorePhotoStorage(context)
        val name = "PETCAM_TEST_${System.currentTimeMillis()}.jpg"

        val mediaId = storage.savePhoto(name) {
            it.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        }

        val uri = Uri.parse(mediaId.rawValue)

        try {
            context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.IS_PENDING
                ),
                null,
                null,
                null
            )!!.use { cursor ->
                assertTrue(cursor.moveToFirst())

                assertEquals(
                    name,
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                )

                assertEquals(
                    "image/jpeg",
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE))
                )

                assertEquals(
                    0,
                    cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.IS_PENDING))
                )
            }
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }
}