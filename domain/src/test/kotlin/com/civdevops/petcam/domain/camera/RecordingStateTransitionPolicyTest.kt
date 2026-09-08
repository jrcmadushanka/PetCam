package com.civdevops.petcam.domain.camera

import com.civdevops.petcam.core.model.camera.RecordingFailure
import com.civdevops.petcam.core.model.camera.RecordingState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecordingStateTransitionPolicyTest {

    private val policy =
        RecordingStateTransitionPolicy()

    @Test
    fun `normal recording lifecycle transitions are allowed`() {
        val transitions = listOf(
            RecordingState.Idle to
                    RecordingState.Preparing,

            RecordingState.Preparing to
                    RecordingState.Recording(
                        elapsedMillis = 0L,
                    ),

            RecordingState.Recording(
                elapsedMillis = 1_000L,
            ) to RecordingState.Paused(
                elapsedMillis = 1_000L,
            ),

            RecordingState.Paused(
                elapsedMillis = 1_000L,
            ) to RecordingState.Recording(
                elapsedMillis = 1_500L,
            ),

            RecordingState.Recording(
                elapsedMillis = 2_000L,
            ) to RecordingState.Finalizing,

            RecordingState.Finalizing to
                    RecordingState.Idle,
        )

        transitions.forEach { (from, to) ->
            assertTrue(
                policy.isAllowed(
                    from = from,
                    to = to,
                ),
                "$from -> $to should be allowed",
            )
        }
    }

    @Test
    fun `invalid lifecycle jumps are rejected`() {
        val transitions = listOf(
            RecordingState.Idle to
                    RecordingState.Recording(
                        elapsedMillis = 0L,
                    ),

            RecordingState.Idle to
                    RecordingState.Finalizing,

            RecordingState.Finalizing to
                    RecordingState.Recording(
                        elapsedMillis = 0L,
                    ),

            RecordingState.Failed(
                failure = RecordingFailure.START_FAILED,
            ) to RecordingState.Recording(
                elapsedMillis = 0L,
            ),
        )

        transitions.forEach { (from, to) ->
            assertFalse(
                policy.isAllowed(
                    from = from,
                    to = to,
                ),
                "$from -> $to should be rejected",
            )
        }
    }

    @Test
    fun `recording progress may advance`() {
        assertTrue(
            policy.isAllowed(
                from = RecordingState.Recording(
                    elapsedMillis = 1_000L,
                ),
                to = RecordingState.Recording(
                    elapsedMillis = 2_000L,
                ),
            ),
        )
    }

    @Test
    fun `recording progress cannot move backwards`() {
        assertFalse(
            policy.isAllowed(
                from = RecordingState.Recording(
                    elapsedMillis = 5_000L,
                ),
                to = RecordingState.Recording(
                    elapsedMillis = 4_000L,
                ),
            ),
        )
    }

    @Test
    fun `paused recording cannot resume with earlier elapsed time`() {
        assertFalse(
            policy.isAllowed(
                from = RecordingState.Paused(
                    elapsedMillis = 5_000L,
                ),
                to = RecordingState.Recording(
                    elapsedMillis = 4_999L,
                ),
            ),
        )
    }

    @Test
    fun `active recording may fail`() {
        assertTrue(
            policy.isAllowed(
                from = RecordingState.Recording(
                    elapsedMillis = 1_000L,
                ),
                to = RecordingState.Failed(
                    failure =
                        RecordingFailure.FINALIZATION_FAILED,
                ),
            ),
        )
    }

    @Test
    fun `failed recording may recover to idle`() {
        assertTrue(
            policy.isAllowed(
                from = RecordingState.Failed(
                    failure =
                        RecordingFailure.START_FAILED,
                ),
                to = RecordingState.Idle,
            ),
        )
    }
}