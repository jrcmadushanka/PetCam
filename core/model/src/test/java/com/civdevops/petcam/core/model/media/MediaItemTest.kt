package com.civdevops.petcam.core.model.media

import com.civdevops.petcam.core.model.MediaId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MediaItemTest {

    @Test
    fun `photo exposes photo media type`() {
        val photo = MediaItem.Photo(
            id = MediaId("photo-1"),
            createdAtEpochMillis = 1_000L,
        )

        assertEquals(MediaType.PHOTO, photo.type)
    }

    @Test
    fun `video exposes video media type`() {
        val video = MediaItem.Video(
            id = MediaId("video-1"),
            createdAtEpochMillis = 1_000L,
            durationMillis = 5_000L,
        )

        assertEquals(MediaType.VIDEO, video.type)
    }

    @Test
    fun `video preserves duration`() {
        val video = MediaItem.Video(
            id = MediaId("video-1"),
            createdAtEpochMillis = 1_000L,
            durationMillis = 5_000L,
        )

        assertEquals(5_000L, video.durationMillis)
    }

    @Test
    fun `photo rejects negative creation time`() {
        assertFailsWith<IllegalArgumentException> {
            MediaItem.Photo(
                id = MediaId("photo-1"),
                createdAtEpochMillis = -1L,
            )
        }
    }

    @Test
    fun `video rejects negative creation time`() {
        assertFailsWith<IllegalArgumentException> {
            MediaItem .Video(
                id = MediaId("video-1"),
                createdAtEpochMillis = -1L,
                durationMillis = 5_000L,
            )
        }
    }

    @Test
    fun `video rejects negative duration`() {
        assertFailsWith<IllegalArgumentException> {
            MediaItem.Video(
                id = MediaId("video-1"),
                createdAtEpochMillis = 1_000L,
                durationMillis = -1L,
            )
        }
    }
}