package com.dpis.module.viewport

import android.content.res.Configuration
import com.dpis.module.BuildConfig
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.runtime.probe.RuntimeClock
import java.util.concurrent.ConcurrentHashMap

object ViewportRuntimeMarkerProbe {
    private const val LOG_MIN_INTERVAL_MILLIS = 2_000L
    private const val MAX_LOG_KEYS = 128
    private val lastLogMillis = ConcurrentHashMap<String, Long>()

    @JvmStatic
    fun publishSystemServerProbe(
        packageName: String,
        sourceConfiguration: Configuration,
        result: PerAppDisplayEnvironment,
        targetSmallestWidthDp: Int,
        entryName: String
    ) =
        publishSystemServerProbe(
            packageName,
            sourceConfiguration,
            result,
            ViewportTargetSpec.absoluteDp(targetSmallestWidthDp),
            targetSmallestWidthDp,
            entryName
        )

    @JvmStatic
    fun publishSystemServerProbe(
        packageName: String?,
        sourceConfiguration: Configuration?,
        result: PerAppDisplayEnvironment?,
        targetSpec: ViewportTargetSpec?,
        effectiveSmallestWidthDp: Int,
        entryName: String?
    ) {
        if (!BuildConfig.DEBUG || packageName == null || sourceConfiguration == null || result == null || targetSpec == null || !targetSpec.isEnabled() || effectiveSmallestWidthDp <= 0) return
        val source = ViewportSourceSnapshot.fromConfiguration(
            ViewportSourceSnapshot.ORIGIN_SYSTEM_CONFIGURATION,
            sourceConfiguration,
            null
        )
        val viewportResult = ViewportOverride.Result(
            result.widthDp,
            result.heightDp,
            result.smallestWidthDp,
            result.densityDpi
        )
        val record = ViewportRuntimeMarkerBridge.createRecord(
            packageName,
            targetSpec,
            effectiveSmallestWidthDp,
            source,
            viewportResult,
            "s",
            RuntimeClock.crossProcessMarkerMillis()
        ) ?: return
        val encoded = ViewportRuntimeMarkerBridge.encode(record)
        val published = ViewportRuntimeMarkerBridge.isCurrentMarker(packageName, record)
        logAtMostEvery(
            "system|$packageName|$entryName|$published",
            "DPIS_VIEWPORT_MARKER system publish: entry=$entryName, package=$packageName, published=$published, length=${encoded.length}, targetFp=${record.targetFingerprint}, sourceSig=${record.sourceSignature}, resultSig=${record.resultSignature}, effectiveSwDp=${record.effectiveSmallestWidthDp}, property=${
                ViewportRuntimeMarkerBridge.propertyNameForPackage(packageName)
            }"
        )
    }

    @JvmStatic
    fun observeAppProcessProbe(packageName: String, targetSmallestWidthDp: Int, sourceTag: String) =
        observeAppProcessProbe(
            packageName,
            ViewportTargetSpec.absoluteDp(targetSmallestWidthDp),
            sourceTag
        )

    @JvmStatic
    fun observeAppProcessProbe(
        packageName: String?,
        targetSpec: ViewportTargetSpec?,
        sourceTag: String?
    ) {
        if (!BuildConfig.DEBUG || packageName == null || targetSpec == null || !targetSpec.isEnabled()) return
        val expected = targetSpec.fingerprint()
        val result = ViewportRuntimeMarkerBridge.read(
            packageName,
            expected,
            RuntimeClock.crossProcessMarkerMillis()
        )
        if (result.hit) {
            val record = result.record ?: return
            val detail =
                "source=$sourceTag, result=hit, ageMs=${result.ageMillis}, targetFp=${record.targetFingerprint}, sourceSig=${record.sourceSignature}, resultSig=${record.resultSignature}, effectiveSwDp=${record.effectiveSmallestWidthDp}, provenance=${record.provenance}"
            if (logAtMostEvery(
                    "app-hit|$packageName|$sourceTag|${record.targetFingerprint}|${record.resultSignature}",
                    "DPIS_VIEWPORT_MARKER app observe: $detail"
                )
            ) RuntimeHotPathEvents.probe(
                packageName,
                "viewport",
                "viewport_marker_app_observe",
                detail
            )
        } else {
            val detail =
                "source=$sourceTag, result=miss, reason=${result.reason}, expectedTargetFp=$expected, property=${
                    ViewportRuntimeMarkerBridge.propertyNameForPackage(packageName)
                }"
            if (logAtMostEvery(
                    "app-miss|$packageName|$sourceTag|$expected|${result.reason}",
                    "DPIS_VIEWPORT_MARKER app observe: $detail"
                )
            ) RuntimeHotPathEvents.probe(
                packageName,
                "viewport",
                "viewport_marker_app_observe",
                detail
            )
        }
    }

    private fun logAtMostEvery(key: String, message: String): Boolean {
        val now = RuntimeClock.elapsedRealtimeMillis()
        if (!lastLogMillis.containsKey(key) && lastLogMillis.size >= MAX_LOG_KEYS) lastLogMillis.clear()
        val previous = lastLogMillis.put(key, now)
        if (previous != null && now - previous < LOG_MIN_INTERVAL_MILLIS) return false
        DpisLog.i(message)
        return true
    }
}
