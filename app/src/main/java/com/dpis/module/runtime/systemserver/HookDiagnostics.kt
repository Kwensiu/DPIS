package com.dpis.module.runtime.systemserver

import android.content.res.Configuration
import android.graphics.Rect
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.viewport.PerAppDisplayEnvironment

object HookDiagnostics {
    const val BUILD_MARKER = "2026-04-16-non-primary-alignment-v9"
    private const val MAX_PENDING_EVENTS = 32
    private val PENDING_LOCK = Any()
    private val PENDING_EVENTS = ArrayList<String?>()

    @JvmStatic
    fun buildBootstrapLog(): String =
        "system_server display emulation bootstrap ready: marker=$BUILD_MARKER"

    @JvmStatic
    fun buildInstallEnterLog(storeNull: Boolean, installed: Boolean): String =
        "system_server install enter: storeNull=$storeNull" +
                ", installed=$installed" +
                ", marker=$BUILD_MARKER"

    @JvmStatic
    fun buildInstallSkipLog(reason: String?): String =
        "system_server install skipped: reason=" + safeSummary(reason)

    @JvmStatic
    fun buildHookReadyLog(entryName: String?, className: String?, methodName: String?): String =
        "system_server hook ready: entry=$entryName" +
                ", class=$className" +
                ", method=$methodName"

    @JvmStatic
    fun buildHookMethodSelectedLog(entryName: String?, methodSignature: String?): String =
        "system_server hook method selected: entry=" + safeSummary(entryName) +
                ", method=" + safeSummary(methodSignature)

    @JvmStatic
    fun buildHookConstructorSelectedLog(
        entryName: String?,
        constructorSignature: String?,
    ): String =
        "system_server hook constructor selected: entry=" + safeSummary(entryName) +
                ", constructor=" + safeSummary(constructorSignature)

    @JvmStatic
    fun buildGateDisabledLog(totalEnabled: Boolean, safeModeEnabled: Boolean): String =
        "system_server hooks disabled: totalEnabled=$totalEnabled" +
                ", safeMode=$safeModeEnabled"


    @JvmStatic
    fun buildPackageReadyStateLog(
        processName: String?,
        packageName: String?,
        moduleLoadedObserved: Boolean,
        installAttempted: Boolean,
    ): String =
        "system_server package ready: process=" + safeSummary(processName) +
                ", package=" + safeSummary(packageName) +
                ", moduleLoadedObserved=$moduleLoadedObserved" +
                ", installAttempted=$installAttempted" +
                ", marker=$BUILD_MARKER"

    @JvmStatic
    fun buildHookMissingLog(entryName: String?, className: String?, methodName: String?): String =
        "system_server hook missing: entry=$entryName" +
                ", class=$className" +
                ", method=$methodName"

    @JvmStatic
    fun buildInstallSummaryLog(installedCount: Int, missingCount: Int): String =
        "system_server display hooks installed: installed=$installedCount" +
                ", missing=$missingCount"

    @JvmStatic
    fun buildProbeLog(
        entryName: String?,
        packageName: String?,
        originalSummary: String?,
        targetSummary: String?,
        actualSummary: String?,
    ): String =
        "system_server probe: entry=$entryName" +
                ", package=$packageName" +
                ", original=" + safeSummary(originalSummary) +
                ", target=" + safeSummary(targetSummary) +
                ", actual=" + safeSummary(actualSummary)

    @JvmStatic
    fun buildApplyLog(
        entryName: String?,
        packageName: String?,
        beforeSummary: String?,
        afterSummary: String?,
    ): String =
        "system_server apply: entry=$entryName" +
                ", package=$packageName" +
                ", before=" + safeSummary(beforeSummary) +
                ", after=" + safeSummary(afterSummary)

    @JvmStatic
    fun buildDisplayInfoProbeLog(
        entryName: String?,
        packageName: String?,
        displayInfoSummary: String?,
        frameSummary: String?,
        configurationSummary: String?,
    ): String =
        "system_server display probe: entry=$entryName" +
                ", package=$packageName" +
                ", displayInfo=" + safeSummary(displayInfoSummary) +
                ", frame=" + safeSummary(frameSummary) +
                ", config=" + safeSummary(configurationSummary)

    @JvmStatic
    fun buildPackageResolveMissLog(
        entryName: String?,
        selfClass: String?,
        argClasses: String?,
        argPreview: String?,
        textPackages: String?,
    ): String =
        "system_server package unresolved: entry=" + safeSummary(entryName) +
                ", this=" + safeSummary(selfClass) +
                ", argClasses=" + safeSummary(argClasses) +
                ", argPreview=" + safeSummary(argPreview) +
                ", textPackages=" + safeSummary(textPackages)

    @JvmStatic
    fun buildInterceptEnterLog(
        entryName: String?,
        selfClass: String?,
        argClasses: String?,
        argPreview: String?,
    ): String =
        "system_server intercept enter: entry=" + safeSummary(entryName) +
                ", this=" + safeSummary(selfClass) +
                ", argClasses=" + safeSummary(argClasses) +
                ", argPreview=" + safeSummary(argPreview)

    @JvmStatic
    @JvmOverloads
    fun buildConfigMissLog(
        entryName: String?,
        packageName: String?,
        targetPackageCandidates: String?,
        sourceState: String? = "",
    ): String {
        val state = safeSummary(sourceState)
        return "system_server config miss: entry=" + safeSummary(entryName) +
                ", package=" + safeSummary(packageName) +
                ", targetCandidates=" + safeSummary(targetPackageCandidates) +
                if (state == "unavailable") "" else ", sourceState=$state"
    }

    @JvmStatic
    fun buildConfigFallbackLog(
        entryName: String?,
        fromPackageName: String?,
        selectedPackageName: String?,
        targetPackageCandidates: String?,
    ): String =
        "system_server config fallback: entry=" + safeSummary(entryName) +
                ", fromPackage=" + safeSummary(fromPackageName) +
                ", selectedPackage=" + safeSummary(selectedPackageName) +
                ", targetCandidates=" + safeSummary(targetPackageCandidates)

    @JvmStatic
    fun buildInterceptErrorLog(entryName: String?, throwable: Throwable?): String {
        val throwableSummary = if (throwable == null) {
            "null"
        } else {
            throwable.javaClass.name + ":" + safeSummary(throwable.message)
        }
        return "system_server intercept error: entry=" + safeSummary(entryName) +
                ", throwable=$throwableSummary"
    }

    @JvmStatic
    fun buildTargetLog(packageName: String?, environment: PerAppDisplayEnvironment?): String {
        if (environment == null) {
            return "system_server display target: package=$packageName, environment=null"
        }
        return "system_server display target: package=$packageName" +
                ", widthDp=" + environment.widthDp +
                ", heightDp=" + environment.heightDp +
                ", smallestWidthDp=" + environment.smallestWidthDp +
                ", densityDpi=" + environment.densityDpi +
                ", widthPx=" + environment.widthPx +
                ", heightPx=" + environment.heightPx
    }

    @JvmStatic
    fun describeConfiguration(configuration: Configuration?): String? {
        if (configuration == null) {
            return null
        }
        return "config{widthDp=" + configuration.screenWidthDp +
                ",heightDp=" + configuration.screenHeightDp +
                ",smallestWidthDp=" + configuration.smallestScreenWidthDp +
                ",densityDpi=" + configuration.densityDpi +
                ",fontScale=" + configuration.fontScale + "}"
    }

    @JvmStatic
    fun describeFrame(frame: Rect?): String? {
        if (frame == null) {
            return null
        }
        return "frame{widthPx=" + frame.width() + ",heightPx=" + frame.height() + "}"
    }

    @JvmStatic
    fun describeState(configuration: Configuration?, frame: Rect?): String? {
        val configurationSummary = describeConfiguration(configuration)
        val frameSummary = describeFrame(frame)
        if (configurationSummary == null) {
            return frameSummary
        }
        if (frameSummary == null) {
            return configurationSummary
        }
        return "$configurationSummary,$frameSummary"
    }

    @JvmStatic
    fun recordPending(message: String?) {
        synchronized(PENDING_LOCK) {
            if (PENDING_EVENTS.size >= MAX_PENDING_EVENTS) {
                PENDING_EVENTS.removeAt(0)
            }
            PENDING_EVENTS.add(message)
        }
    }

    @JvmStatic
    fun flushPending() {
        val snapshot: List<String?>
        synchronized(PENDING_LOCK) {
            if (PENDING_EVENTS.isEmpty()) {
                return
            }
            snapshot = ArrayList(PENDING_EVENTS)
            PENDING_EVENTS.clear()
        }
        for (event in snapshot) {
            DpisLog.i("system_server replay: " + safeSummary(event))
        }
    }

    private fun safeSummary(summary: String?): String =
        if (summary.isNullOrEmpty()) "unavailable" else summary
}
