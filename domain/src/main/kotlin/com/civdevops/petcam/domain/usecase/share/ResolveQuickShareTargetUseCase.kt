package com.civdevops.petcam.domain.usecase.share

import com.civdevops.petcam.core.model.share.QuickShareTarget
import com.civdevops.petcam.domain.repository.ShareRepository

class ResolveQuickShareTargetUseCase(
    private val shareRepository: ShareRepository,
) {

    suspend operator fun invoke(
        preferredTarget: QuickShareTarget?,
    ): QuickShareTarget? {
        if (preferredTarget == null) {
            return null
        }

        val availableTargets =
            shareRepository.getAvailableQuickShareTargets()

        return preferredTarget.takeIf { target ->
            target in availableTargets
        }
    }
}