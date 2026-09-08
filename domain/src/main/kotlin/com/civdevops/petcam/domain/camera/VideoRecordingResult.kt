package com.civdevops.petcam.domain.camera

import com.civdevops.petcam.core.model.MediaId
import com.civdevops.petcam.core.model.camera.RecordingFailure

sealed interface VideoRecordingResult {

    data class Saved(
        val mediaId: MediaId,
    ) : VideoRecordingResult

    data class Failed(
        val failure: RecordingFailure,
    ) : VideoRecordingResult
}