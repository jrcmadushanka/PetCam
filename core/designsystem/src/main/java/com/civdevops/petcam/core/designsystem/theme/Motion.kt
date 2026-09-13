package com.civdevops.petcam.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

object PetCamMotion {
    const val durationShort = 120
    const val durationMedium = 240
    const val durationLong = 360

    val standardEasing: Easing = CubicBezierEasing(
        a = 0.2f,
        b = 0f,
        c = 0f,
        d = 1f
    )

    val emphasizedEasing: Easing = CubicBezierEasing(
        a = 0.2f,
        b = 0f,
        c = 0f,
        d = 1f
    )
}