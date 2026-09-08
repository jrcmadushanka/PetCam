package com.civdevops.petcam.domain.camera

import com.civdevops.petcam.core.model.camera.RecordingState

class RecordingStateTransitionPolicy {

    fun isAllowed(
        from: RecordingState,
        to: RecordingState,
    ): Boolean {
        if (!isStructuralTransitionAllowed(from, to)) {
            return false
        }

        return elapsedTimeDoesNotRegress(
            from = from,
            to = to,
        )
    }

    private fun isStructuralTransitionAllowed(
        from: RecordingState,
        to: RecordingState,
    ): Boolean =
        when (from) {
            RecordingState.Idle ->
                to is RecordingState.Preparing

            RecordingState.Preparing ->
                to is RecordingState.Recording ||
                        to is RecordingState.Finalizing ||
                        to is RecordingState.Failed

            is RecordingState.Recording ->
                to is RecordingState.Recording ||
                        to is RecordingState.Paused ||
                        to is RecordingState.Finalizing ||
                        to is RecordingState.Failed

            is RecordingState.Paused ->
                to is RecordingState.Paused ||
                        to is RecordingState.Recording ||
                        to is RecordingState.Finalizing ||
                        to is RecordingState.Failed

            RecordingState.Finalizing ->
                to is RecordingState.Idle ||
                        to is RecordingState.Failed

            is RecordingState.Failed ->
                to is RecordingState.Idle
        }

    private fun elapsedTimeDoesNotRegress(
        from: RecordingState,
        to: RecordingState,
    ): Boolean {
        val previousElapsedMillis =
            from.elapsedMillisOrNull()

        val nextElapsedMillis =
            to.elapsedMillisOrNull()

        if (
            previousElapsedMillis == null ||
            nextElapsedMillis == null
        ) {
            return true
        }

        return nextElapsedMillis >= previousElapsedMillis
    }

    private fun RecordingState.elapsedMillisOrNull(): Long? =
        when (this) {
            is RecordingState.Recording -> elapsedMillis
            is RecordingState.Paused -> elapsedMillis

            RecordingState.Idle,
            RecordingState.Preparing,
            RecordingState.Finalizing,
            is RecordingState.Failed,
                -> null
        }
}