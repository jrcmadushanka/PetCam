package com.civdevops.petcam.core.model.media

import com.civdevops.petcam.core.model.MediaId

sealed interface MediaItem {

    val id: MediaId

    val createdAtEpochMillis: Long

    val type: MediaType

    data class Photo(
        override val id: MediaId,
        override val createdAtEpochMillis: Long,
    ) : MediaItem {

        override val type: MediaType = MediaType.PHOTO

        init {
            require(createdAtEpochMillis >= 0L) {
                "Photo creation time must not be negative."
            }
        }
    }

    data class Video(
        override val id: MediaId,
        override val createdAtEpochMillis: Long,
        val durationMillis: Long,
    ) : MediaItem {

        override val type: MediaType = MediaType.VIDEO

        init {
            require(createdAtEpochMillis >= 0L) {
                "Video creation time must not be negative."
            }

            require(durationMillis >= 0L) {
                "Video duration must not be negative."
            }
        }
    }
}