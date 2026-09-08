package com.civdevops.petcam.domain.usecase.share

import com.civdevops.petcam.core.model.MediaId
import com.civdevops.petcam.core.model.share.QuickShareTarget
import com.civdevops.petcam.domain.repository.ShareRepository
import com.civdevops.petcam.domain.share.ShareLaunchResult
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolveQuickShareTargetUseCaseTest {

    @Test
    fun `preferred target is returned when available`() = runBlocking {
        val useCase = ResolveQuickShareTargetUseCase(
            shareRepository = FakeShareRepository(
                availableTargets = setOf(
                    QuickShareTarget.TIKTOK,
                    QuickShareTarget.INSTAGRAM,
                ),
            ),
        )

        val result = useCase(
            preferredTarget = QuickShareTarget.TIKTOK,
        )

        assertEquals(
            QuickShareTarget.TIKTOK,
            result,
        )
    }

    @Test
    fun `null is returned when preferred target is unavailable`() =
        runBlocking {
            val useCase = ResolveQuickShareTargetUseCase(
                shareRepository = FakeShareRepository(
                    availableTargets = setOf(
                        QuickShareTarget.INSTAGRAM,
                    ),
                ),
            )

            val result = useCase(
                preferredTarget = QuickShareTarget.TIKTOK,
            )

            assertNull(result)
        }

    @Test
    fun `null is returned when there is no preferred target`() =
        runBlocking {
            val useCase = ResolveQuickShareTargetUseCase(
                shareRepository = FakeShareRepository(
                    availableTargets = setOf(
                        QuickShareTarget.TIKTOK,
                    ),
                ),
            )

            assertNull(
                useCase(
                    preferredTarget = null,
                ),
            )
        }

    private class FakeShareRepository(
        private val availableTargets: Set<QuickShareTarget>,
    ) : ShareRepository {

        override suspend fun getAvailableQuickShareTargets():
                Set<QuickShareTarget> =
            availableTargets

        override suspend fun shareMedia(
            mediaId: MediaId,
            preferredTarget: QuickShareTarget?,
        ): ShareLaunchResult =
            ShareLaunchResult.Presented
    }
}