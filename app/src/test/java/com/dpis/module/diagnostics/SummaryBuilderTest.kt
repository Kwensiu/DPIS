package com.dpis.module.diagnostics

import com.dpis.module.root.RootAccessProbe
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryBuilderTest {
    @Test
    fun summaryReportsConfiguredDiagnosticInputsAndSuccessfulLaunch() {
        val input = SummaryBuilder.Input(
            packageName = " com.example.target ",
            label = "Target",
            versionName = "1.2.3",
            scopeKnown = true,
            inScope = true,
            dpisEnabled = true,
            previewFromGlobalPrefill = false,
            viewportSummary = "scale=125%",
            viewportApplyMode = "system",
            fontScalePercent = 110,
            fontApplyMode = "field_rewrite",
            typefaceId = "font_modern",
            fontHookDomainsRaw = "resources_font",
        )

        val summary = SummaryBuilder().build(
            input = input,
            startedAtMillis = 0L,
            finishedAtMillis = 1_000L,
            durationMs = 1_000L,
            targetLaunchStarted = true,
            rootAccess = RootAccessProbe.Result.available("Magisk"),
            systemHooksEnabled = true,
        )

        assertTrue(summary.contains("package: com.example.target"))
        assertTrue(summary.contains("rootStatus: available"))
        assertTrue(summary.contains("rootProvider: Magisk"))
        assertTrue(summary.contains("viewport: scale=125%, mode=system"))
        assertTrue(summary.contains("font: scale=110%, mode=field_rewrite, typeface=font_modern, hookDomains=custom"))
        assertTrue(summary.contains("Diagnostic package includes diagnostic.txt"))
    }

    @Test
    fun summaryUsesUnknownDefaultsWhenInputAndRootAreUnavailable() {
        val summary = SummaryBuilder().build(
            input = null,
            startedAtMillis = 0L,
            finishedAtMillis = 0L,
            durationMs = 0L,
            targetLaunchStarted = false,
            rootAccess = null,
            systemHooksEnabled = false,
        )

        assertTrue(summary.contains("package: unknown"))
        assertTrue(summary.contains("rootStatus: unknown"))
        assertTrue(summary.contains("rootProvider: unknown"))
        assertTrue(summary.contains("viewport: off, mode=unknown"))
        assertTrue(summary.contains("font: scale=off, mode=unknown, typeface=default, hookDomains=default"))
        assertTrue(summary.contains("Target app launch failed or was unavailable."))
    }
}
