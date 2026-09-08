package com.civdevops.petcam.domain.camera

import com.civdevops.petcam.core.model.MediaId

sealed interface PhotoCaptureResult {

    data class Saved(
        val mediaId: MediaId,
    ) : PhotoCaptureResult

    data class Failed(
        val failure: PhotoCaptureFailure,
    ) : PhotoCaptureResult
}