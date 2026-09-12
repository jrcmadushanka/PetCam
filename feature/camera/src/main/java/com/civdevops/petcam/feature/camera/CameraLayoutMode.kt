package com.civdevops.petcam.feature.camera

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

internal enum class CameraLayoutMode {
    COMPACT,
    EXPANDED
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun currentCameraLayoutMode(): CameraLayoutMode {
    val windowSizeClass = currentWindowAdaptiveInfo(
        supportLargeAndXLargeWidth = true
    ).windowSizeClass

    return cameraLayoutMode(windowSizeClass)
}

internal fun cameraLayoutMode(
    windowSizeClass: WindowSizeClass
): CameraLayoutMode {
    return if (
        windowSizeClass.isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
        )
    ) {
        CameraLayoutMode.EXPANDED
    } else {
        CameraLayoutMode.COMPACT
    }
}