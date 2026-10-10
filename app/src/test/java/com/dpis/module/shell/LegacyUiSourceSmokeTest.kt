package com.dpis.module

import com.dpis.module.applist.presentation.StatusFormatter
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyUiSourceSmokeTest {
    @Test
    fun scopeUnavailableActionPromptsManualLsposedSelection() {
        val source = read("src/main/java/com/dpis/module/hooks/SystemScopeCoordinator.kt")
        val unavailableBlock = source.substringBefore("ScopeRequestGate.shared()")

        assertFalse(unavailableBlock.contains("openLsposedManager"))
        assertFalse(unavailableBlock.contains("scope_manual_manage_required"))
        assertFalse(unavailableBlock.contains("scope_manual_open_failed"))
        assertFalse(unavailableBlock.contains("R.string.status_save_requires_init"))
    }

    @Test
    fun unknownScopeHidesInjectionStatus() {
        val labels = StatusFormatter.Labels(
            "Injected", "Not injected", "Enabled", "Disabled", "Not enabled",
            "Not installed", "No value", "System", "Compat", "Interface", "Interface",
            "Font", "WeChat DPI", Locale.US,
        )
        val status = StatusFormatter.formatCompact(
            labels,
            StatusFormatter.StatusInput(
                false,
                false,
                ViewportTargetSpec.absoluteDp(320),
                ViewportApplyMode.COMPAT,
                null,
                FontApplyMode.OFF,
                null,
                true,
            ),
        )

        assertEquals("Interface 320dp", status)
    }

    @Test
    fun scopeActionIsEnabledOnlyWhenScopeIsKnown() {
        val editor = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorContent.kt")

        assertTrue(editor.contains("enabled = state.item.scopeKnown"))
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)
}
