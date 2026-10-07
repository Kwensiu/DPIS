package com.dpis.module.diagnostics

import com.dpis.module.root.RootAccessProbe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class SummaryBuilder {
    fun build(
        input: Input?,
        startedAtMillis: Long,
        finishedAtMillis: Long,
        durationMs: Long,
        targetLaunchStarted: Boolean,
        rootAccess: RootAccessProbe.Result?,
        systemHooksEnabled: Boolean,
    ): String {
        val request = input ?: Input.empty()
        return buildString {
            appendLine("# DPIS")
            appendLine("source: feedback-diagnostic-summary")
            appendLine("package: ${request.packageName}")
            appendLine("label: ${valueOrUnknown(request.label)}")
            appendLine("versionName: ${valueOrUnknown(request.versionName)}")
            appendLine("startedAt: ${formatTime(startedAtMillis)}")
            appendLine("finishedAt: ${formatTime(finishedAtMillis)}")
            appendLine("durationMs: $durationMs")
            appendLine("targetLaunchStarted: $targetLaunchStarted")
            appendLine("rootStatus: ${rootStatus(rootAccess)}")
            appendLine("rootProvider: ${rootProvider(rootAccess)}")
            appendLine("systemHooksEnabled: $systemHooksEnabled")
            appendLine("scopeKnown: ${request.scopeKnown}")
            appendLine("inScope: ${request.inScope}")
            appendLine("dpisEnabled: ${request.dpisEnabled}")
            appendLine("previewFromGlobalPrefill: ${request.previewFromGlobalPrefill}")
            appendLine("viewport: ${formatViewport(request)}")
            appendLine("font: ${formatFont(request)}")
            append("notes: ")
            appendLine(
                if (targetLaunchStarted) {
                    "Diagnostic package includes diagnostic.txt, timeline.tsv, " +
                        "module-effects.tsv, dpis-log.txt, and lsposed-log.txt. " +
                        "Runtime evidence is collected from DPIS app events, " +
                        "runtime transport, and the LSPosed log window when available."
                } else {
                    "Target app launch failed or was unavailable."
                },
            )
        }
    }

    private fun formatViewport(input: Input) = "${input.viewportSummary}, mode=${input.viewportApplyMode}"

    private fun formatFont(input: Input): String {
        val scale = input.fontScalePercent?.let { "${it}%" } ?: "off"
        val typeface = input.typefaceId ?: "default"
        val hookDomains = if (input.fontHookDomainsRaw != null) "custom" else "default"
        return "scale=$scale, mode=${input.fontApplyMode}, typeface=$typeface, hookDomains=$hookDomains"
    }

    private fun rootStatus(rootAccess: RootAccessProbe.Result?): String =
        (rootAccess ?: RootAccessProbe.Result.unknown()).status.name.lowercase(Locale.ROOT)

    private fun rootProvider(rootAccess: RootAccessProbe.Result?): String =
        rootAccess?.provider?.takeUnless(String::isBlank) ?: UNKNOWN

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date(millis))

    private fun valueOrUnknown(value: String?): String = normalizeOrUnknown(value)

    class Input(
        packageName: String?,
        val label: String?,
        val versionName: String?,
        val scopeKnown: Boolean,
        val inScope: Boolean,
        val dpisEnabled: Boolean,
        val previewFromGlobalPrefill: Boolean,
        viewportSummary: String?,
        viewportApplyMode: String?,
        val fontScalePercent: Int?,
        fontApplyMode: String?,
        val typefaceId: String?,
        val fontHookDomainsRaw: String?,
    ) {
        val packageName: String = normalizeOrUnknown(packageName)
        val viewportSummary: String = normalizeOrUnknown(viewportSummary)
        val viewportApplyMode: String = normalizeOrUnknown(viewportApplyMode)
        val fontApplyMode: String = normalizeOrUnknown(fontApplyMode)

        companion object {
            fun empty() = Input(
                packageName = UNKNOWN,
                label = UNKNOWN,
                versionName = UNKNOWN,
                scopeKnown = false,
                inScope = false,
                dpisEnabled = false,
                previewFromGlobalPrefill = false,
                viewportSummary = "off",
                viewportApplyMode = UNKNOWN,
                fontScalePercent = null,
                fontApplyMode = UNKNOWN,
                typefaceId = null,
                fontHookDomainsRaw = null,
            )
        }
    }

}

private const val UNKNOWN = "unknown"

private fun normalizeOrUnknown(value: String?): String =
    value?.trim().takeUnless { it.isNullOrEmpty() } ?: UNKNOWN
