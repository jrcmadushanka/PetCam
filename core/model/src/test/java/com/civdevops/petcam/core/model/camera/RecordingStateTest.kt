package com.civdevops.petcam.core.model.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecordingStateTest {

    @Test
    fun `recording preserves elapsed time`() {
        val state = RecordingState.Recording(
            elapsedMillis = 5_000L,
        )

        assertEquals(5_000L, state.elapsedMillis)
    }

    @Test
    fun `paused preserves elapsed time`() {
        val state = RecordingState.Paused(
            elapsedMillis = 7_500L,
        )

        assertEquals(7_500L, state.elapsedMillis)
    }

    @Test
    fun `recording rejects negative elapsed time`() {
        assertFailsWith<IllegalArgumentException> {
            RecordingState.Recording(
                elapsedMillis = -1L,
            )
        }
    }

    @Test
    fun `paused rejects negative elapsed time`() {
        assertFailsWith<IllegalArgumentException> {
            RecordingState.Paused(
                elapsedMillis = -1L,
            )
        }
    }

    @Test
    fun `failed state preserves typed failure`() {
        val state = RecordingState.Failed(
            failure = RecordingFailure.FINALIZATION_FAILED,
        )

        assertEquals(
            RecordingFailure.FINALIZATION_FAILED,
            state.failure,
        )
    }
}