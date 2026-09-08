package com.civdevops.petcam.domain.share

sealed interface ShareLaunchResult {

    data object Presented : ShareLaunchResult

    data class Failed(
        val failure: ShareFailure,
    ) : ShareLaunchResult
}