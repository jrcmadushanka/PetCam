package com.civdevops.petcam.domain.camera

import com.civdevops.petcam.core.model.camera.RecordingFailure

sealed interface RecordingCommandResult {

    data object Success : RecordingCommandResult

    data class Failed(
        val failure: RecordingFailure,
    ) : RecordingCommandResult
}