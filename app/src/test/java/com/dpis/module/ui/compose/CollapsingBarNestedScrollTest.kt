package com.dpis.module.ui.compose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollapsingBarNestedScrollTest {
    @Test
    fun collapsingBarOwnsScrollWhileTheBarIsStillMoving() {
        assertTrue(collapsingBarOwnsNestedScroll(0f, firstVisibleItemIndex = 3, firstVisibleItemScrollOffset = 20))
        assertTrue(collapsingBarOwnsNestedScroll(0.5f, firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0))
    }

    @Test
    fun collapsedBarStillOwnsScrollAtTheTopOfTheList() {
        assertTrue(collapsingBarOwnsNestedScroll(1f, firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0))
    }

    @Test
    fun collapsedBarDoesNotOwnBodyScrollAwayFromTheTop() {
        assertFalse(collapsingBarOwnsNestedScroll(1f, firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 12))
        assertFalse(collapsingBarOwnsNestedScroll(1f, firstVisibleItemIndex = 2, firstVisibleItemScrollOffset = 0))
        assertFalse(collapsingBarOwnsNestedScroll(0.9995f, firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0))
    }
}
