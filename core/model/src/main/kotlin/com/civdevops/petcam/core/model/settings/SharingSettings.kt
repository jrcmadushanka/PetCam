package com.civdevops.petcam.core.model.settings

import com.civdevops.petcam.core.model.share.QuickShareTarget

data class SharingSettings(
    val autoOpenShareAfterCapture: Boolean,
    val preferredQuickShareTarget: QuickShareTarget?,
)