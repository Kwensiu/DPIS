package com.dpis.module;

import android.content.res.Configuration;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

final class SystemServerDisplayDiagnostics {
    static final String BUILD_MARKER = "2026-04-16-non-primary-alignment-v9";
    private static final int MAX_PENDING_EVENTS = 32;
    private static final Object PENDING_LOCK = new Object();
    private static final List<String> PENDING_EVENTS = new ArrayList<>();

    private SystemServerDisplayDiagnostics() {
    }

    static String buildBootstrapLog() {
        return "system_server display emulation bootstrap ready: marker=" + BUILD_MARKER;
    }

    static String buildInstallEnterLog(boolean storeNull, boolean installed) {
        return "system_server install enter: storeNull=" + storeNull
                + ", installed=" + installed
                + ", marker=" + BUILD_MARKER;
    }

    static String buildInstallSkipLog(String reason) {
        return "system_server install skipped: reason=" + safeSummary(reason);
    }

    static String buildHookReadyLog(String entryName, String className, String methodName) {
        return "system_server hook ready: entry=" + entryName
                + ", class=" + className
                + ", method=" + methodName;
    }

    static String buildGateDisabledLog(boolean totalEnabled, boolean safeModeEnabled) {
        return "system_server hooks disabled: totalEnabled=" + totalEnabled
                + ", safeMode=" + safeModeEnabled;
    }

    static String buildPackageReadyStateLog(String processName, String packageName,
                                            boolean moduleLoadedObserved,
                                            boolean installAttempted) {
        return "system_server package ready: process=" + safeSummary(processName)
                + ", package=" + safeSummary(packageName)
                + ", moduleLoadedObserved=" + moduleLoadedObserved
                + ", installAttempted=" + installAttempted
                + ", marker=" + BUILD_MARKER;
    }

    static String buildHookMissingLog(String entryName, String className, String methodName) {
        return "system_server hook missing: entry=" + entryName
                + ", class=" + className
                + ", method=" + methodName;
    }

    static String buildInstallSummaryLog(int installedCount, int missingCount) {
        return "system_server display hooks installed: installed=" + installedCount
                + ", missing=" + missingCount;
    }

    static String buildProbeLog(String entryName, String packageName, String originalSummary,
                                String targetSummary, String actualSummary) {
        return "system_server probe: entry=" + entryName
                + ", package=" + packageName
                + ", original=" + safeSummary(originalSummary)
                + ", target=" + safeSummary(targetSummary)
                + ", actual=" + safeSummary(actualSummary);
    }

    static String buildApplyLog(String entryName, String packageName,
                                String beforeSummary, String afterSummary) {
        return "system_server apply: entry=" + entryName
                + ", package=" + packageName
                + ", before=" + safeSummary(beforeSummary)
                + ", after=" + safeSummary(afterSummary);
    }

    static String buildDisplayInfoProbeLog(String entryName, String packageName,
                                           String displayInfoSummary, String frameSummary,
                                           String configurationSummary) {
        return "system_server display probe: entry=" + entryName
                + ", package=" + packageName
                + ", displayInfo=" + safeSummary(displayInfoSummary)
                + ", frame=" + safeSummary(frameSummary)
                + ", config=" + safeSummary(configurationSummary);
    }

    static String buildPackageResolveMissLog(String entryName,
                                             String selfClass,
                                             String argClasses,
                                             String argPreview,
                                             String textPackages) {
        return "system_server package unresolved: entry=" + safeSummary(entryName)
                + ", this=" + safeSummary(selfClass)
                + ", argClasses=" + safeSummary(argClasses)
                + ", argPreview=" + safeSummary(argPreview)
                + ", textPackages=" + safeSummary(textPackages);
    }

    static String buildInterceptEnterLog(String entryName,
                                         String selfClass,
                                         String argClasses,
                                         String argPreview) {
        return "system_server intercept enter: entry=" + safeSummary(entryName)
                + ", this=" + safeSummary(selfClass)
                + ", argClasses=" + safeSummary(argClasses)
                + ", argPreview=" + safeSummary(argPreview);
    }

    static String buildConfigMissLog(String entryName,
                                     String packageName,
                                     String targetPackageCandidates) {
        return "system_server config miss: entry=" + safeSummary(entryName)
                + ", package=" + safeSummary(packageName)
                + ", targetCandidates=" + safeSummary(targetPackageCandidates);
    }

    static String buildConfigFallbackLog(String entryName,
                                         String fromPackageName,
                                         String selectedPackageName,
                                         String targetPackageCandidates) {
        return "system_server config fallback: entry=" + safeSummary(entryName)
                + ", fromPackage=" + safeSummary(fromPackageName)
                + ", selectedPackage=" + safeSummary(selectedPackageName)
                + ", targetCandidates=" + safeSummary(targetPackageCandidates);
    }

    static String buildInterceptErrorLog(String entryName, Throwable throwable) {
        String throwableSummary = throwable == null
                ? "null"
                : throwable.getClass().getName() + ":" + safeSummary(throwable.getMessage());
        return "system_server intercept error: entry=" + safeSummary(entryName)
                + ", throwable=" + throwableSummary;
    }

    static String buildTargetLog(String packageName, PerAppDisplayEnvironment environment) {
        if (environment == null) {
            return "system_server display target: package=" + packageName + ", environment=null";
        }
        return "system_server display target: package=" + packageName
                + ", widthDp=" + environment.widthDp
                + ", heightDp=" + environment.heightDp
                + ", smallestWidthDp=" + environment.smallestWidthDp
                + ", densityDpi=" + environment.densityDpi
                + ", widthPx=" + environment.widthPx
                + ", heightPx=" + environment.heightPx;
    }

    static String describeConfiguration(Configuration configuration) {
        if (configuration == null) {
            return null;
        }
        return "config{widthDp=" + configuration.screenWidthDp
                + ",heightDp=" + configuration.screenHeightDp
                + ",smallestWidthDp=" + configuration.smallestScreenWidthDp
                + ",densityDpi=" + configuration.densityDpi
                + ",fontScale=" + configuration.fontScale + "}";
    }

    static String describeFrame(Rect frame) {
        if (frame == null) {
            return null;
        }
        return "frame{widthPx=" + frame.width() + ",heightPx=" + frame.height() + "}";
    }

    static String describeState(Configuration configuration, Rect frame) {
        String configurationSummary = describeConfiguration(configuration);
        String frameSummary = describeFrame(frame);
        if (configurationSummary == null) {
            return frameSummary;
        }
        if (frameSummary == null) {
            return configurationSummary;
        }
        return configurationSummary + "," + frameSummary;
    }

    static void recordPending(String message) {
        synchronized (PENDING_LOCK) {
            if (PENDING_EVENTS.size() >= MAX_PENDING_EVENTS) {
                PENDING_EVENTS.remove(0);
            }
            PENDING_EVENTS.add(message);
        }
    }

    static void flushPending() {
        List<String> snapshot;
        synchronized (PENDING_LOCK) {
            if (PENDING_EVENTS.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(PENDING_EVENTS);
            PENDING_EVENTS.clear();
        }
        for (String event : snapshot) {
            DpisLog.i("system_server replay: " + safeSummary(event));
        }
    }

    private static String safeSummary(String summary) {
        return summary == null || summary.isEmpty() ? "unavailable" : summary;
    }
}
