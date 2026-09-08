package com.civdevops.petcam.domain.media

sealed interface MediaDeleteResult {

    data object Deleted : MediaDeleteResult

    data object NotFound : MediaDeleteResult

    data object Failed : MediaDeleteResult
}