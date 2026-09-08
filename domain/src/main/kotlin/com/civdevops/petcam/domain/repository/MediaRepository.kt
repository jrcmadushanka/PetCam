package com.civdevops.petcam.domain.repository

import com.civdevops.petcam.core.model.MediaId
import com.civdevops.petcam.core.model.media.MediaItem
import com.civdevops.petcam.domain.media.MediaDeleteResult
import kotlinx.coroutines.flow.Flow

interface MediaRepository {

    fun observeMedia(): Flow<List<MediaItem>>

    suspend fun getMedia(
        id: MediaId,
    ): MediaItem?

    suspend fun deleteMedia(
        id: MediaId,
    ): MediaDeleteResult
}