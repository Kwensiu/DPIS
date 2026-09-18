package com.dpis.module.applist

import com.dpis.module.applist.presentation.AppListBatchActionCoordinator
import org.junit.Assert.assertEquals
import org.junit.Test

class AppListBatchActionCoordinatorTest {
    @Test
    fun scopeResultCountsAlreadyScopedAndDeniedPackagesAsSkipped() {
        val result = AppListBatchActionCoordinator.scopeResult(
            selectedPackages = listOf("already", "approved", "denied"),
            currentScope = setOf("already"),
            approvedPackages = listOf("approved"),
        )

        assertEquals(1, result.updatedCount)
        assertEquals(2, result.skippedCount)
    }
}
