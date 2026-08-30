package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyUiSourceSmokeTest {
    @Test
    fun scopeUnavailableActionPromptsManualLsposedSelection() {
        val source = read("src/main/java/com/dpis/module/settings/SystemScopeCoordinator.kt")
        val unavailableBlock = source.substringBefore("ScopeRequestGate.shared()")

        assertFalse(unavailableBlock.contains("openLsposedManager"))
        assertFalse(unavailableBlock.contains("scope_manual_manage_required"))
        assertFalse(unavailableBlock.contains("scope_manual_open_failed"))
        assertFalse(unavailableBlock.contains("R.string.status_save_requires_init"))
    }

    @Test
    fun unknownScopeHidesInjectionStatusAndDisablesScopeAction() {
        val source = read("src/main/java/com/dpis/module/applist/AppStatusFormatter.java")
        val dialogBinder = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java")
        val strings = read("src/main/res/values-zh-rCN/strings.xml")

        assertTrue(source.contains("scopeKnown"))
        assertFalse(source.contains("labels.scopeUnknown"))
        assertTrue(dialogBinder.contains("scopeButton.setEnabled(scopeKnown);"))
        assertTrue(dialogBinder.contains("scopeButton.setAlpha(scopeKnown ? 1f : 0.6f);"))
        assertFalse(strings.contains("<string name=\"app_status_scope_unknown\">"))
        assertFalse(strings.contains("<string name=\"scope_manual_button\">"))
        assertTrue(strings.contains("LSPosed"))
        assertFalse(strings.contains(
            "remote preferences 未初始化，先重新打开模块 App</string>\n    " +
                "<string name=\"scope_manual_manage_required\"",
        ))
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)
}
