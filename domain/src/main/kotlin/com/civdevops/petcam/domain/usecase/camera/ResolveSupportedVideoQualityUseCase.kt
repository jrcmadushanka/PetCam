package com.civdevops.petcam.domain.usecase.camera

import com.civdevops.petcam.core.model.camera.VideoQuality

class ResolveSupportedVideoQualityUseCase {

    operator fun invoke(
        preferredQuality: VideoQuality,
        supportedQualities: Set<VideoQuality>,
    ): VideoQuality? {
        if (preferredQuality in supportedQualities) {
            return preferredQuality
        }

        val preferredIndex =
            qualityOrder.indexOf(preferredQuality)

        val lowerQuality =
            qualityOrder
                .drop(preferredIndex + 1)
                .firstOrNull { quality ->
                    quality in supportedQualities
                }

        if (lowerQuality != null) {
            return lowerQuality
        }

        return qualityOrder
            .take(preferredIndex)
            .asReversed()
            .firstOrNull { quality ->
                quality in supportedQualities
            }
    }

    private companion object {
        val qualityOrder = listOf(
            VideoQuality.UHD,
            VideoQuality.FHD,
            VideoQuality.HD,
            VideoQuality.SD,
        )
    }
}