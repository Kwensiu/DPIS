package com.dpis.module.runtime.resources

import com.dpis.module.viewport.ViewportOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ResourcesImplResultSelectorTest {
    @Test
    fun windowObservationWinsOverPublishedDisplayCandidates() {
        val window = result(360, 640, 360, 480)
        val published = result(540, 1188, 540, 320)
        var fallbackCalled = false

        val selection = ResourcesImplResultSelector.select(
            windowLikeBorrow = window,
            appProcessWindowMetrics = null,
            publishedRecord = published,
            trustedDisplay = result(540, 1188, 540, 320),
        ) {
            fallbackCalled = true
            result(1, 1, 1, 1)
        }

        assertSame(window, selection.result)
        assertEquals(
            ResourcesImplResultSelector.Source.WINDOW_LIKE_BORROW,
            selection.source,
        )
        assertEquals(false, fallbackCalled)
    }

    @Test
    fun selectorUsesCandidatesInOwnershipPrecedenceOrder() {
        val appProcess = result(400, 700, 400, 400)
        val published = result(540, 1188, 540, 320)
        val trusted = result(500, 1000, 500, 400)

        val selection = ResourcesImplResultSelector.select(
            windowLikeBorrow = null,
            appProcessWindowMetrics = appProcess,
            publishedRecord = published,
            trustedDisplay = trusted,
        ) { error("fallback must not run") }

        assertSame(appProcess, selection.result)
        assertEquals(
            ResourcesImplResultSelector.Source.APP_PROCESS_WINDOW_METRICS,
            selection.source,
        )
    }

    @Test
    fun publishedRecordWinsOverTrustedDisplayFallback() {
        val published = result(540, 1188, 540, 320)
        val trusted = result(500, 1000, 500, 400)

        val selection = ResourcesImplResultSelector.select(
            windowLikeBorrow = null,
            appProcessWindowMetrics = null,
            publishedRecord = published,
            trustedDisplay = trusted,
        ) { error("fallback must not run") }

        assertSame(published, selection.result)
        assertEquals(
            ResourcesImplResultSelector.Source.PUBLISHED_RECORD,
            selection.source,
        )
    }

    @Test
    fun selectorDerivesOnlyWhenNoCandidateOwnsTheCallback() {
        val derived = result(432, 950, 432, 400)

        val selection = ResourcesImplResultSelector.select(
            windowLikeBorrow = null,
            appProcessWindowMetrics = null,
            publishedRecord = null,
            trustedDisplay = null,
        ) { derived }

        assertSame(derived, selection.result)
        assertEquals(ResourcesImplResultSelector.Source.DERIVED, selection.source)
    }

    private fun result(
        widthDp: Int,
        heightDp: Int,
        smallestWidthDp: Int,
        densityDpi: Int,
    ) = ViewportOverride.Result(widthDp, heightDp, smallestWidthDp, densityDpi)
}
