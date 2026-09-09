package com.civdevops.petcam.data.camera

import androidx.camera.video.Quality
import com.civdevops.petcam.core.model.camera.VideoQuality

internal object CameraXVideoQualityMapper {

    fun toDomain(qualities: List<Quality>): Set<VideoQuality> {
        return qualities.mapNotNull { quality ->
            when (quality) {
                Quality.UHD -> VideoQuality.UHD
                Quality.FHD -> VideoQuality.FHD
                Quality.HD -> VideoQuality.HD
                Quality.SD -> VideoQuality.SD
                else -> null
            }
        }.toSet()
    }

    fun toCameraX(quality: VideoQuality): Quality {
        return when (quality) {
            VideoQuality.UHD -> Quality.UHD
            VideoQuality.FHD -> Quality.FHD
            VideoQuality.HD -> Quality.HD
            VideoQuality.SD -> Quality.SD
        }
    }
}