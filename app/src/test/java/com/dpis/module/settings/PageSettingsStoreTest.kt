package com.dpis.module.settings

import com.dpis.module.FakePrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PageSettingsStoreTest {
    @Test
    fun predictiveBackDefaultsEnabledUntilExplicitlyDisabled() {
        assertTrue(PageSettingsStore.resolvePredictiveBackEnabled(null))
        assertTrue(PageSettingsStore.resolvePredictiveBackEnabled(true))
        assertFalse(PageSettingsStore.resolvePredictiveBackEnabled(false))
    }

    @Test
    fun homeActivationDetectionDefaultsEnabledUntilExplicitlyDisabled() {
        assertTrue(PageSettingsStore.resolveHomeActivationDetectionEnabled(null))
        assertTrue(PageSettingsStore.resolveHomeActivationDetectionEnabled(true))
        assertFalse(PageSettingsStore.resolveHomeActivationDetectionEnabled(false))
    }

    @Test
    fun predictiveBackReadsAndPersistsDedicatedPreference() {
        val prefs = FakePrefs()
        assertTrue(PageSettingsStore.isPredictiveBackEnabled(prefs))
        PageSettingsStore.setPredictiveBackEnabled(prefs, false)
        assertFalse(PageSettingsStore.isPredictiveBackEnabled(prefs))
        PageSettingsStore.setPredictiveBackEnabled(prefs, true)
        assertTrue(PageSettingsStore.isPredictiveBackEnabled(prefs))
    }

    @Test
    fun homeActivationDetectionReadsAndPersistsDedicatedPreference() {
        val prefs = FakePrefs()
        assertTrue(PageSettingsStore.isHomeActivationDetectionEnabled(prefs))
        PageSettingsStore.setHomeActivationDetectionEnabled(prefs, false)
        assertFalse(PageSettingsStore.isHomeActivationDetectionEnabled(prefs))
        PageSettingsStore.setHomeActivationDetectionEnabled(prefs, true)
        assertTrue(PageSettingsStore.isHomeActivationDetectionEnabled(prefs))
    }

    @Test
    fun homeEditButtonDefaultsVisibleUntilExplicitlyHidden() {
        val prefs = FakePrefs()
        assertTrue(PageSettingsStore.isHomeEditButtonVisible(prefs))
        PageSettingsStore.setHomeEditButtonVisible(prefs, false)
        assertFalse(PageSettingsStore.isHomeEditButtonVisible(prefs))
    }

    @Test
    fun defaultStartupPageFallsBackToHomeForMissingOrInvalidValues() {
        val prefs = FakePrefs()
        assertEquals(PageSettingsStore.HOME, PageSettingsStore.getDefaultStartupPage(prefs))
        prefs.edit().putString("default_startup_page", "unknown").apply()
        assertEquals(PageSettingsStore.HOME, PageSettingsStore.getDefaultStartupPage(prefs))
        prefs.edit().putString("default_startup_page", "app").apply()
        assertEquals("APP", PageSettingsStore.getDefaultStartupPage(prefs))
    }

    @Test
    fun defaultStartupPageRejectsUnknownPages() {
        assertThrows(IllegalArgumentException::class.java) {
            PageSettingsStore.setDefaultStartupPage(FakePrefs(), "UNKNOWN")
        }
        val prefs = FakePrefs()
        PageSettingsStore.setDefaultStartupPage(prefs, "TOOLS")
        assertEquals("TOOLS", PageSettingsStore.getDefaultStartupPage(prefs))
    }

    @Test
    fun workspaceOrderDropsInvalidEntriesAndFillsMissingPages() {
        val prefs = FakePrefs()
        assertEquals(
            listOf("APP", "TEMPLATE", PageSettingsStore.HOME, "TOOLS", "SETTINGS"),
            PageSettingsStore.getWorkspaceOrder(prefs),
        )
        PageSettingsStore.setWorkspaceOrder(prefs, listOf("TOOLS", "APP", "TOOLS", "bogus"))
        assertEquals(
            listOf("TOOLS", "APP", "TEMPLATE", PageSettingsStore.HOME, "SETTINGS"),
            PageSettingsStore.getWorkspaceOrder(prefs),
        )
    }

    @Test
    fun hiddenWorkspacesIgnoreSettingsAndUnknownPages() {
        val prefs = FakePrefs()
        assertTrue(PageSettingsStore.getHiddenWorkspaces(prefs).isEmpty())
        PageSettingsStore.setWorkspaceVisible(prefs, "SETTINGS", false)
        assertTrue(PageSettingsStore.getHiddenWorkspaces(prefs).isEmpty())
        PageSettingsStore.setWorkspaceVisible(prefs, "APP", false)
        assertEquals(setOf("APP"), PageSettingsStore.getHiddenWorkspaces(prefs))
        PageSettingsStore.setWorkspaceVisible(prefs, "APP", true)
        assertTrue(PageSettingsStore.getHiddenWorkspaces(prefs).isEmpty())
        prefs.edit().putStringSet("workspace_hidden", setOf("APP", "bogus")).apply()
        assertEquals(setOf("APP"), PageSettingsStore.getHiddenWorkspaces(prefs))
    }
}
