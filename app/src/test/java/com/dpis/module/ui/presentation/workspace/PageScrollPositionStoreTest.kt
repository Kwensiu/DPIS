package com.dpis.module.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

class PageScrollPositionStoreTest {
    @Test
    fun missingTopBarEntryIsUnknownNotExpanded() {
        val store = PageScrollPositionStore()
        assertEquals(null, store.storedTopBarCollapsed("about"))
    }

    @Test
    fun storedTopBarCollapsedRoundTrips() {
        val store = PageScrollPositionStore()
        store.updateTopBar("about", true)
        assertEquals(true, store.storedTopBarCollapsed("about"))
        store.updateTopBar("about", false)
        assertEquals(false, store.storedTopBarCollapsed("about"))
    }
}
