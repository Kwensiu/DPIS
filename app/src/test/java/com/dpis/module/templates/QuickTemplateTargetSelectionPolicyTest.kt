package com.dpis.module

import com.dpis.module.templates.QuickTemplateTargetSelectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickTemplateTargetSelectionPolicyTest {
    @Test
    fun defaultFiltersHideSystemAppsUnlessConfiguredAndShown() {
        assertFalse(matches(system = true, configured = false, showAll = false, showSystem = false))
        assertTrue(matches(system = true, configured = false, showAll = false, showSystem = true))
        assertTrue(matches(system = true, configured = true, showAll = false, showSystem = false, showConfigured = true))
        assertFalse(matches(system = true, configured = true, showAll = false, showSystem = false, showConfigured = false))
    }

    @Test
    fun userAppsStayVisibleUnlessUserChipAndConfiguredChipBothHideThem() {
        assertTrue(matches(system = false, configured = false, showAll = false, showUser = true))
        assertFalse(matches(system = false, configured = false, showAll = false, showUser = false, showConfigured = false))
        assertTrue(matches(system = false, configured = true, showAll = false, showUser = false, showConfigured = true))
        assertFalse(matches(system = false, configured = true, showAll = false, showUser = true, showConfigured = false))
    }

    @Test
    fun queryMatchesLabelOrPackageIgnoringCase() {
        assertTrue(
            QuickTemplateTargetSelectionPolicy.matches(
                label = "Maps",
                packageName = "com.example.maps",
                configured = false,
                systemApp = false,
                query = " MAP ",
                showAllApps = true,
                showSystemApps = false,
                showUserApps = false,
                showConfiguredApps = true,
            ),
        )
        assertTrue(
            QuickTemplateTargetSelectionPolicy.matches(
                label = "Maps",
                packageName = "com.example.maps",
                configured = false,
                systemApp = false,
                query = "EXAMPLE",
                showAllApps = true,
                showSystemApps = false,
                showUserApps = false,
                showConfiguredApps = true,
            ),
        )
        assertFalse(
            QuickTemplateTargetSelectionPolicy.matches(
                label = "Maps",
                packageName = "com.example.maps",
                configured = false,
                systemApp = false,
                query = "mail",
                showAllApps = true,
                showSystemApps = false,
                showUserApps = false,
                showConfiguredApps = true,
            ),
        )
    }

    @Test
    fun sortPutsSavedTargetsThenConfiguredThenName() {
        val saved = setOf("com.saved")
        val savedApp = target("Zebra", "com.saved", configured = false, updated = 1, installed = 1)
        val configured = target("Alpha", "com.configured", configured = true, updated = 1, installed = 1)
        val other = target("Beta", "com.other", configured = false, updated = 1, installed = 1)

        assertTrue(
            QuickTemplateTargetSelectionPolicy.compare(
                savedApp,
                configured,
                saved,
                QuickTemplateTargetSelectionPolicy.SORT_NAME,
                false,
            ) < 0,
        )
        assertTrue(
            QuickTemplateTargetSelectionPolicy.compare(
                configured,
                other,
                saved,
                QuickTemplateTargetSelectionPolicy.SORT_NAME,
                false,
            ) < 0,
        )
        val later = target("Gamma", "com.later", configured = false, updated = 20, installed = 2)
        val earlier = target("Gamma", "com.earlier", configured = false, updated = 10, installed = 1)
        assertTrue(
            QuickTemplateTargetSelectionPolicy.compare(
                later,
                earlier,
                emptySet(),
                QuickTemplateTargetSelectionPolicy.SORT_UPDATED,
                false,
            ) < 0,
        )
        assertTrue(
            QuickTemplateTargetSelectionPolicy.compare(
                later,
                earlier,
                emptySet(),
                QuickTemplateTargetSelectionPolicy.SORT_UPDATED,
                true,
            ) > 0,
        )
    }

    @Test
    fun retainInstalledAndUnsavedDraftUseSetEquality() {
        val selected = linkedSetOf("com.keep", "com.gone")
        QuickTemplateTargetSelectionPolicy.retainInstalled(selected, setOf("com.keep"))
        assertEquals(setOf("com.keep"), selected.toSet())
        assertFalse(
            QuickTemplateTargetSelectionPolicy.hasUnsavedChanges(
                setOf("com.keep"),
                setOf("com.keep"),
            ),
        )
        assertTrue(
            QuickTemplateTargetSelectionPolicy.hasUnsavedChanges(
                setOf("com.keep", "com.extra"),
                setOf("com.keep"),
            ),
        )
    }

    private fun matches(
        system: Boolean,
        configured: Boolean,
        showAll: Boolean,
        showSystem: Boolean = false,
        showUser: Boolean = true,
        showConfigured: Boolean = true,
    ) = QuickTemplateTargetSelectionPolicy.matches(
        label = "App",
        packageName = "com.example.app",
        configured = configured,
        systemApp = system,
        query = "",
        showAllApps = showAll,
        showSystemApps = showSystem,
        showUserApps = showUser,
        showConfiguredApps = showConfigured,
    )

    private fun target(
        label: String,
        packageName: String,
        configured: Boolean,
        updated: Long,
        installed: Long,
    ) = QuickTemplateTargetSelectionPolicy.SortableTarget(
        label = label,
        packageName = packageName,
        configured = configured,
        firstInstallTime = installed,
        lastUpdateTime = updated,
    )
}
