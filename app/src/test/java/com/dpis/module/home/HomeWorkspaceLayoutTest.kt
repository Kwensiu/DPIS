package com.dpis.module.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeWorkspaceLayoutTest {
    @Test
    fun hidingOneSectionLeavesOtherSectionsVisible() {
        val layout = HomeWorkspaceLayout.defaults()
            .withVisibility(HomeWorkspaceLayout.Item.FEEDBACK, false)

        assertFalse(layout.isVisible(HomeWorkspaceLayout.Item.FEEDBACK))
        assertTrue(layout.isVisible(HomeWorkspaceLayout.Item.CONFIGURED_APPS))
        assertTrue(layout.isVisible(HomeWorkspaceLayout.Item.BASIC_INFO))
    }

    @Test
    fun restoringVisibilityRemovesTheSectionFromTheHiddenSet() {
        val layout = HomeWorkspaceLayout.defaults()
            .withVisibility(HomeWorkspaceLayout.Item.DONATE, false)
            .withVisibility(HomeWorkspaceLayout.Item.DONATE, true)

        assertTrue(layout.isVisible(HomeWorkspaceLayout.Item.DONATE))
        assertTrue(layout.hiddenItems.isEmpty())
    }

    @Test
    fun unknownPersistedItemsAreIgnored() {
        val parsed = parseHiddenItems(
            setOf("FEEDBACK", "future_item", "DONATE"),
        )

        assertTrue(HomeWorkspaceLayout.Item.FEEDBACK in parsed)
        assertTrue(HomeWorkspaceLayout.Item.DONATE in parsed)
        assertTrue(parsed.size == 2)
    }

    @Test
    fun nullPersistedItemsRestoreDefaults() {
        assertTrue(parseHiddenItems(null).isEmpty())
    }
}
