package com.civdevops.petcam.data.camera

import androidx.camera.video.Quality
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.civdevops.petcam.core.model.camera.VideoQuality
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraXVideoQualityMapperTest {

    @Test
    fun cameraQualitiesMapToDomainQualities() {
        val result = CameraXVideoQualityMapper.toDomain(
            listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
        )

        assertEquals(
            setOf(VideoQuality.UHD, VideoQuality.FHD, VideoQuality.HD, VideoQuality.SD),
            result
        )
    }

    @Test
    fun domainQualitiesMapToCameraXQualities() {
        assertEquals(Quality.UHD, CameraXVideoQualityMapper.toCameraX(VideoQuality.UHD))
        assertEquals(Quality.FHD, CameraXVideoQualityMapper.toCameraX(VideoQuality.FHD))
        assertEquals(Quality.HD, CameraXVideoQualityMapper.toCameraX(VideoQuality.HD))
        assertEquals(Quality.SD, CameraXVideoQualityMapper.toCameraX(VideoQuality.SD))
    }
}