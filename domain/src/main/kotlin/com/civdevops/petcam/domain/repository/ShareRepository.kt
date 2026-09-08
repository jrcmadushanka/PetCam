package com.civdevops.petcam.domain.repository

import com.civdevops.petcam.core.model.MediaId
import com.civdevops.petcam.core.model.share.QuickShareTarget
import com.civdevops.petcam.domain.share.ShareLaunchResult

interface ShareRepository {

    suspend fun getAvailableQuickShareTargets(): Set<QuickShareTarget>

    suspend fun shareMedia(
        mediaId: MediaId,
        preferredTarget: QuickShareTarget?,
    ): ShareLaunchResult
}