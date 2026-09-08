package com.civdevops.petcam.core.model.camera

import kotlin.test.Test
import kotlin.test.assertEquals

class CameraModelTest {

    @Test
    fun `capture mode contains photo and video`() {
        assertEquals(
            listOf(CaptureMode.PHOTO, CaptureMode.VIDEO),
            CaptureMode.entries,
        )
    }

    @Test
    fun `camera lens contains back and front`() {
        assertEquals(
            listOf(CameraLens.BACK, CameraLens.FRONT),
            CameraLens.entries,
        )
    }

    @Test
    fun `flash mode contains supported preferences`() {
        assertEquals(
            listOf(
                FlashMode.OFF,
                FlashMode.ON,
                FlashMode.AUTO,
            ),
            FlashMode.entries,
        )
    }

    @Test
    fun `video quality contains supported quality preferences`() {
        assertEquals(
            listOf(
                VideoQuality.UHD,
                VideoQuality.FHD,
                VideoQuality.HD,
                VideoQuality.SD,
            ),
            VideoQuality.entries,
        )
    }
}