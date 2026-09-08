package com.civdevops.petcam.core.model.camera

sealed interface RecordingState {

    data object Idle : RecordingState

    data object Preparing : RecordingState

    data class Recording(
        val elapsedMillis: Long,
    ) : RecordingState {
        init {
            require(elapsedMillis >= 0L) {
                "Recording elapsed time must not be negative."
            }
        }
    }

    data class Paused(
        val elapsedMillis: Long,
    ) : RecordingState {
        init {
            require(elapsedMillis >= 0L) {
                "Paused elapsed time must not be negative."
            }
        }
    }

    data object Finalizing : RecordingState

    data class Failed(
        val failure: RecordingFailure,
    ) : RecordingState
}