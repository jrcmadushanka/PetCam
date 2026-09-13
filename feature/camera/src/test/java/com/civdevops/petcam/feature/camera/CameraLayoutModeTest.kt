package com.civdevops.petcam.feature.camera

import androidx.window.core.layout.WindowSizeClass
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraLayoutModeTest {

    @Test
    fun widthBelowExpandedBreakpointUsesCompactLayout() {
        val windowSizeClass = WindowSizeClass(
            minWidthDp =
                WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND - 1,
            minHeightDp = 600
        )

        assertEquals(
            CameraLayoutMode.COMPACT,
            cameraLayoutMode(windowSizeClass)
        )
    }

    @Test
    fun expandedBreakpointUsesExpandedLayout() {
        val windowSizeClass = WindowSizeClass(
            minWidthDp =
                WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND,
            minHeightDp = 600
        )

        assertEquals(
            CameraLayoutMode.EXPANDED,
            cameraLayoutMode(windowSizeClass)
        )
    }

    @Test
    fun widthAboveExpandedBreakpointUsesExpandedLayout() {
        val windowSizeClass = WindowSizeClass(
            minWidthDp =
                WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND + 400,
            minHeightDp = 800
        )

        assertEquals(
            CameraLayoutMode.EXPANDED,
            cameraLayoutMode(windowSizeClass)
        )
    }
}