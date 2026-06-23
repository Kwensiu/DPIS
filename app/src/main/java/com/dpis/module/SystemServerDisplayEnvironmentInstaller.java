package com.dpis.module;

import android.content.res.Configuration;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Binder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;

import io.github.libxposed.api.XposedInterface;

final class SystemServerDisplayEnvironmentInstaller {
    private static final String PROP_DISABLE_SYSTEM_SERVER_FONT_PACKAGE =
            "debug.dpis.font.disable_system_server_package";
    private static final String PROP_SYSTEM_SERVER_FONT_FALLBACK_PACKAGE =
            "debug.dpis.font.system_server_fallback_package";
    private static final int MAX_PACKAGE_RECURSION_DEPTH = 5;
    private static final ReflectionProbeCache REFLECTION_CACHE = new ReflectionProbeCache();
    private static final SystemServerPackageUidResolver PACKAGE_UID_RESOLVER =
            new SystemServerPackageUidResolver(
                    ConfigSnapshotRefreshPolicy.SYSTEM_SERVER_TTL_MILLIS);
    private static final String[] PACKAGE_STRING_METHOD_NAMES = new String[]{
            "getOwningPackage",
            "getPackageName",
            "getPackage",
            "getOpPackageName"
    };
    private static final String[] PACKAGE_OBJECT_METHOD_NAMES = new String[]{
            "getIntent",
            "getComponent",
            "getActivityInfo",
            "getApplicationInfo",
            "getRequest",
            "getTargetActivity",
            "getOrigActivity",
            "getRealActivity"
    };
    private static final String[] PACKAGE_STRING_FIELD_NAMES = new String[]{
            "packageName", "mPackageName", "package", "launchedFromPackage"
    };
    private static final String[] PACKAGE_OBJECT_FIELD_NAMES = new String[]{
            "intent",
            "mIntent",
            "component",
            "mComponent",
            "activityInfo",
            "applicationInfo",
            "request",
            "mRequest",
            "targetActivity",
            "origActivity",
            "realActivity"
    };
    private static final String[] CONFIGURATION_FIELD_NAMES = new String[]{
            "mergedConfiguration",
            "mLastReportedConfiguration",
            "mTmpConfig",
            "configuration",
            "mConfiguration"
    };
    private static final String[] WINDOW_CONTAINER_CONFIGURATION_FIELD_NAMES = new String[]{
            "mFullConfiguration",
            "mResolvedOverrideConfiguration",
            "mMergedOverrideConfiguration",
            "mTmpConfig",
            "mConfiguration",
            "configuration"
    };
    private static final String[] MERGED_CONFIGURATION_FIELD_NAMES = new String[]{
            "mGlobalConfig",
            "mOverrideConfig",
            "mMergedConfig"
    };
    private static final String[] DISPLAY_INFO_FIELD_NAMES = new String[]{
            "displayInfo",
            "mDisplayInfo",
            "mTmpDisplayInfo",
            "mLastDisplayInfo"
    };
    private static final String[] FRAME_DIRECT_FIELD_NAMES = new String[]{
            "frame", "mFrame", "displayFrame"
    };
    private static final String[] FRAME_NESTED_FIELD_NAMES = new String[]{
            "frames", "windowFrames", "clientWindowFrames", "outFrames", "result"
    };
    private static volatile int installedPid = -1;

    private SystemServerDisplayEnvironmentInstaller() {
    }

    static void install(XposedInterface xposed,
                        DpiConfigStore store,
                        ModernApiCapabilities apiCapabilities) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            SystemServerDisplayDiagnostics.recordPending("system_server install skipped: reason=already-installed-fast-path");
            DpisLog.i(SystemServerDisplayDiagnostics.buildInstallSkipLog("already-installed-fast-path"));
            return;
        }
        synchronized (SystemServerDisplayEnvironmentInstaller.class) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                SystemServerDisplayDiagnostics.recordPending("system_server install skipped: reason=already-installed-synchronized");
                DpisLog.i(SystemServerDisplayDiagnostics.buildInstallSkipLog("already-installed-synchronized"));
                return;
            }
            SystemServerDisplayDiagnostics.recordPending(
                    SystemServerDisplayDiagnostics.buildInstallEnterLog(store == null, installedPid != -1));
            DpisLog.i(SystemServerDisplayDiagnostics.buildInstallEnterLog(
                    store == null, installedPid != -1));
            try {
                SystemServerDisplayDiagnostics.recordPending(
                        SystemServerDisplayDiagnostics.buildBootstrapLog());
                DpisLog.i(SystemServerDisplayDiagnostics.buildBootstrapLog());
                HookRuntimePolicy policy = HookRuntimePolicy.fromNullableStore(store);
                if (!policy.systemServerHooksEnabled) {
                    SystemServerDisplayDiagnostics.recordPending(
                            SystemServerDisplayDiagnostics.buildGateDisabledLog(
                                    false, policy.systemServerSafeModeEnabled));
                DpisLog.i(SystemServerDisplayDiagnostics.buildGateDisabledLog(
                            false, policy.systemServerSafeModeEnabled));
                    FeedbackDiagnosticRuntimeEvents.recordHotReload(
                            null,
                            "system_server",
                            "skipped",
                            "system_server install skipped: gate disabled");
                    installedPid = ProcessScopedInstallGate.currentPid();
                    return;
                }
                PerAppDisplayConfigSource source = new PerAppDisplayConfigSource(
                        new RefreshingConfigSnapshotProvider(
                                () -> ConfigSnapshotLoader.fromStore(
                                        ConfigStoreFactory.createForXposedHost(xposed)),
                                ConfigSnapshotRefreshPolicy.SYSTEM_SERVER_TTL_MILLIS));
                Set<String> configuredPackages = source.getConfiguredPackages();
                int installedCount = 0;
                int missingCount = 0;
                if (SystemServerMutationPolicy.shouldInstallTarget(
                        "launch-activity-item", policy.systemServerSafeModeEnabled)) {
                    if (installLaunchActivityItemHook(
                            xposed,
                            source,
                            SystemServerHookCatalog.LAUNCH_ACTIVITY_ITEM,
                            apiCapabilities)) {
                        installedCount++;
                    } else {
                        missingCount++;
                    }
                }
                if (SystemServerMutationPolicy.shouldInstallTarget(
                        "hyperos-rust-process", policy.systemServerSafeModeEnabled)) {
                    if (HyperOsRustProcessHookInstaller.install(xposed, source)) {
                        installedCount++;
                    } else {
                        missingCount++;
                    }
                }
                for (SystemServerHookSpec hookSpec : SystemServerHookCatalog.methodHookSpecs()) {
                    if (!SystemServerMutationPolicy.shouldInstallTarget(
                            hookSpec.entryName, policy.systemServerSafeModeEnabled)) {
                        continue;
                    }
                    if (installTargetHooks(
                            xposed,
                            source,
                            hookSpec,
                            configuredPackages,
                            apiCapabilities)) {
                        installedCount++;
                    } else {
                        missingCount++;
                    }
                }
                SystemServerDisplayDiagnostics.recordPending(
                        SystemServerDisplayDiagnostics.buildInstallSummaryLog(
                                installedCount, missingCount));
                DpisLog.i(SystemServerDisplayDiagnostics.buildInstallSummaryLog(
                        installedCount, missingCount));
                FeedbackDiagnosticRuntimeEvents.recordHotReload(
                        null,
                        "system_server",
                        "installed",
                        "system_server install summary: installed=" + installedCount
                                + ", missing=" + missingCount);
                installedPid = ProcessScopedInstallGate.currentPid();
            } catch (Throwable throwable) {
                SystemServerDisplayDiagnostics.recordPending(
                        "system_server install failed: throwable=" + throwable.getClass().getName());
                DpisLog.e("system_server install failed", throwable);
                throw throwable;
            }
        }
    }

    static String findPackageNameForTest(Object self, Object... args) {
        List<Object> values = new ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                values.add(arg);
            }
        }
        return findPackageName(self, values);
    }

    static String resolveConfiguredPackageForTest(Object self,
                                                  Predicate<String> hasConfig,
                                                  Object... args) {
        List<Object> values = new ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                values.add(arg);
            }
        }
        return resolveConfiguredPackage(self, values, packageName -> {
            if (packageName == null || hasConfig == null || !hasConfig.test(packageName)) {
                return null;
            }
            return new PerAppDisplayConfig(packageName, 1);
        }).packageName;
    }

    private static boolean installTargetHooks(XposedInterface xposed,
                                              PerAppDisplayConfigSource source,
                                              SystemServerHookSpec hookSpec,
                                              Set<String> configuredPackages,
                                              ModernApiCapabilities apiCapabilities) {
        boolean hooked = false;
        for (ClassLoader classLoader : resolveCandidateClassLoaders()) {
            for (String className : hookSpec.classNames) {
                Class<?> clazz = resolveClass(className, classLoader);
                if (clazz == null) {
                    continue;
                }
                for (Method method : clazz.getDeclaredMethods()) {
                    if (!matchesAnyMethodName(method, hookSpec.methodNames)
                            || Modifier.isAbstract(method.getModifiers())) {
                        continue;
                    }
                    apiCapabilities.applyStableHookId(
                                    xposed.hook(method)
                                            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                                    hookSpec.hookIdFor(method))
                            .intercept(chain -> {
                                Object result = null;
                                boolean proceedAttempted = false;
                                boolean proceeded = false;
                                try {
                                    Object thisObject = chain.getThisObject();
                                    List<Object> args = chain.getArgs();
                                    if (!source.isSystemServerHooksEnabled()) {
                                        return chain.proceed();
                                    }
                                    if (SystemServerEntryRoute.isDisplayManagerInfo(hookSpec.entryName)) {
                                        Object displayManagerResult = chain.proceed();
                                        applyDisplayManagerInfoResult(
                                                source, hookSpec.entryName, displayManagerResult);
                                        return displayManagerResult;
                                    }
                                    boolean loggingEnabled = DpisLog.isLoggingEnabled();
                                    Set<String> currentConfiguredPackages = source.getConfiguredPackages();
                                    if (!shouldInspectHotEntry(
                                            hookSpec.entryName,
                                            thisObject,
                                            args,
                                            currentConfiguredPackages)) {
                                        return chain.proceed();
                                    }
                                    if (loggingEnabled
                                            && SystemServerHookLogGate.shouldLogInterceptEnter(
                                                    hookSpec.entryName)) {
                                        logInterceptEnter(hookSpec.entryName, thisObject, args);
                                    }
                                    ResolvedPackage resolvedPackage = resolveConfiguredPackage(
                                            thisObject,
                                            args,
                                            packageName -> selectConfigForSystemServerEntry(
                                                    hookSpec.entryName, source.get(packageName)));
                                    if (resolvedPackage.packageName == null) {
                                        if (loggingEnabled) {
                                            logPackageResolveMiss(hookSpec.entryName, thisObject, args);
                                        }
                                        return chain.proceed();
                                    }
                                    String packageName = resolvedPackage.packageName;
                                    PerAppDisplayConfig config = resolvedPackage.config;
                                    if (config == null) {
                                        if (loggingEnabled) {
                                            logConfigMiss(hookSpec.entryName, packageName,
                                                    resolvedPackage.candidatePackagesSummary,
                                                    currentConfiguredPackages.size());
                                        }
                                        return chain.proceed();
                                    }
                                    if (loggingEnabled && resolvedPackage.fallbackFromPackage != null) {
                                        logConfigFallback(hookSpec.entryName,
                                                resolvedPackage.fallbackFromPackage,
                                                packageName,
                                                resolvedPackage.candidatePackagesSummary);
                                    }
                                    Snapshot before = captureSnapshot(thisObject, args);
                                    PerAppDisplayEnvironment preEnvironment = resolveTargetEnvironment(
                                            packageName, before, before, config);
                                    if (SystemServerMutationPolicy.shouldApplyPreProceedMutations(
                                            hookSpec.entryName)) {
                                        PerAppDisplayEnvironment applyEnvironment =
                                                resolveMarkerGatedEnvironment(
                                                        hookSpec.entryName,
                                                        packageName,
                                                        before,
                                                        preEnvironment,
                                                        config);
                                        boolean changed = applyEnvironment(
                                                hookSpec.entryName, before, applyEnvironment, config);
                                        changed |= applyConfigDispatchObject(
                                                hookSpec.entryName, thisObject, applyEnvironment);
                                        if (changed) {
                                            logViewportMarkerProbe(
                                                    hookSpec.entryName, packageName, before, applyEnvironment, config);
                                        }
                                    }
                                    proceedAttempted = true;
                                    result = chain.proceed();
                                    proceeded = true;
                                    Snapshot after = captureSnapshot(thisObject, args);
                                    PerAppDisplayEnvironment environment = resolveTargetEnvironment(
                                            packageName, before, after, config);
                                    PerAppDisplayEnvironment effectiveEnvironment = chooseEffectiveEnvironment(
                                            preEnvironment, environment);
                                    if (SystemServerEntryRoute.isConfigDispatch(hookSpec.entryName)) {
                                        applyConfigDispatchObject(
                                                hookSpec.entryName, thisObject, effectiveEnvironment);
                                    }
                                    if (loggingEnabled) {
                                        logTargetComputation(hookSpec.entryName, packageName,
                                                preEnvironment, environment, effectiveEnvironment);
                                    }
                                    Snapshot mutated = after;
                                    if (effectiveEnvironment == null) {
                                        if (loggingEnabled) {
                                            logEnvironmentNull(hookSpec.entryName,
                                                    packageName,
                                                    SystemServerDisplayDiagnostics.describeState(
                                                            before.configuration, before.frame),
                                                    SystemServerDisplayDiagnostics.describeState(
                                                            after.configuration, after.frame));
                                        }
                                    }
                                    if (SystemServerMutationPolicy.shouldApplyPostProceedMutations(
                                            hookSpec.entryName)) {
                                        String beforeApplySummary = loggingEnabled
                                                ? SystemServerDisplayDiagnostics.describeState(
                                                after.configuration, after.frame)
                                                : null;
                                        PerAppDisplayEnvironment applyEnvironment =
                                                resolveMarkerGatedEnvironment(
                                                        hookSpec.entryName,
                                                        packageName,
                                                        after,
                                                        effectiveEnvironment,
                                                        config);
                                        if (applyEnvironment(hookSpec.entryName, after, applyEnvironment, config)) {
                                            logViewportMarkerProbe(
                                                    hookSpec.entryName, packageName, after, applyEnvironment, config);
                                            if (loggingEnabled) {
                                                String afterApplySummary = SystemServerDisplayDiagnostics.describeState(
                                                        mutated.configuration, mutated.frame);
                                                String message = SystemServerDisplayDiagnostics.buildApplyLog(
                                                        hookSpec.entryName,
                                                        packageName,
                                                        beforeApplySummary,
                                                        afterApplySummary);
                                                String key = "apply|" + hookSpec.entryName + "|" + packageName;
                                                logIfChanged(
                                                        key,
                                                        message,
                                                        resolveLogMinIntervalMs(hookSpec.entryName));
                                            }
                                        } else {
                                            if (loggingEnabled) {
                                                logApplySkipped(hookSpec.entryName, packageName, beforeApplySummary);
                                            }
                                        }
                                    }
                                    if (loggingEnabled) {
                                        String originalSummary = SystemServerDisplayDiagnostics.describeState(
                                                before.configuration, before.frame);
                                        String actualSummary = SystemServerDisplayDiagnostics.describeState(
                                                mutated.configuration, mutated.frame);
                                        String targetSummary = buildTargetSummary(
                                                effectiveEnvironment,
                                                after.frame != null ? after.frame : before.frame,
                                                config);
                                        logProbe(hookSpec.entryName, packageName, originalSummary,
                                                targetSummary, actualSummary);
                                        logDisplayInfoProbe(hookSpec.entryName, packageName,
                                                mutated.displayInfo, mutated.frame, mutated.configuration);
                                    }
                                    return result;
                                } catch (Throwable throwable) {
                                    DpisLog.e(SystemServerDisplayDiagnostics.buildInterceptErrorLog(
                                            hookSpec.entryName, throwable), throwable);
                                    if (proceeded) {
                                        return result;
                                    }
                                    if (proceedAttempted) {
                                        throw throwable;
                                    }
                                    return chain.proceed();
                                }
                            });
                    hooked = true;
                }
                if (hooked) {
                    break;
                }
            }
            if (hooked) {
                break;
            }
        }
        if (hooked) {
            SystemServerDisplayDiagnostics.recordPending(
                    SystemServerDisplayDiagnostics.buildHookReadyLog(
                            hookSpec.entryName, hookSpec.describeClassNames(), hookSpec.describeMethodNames()));
            DpisLog.i(SystemServerDisplayDiagnostics.buildHookReadyLog(
                    hookSpec.entryName, hookSpec.describeClassNames(), hookSpec.describeMethodNames()));
            return true;
        }
        SystemServerDisplayDiagnostics.recordPending(
                SystemServerDisplayDiagnostics.buildHookMissingLog(
                        hookSpec.entryName, hookSpec.describeClassNames(), hookSpec.describeMethodNames()));
        DpisLog.i(SystemServerDisplayDiagnostics.buildHookMissingLog(
                hookSpec.entryName, hookSpec.describeClassNames(), hookSpec.describeMethodNames()));
        return false;
    }

    private static boolean installLaunchActivityItemHook(XposedInterface xposed,
                                                         PerAppDisplayConfigSource source,
                                                         SystemServerHookSpec hookSpec,
                                                         ModernApiCapabilities apiCapabilities) {
        Class<?> clazz = resolveClass(hookSpec.classNames[0], null);
        if (clazz == null) {
            DpisLog.i("system_server hook missing: entry=launch-activity-item, class=LaunchActivityItem");
            return false;
        }
        boolean hooked = false;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            apiCapabilities.applyStableHookId(
                            xposed.hook(constructor)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            hookSpec.hookIdFor(constructor))
                    .intercept(chain -> {
                        Object result;
                        try {
                            if (source.isSystemServerHooksEnabled()) {
                                applyLaunchActivityItemArgs(source, chain.getArgs());
                            }
                        } catch (Throwable throwable) {
                            DpisLog.e("system_server launch-activity-item pre-init failed", throwable);
                        }
                        result = chain.proceed();
                        try {
                            if (source.isSystemServerHooksEnabled()) {
                                applyLaunchActivityItemObject(source, chain.getThisObject());
                            }
                        } catch (Throwable throwable) {
                            DpisLog.e("system_server launch-activity-item post-init failed", throwable);
                        }
                        return result;
                    });
            hooked = true;
        }
        if (hooked) {
            DpisLog.i("system_server hook ready: entry=launch-activity-item, class=LaunchActivityItem");
        }
        return hooked;
    }

    private static void applyLaunchActivityItemArgs(PerAppDisplayConfigSource source,
                                                    List<Object> args) {
        String packageName = findActivityInfoPackage(args);
        if (packageName == null) {
            return;
        }
        PerAppDisplayConfig config = selectConfigForSystemServerEntry(
                "launch-activity-item", source.get(packageName));
        if (config == null) {
            return;
        }
        Configuration baseConfiguration = findFirstConfiguration(args);
        if (baseConfiguration == null) {
            return;
        }
        PerAppDisplayEnvironment environment = null;
        if (hasSystemServerViewportOverride(config)) {
            int widthPx = resolveWidthPx(baseConfiguration, null);
            int heightPx = resolveHeightPx(baseConfiguration, null);
            environment = resolveAlreadyAppliedRelativeScaleEnvironment(
                    packageName, baseConfiguration, widthPx, heightPx, config);
            if (environment == null) {
                environment = PerAppDisplayOverrideCalculator.calculate(
                        baseConfiguration, widthPx, heightPx, config.targetViewportSpec);
            }
            environment = resolveMarkerGatedEnvironment(
                    "launch-activity-item",
                    packageName,
                    new Snapshot(baseConfiguration, null, null),
                    environment,
                    config);
        }
        String beforeSummary = DpisLog.isLoggingEnabled()
                ? describeConfigurationArgs(args)
                : null;
        float beforeFontScale = baseConfiguration.fontScale;
        boolean changed = false;
        boolean fontChanged = false;
        for (Object arg : args) {
            if (arg instanceof Configuration configuration) {
                if (environment != null
                        && SystemServerMutationPolicy.shouldApplyMutationField(
                                "launch-activity-item", SystemServerMutationField.VIEWPORT)) {
                    changed |= applyConfiguration(configuration, environment);
                }
                boolean appliedFont = SystemServerMutationPolicy.shouldApplyMutationField(
                        "launch-activity-item", SystemServerMutationField.FONT_SCALE)
                        && applyFontScale(configuration, config);
                fontChanged |= appliedFont;
                changed |= appliedFont;
            }
        }
        Object activityItem = findLaunchActivityItem(args);
        if (activityItem != null && environment != null) {
            changed |= applyLaunchActivityItemConfigurationFields(activityItem, environment);
        }
        if (fontChanged) {
            HyperOsFlutterFontBridge.publishTarget(packageName, config);
            reportSystemServerFontConfig(packageName, beforeFontScale, baseConfiguration.fontScale);
        }
        if (changed && DpisLog.isLoggingEnabled()) {
            String message = "system_server launch-activity-item apply: package=" + packageName
                    + ", before=" + safeToString(beforeSummary)
                    + ", after=" + describeConfigurationArgs(args)
                    + ", target=" + describeEnvironment(environment);
            logIfChanged("launch-activity-item|" + packageName, message,
                    resolveLogMinIntervalMs("launch-activity-item"));
        }
    }

    private static void logViewportMarkerProbe(String entryName,
                                                   String packageName,
                                                   Snapshot source,
                                                   PerAppDisplayEnvironment environment,
                                                   PerAppDisplayConfig config) {
        if (source == null || config == null || environment == null) {
            return;
        }
        Configuration markerSourceConfiguration = source.configuration != null
                ? new Configuration(source.configuration)
                : null;
        ViewportRuntimeMarkerProbe.publishSystemServerProbe(
                packageName,
                markerSourceConfiguration,
                environment,
                config.targetViewportSpec,
                environment.smallestWidthDp,
                entryName);
    }

    private static String findActivityInfoPackage(List<Object> args) {
        for (Object arg : args) {
            if (arg instanceof ActivityInfo activityInfo
                    && activityInfo.packageName != null
                    && isLikelyPackageName(activityInfo.packageName)) {
                return activityInfo.packageName;
            }
        }
        return null;
    }

    private static Configuration findFirstConfiguration(List<Object> args) {
        for (Object arg : args) {
            if (arg instanceof Configuration configuration) {
                return configuration;
            }
        }
        return null;
    }

    private static void applyLaunchActivityItemObject(PerAppDisplayConfigSource source,
                                                      Object launchActivityItem) {
        if (source == null || launchActivityItem == null) {
            return;
        }
        ActivityInfo activityInfo = readLaunchActivityInfo(launchActivityItem);
        String packageName = activityInfo != null ? activityInfo.packageName : null;
        if (packageName == null || !isLikelyPackageName(packageName)) {
            return;
        }
        PerAppDisplayConfig config = selectConfigForSystemServerEntry(
                "launch-activity-item", source.get(packageName));
        if (config == null || !hasSystemServerViewportOverride(config)) {
            return;
        }
        Configuration baseConfiguration = readLaunchActivityConfiguration(launchActivityItem);
        if (baseConfiguration == null) {
            return;
        }
        int widthPx = resolveWidthPx(baseConfiguration, null);
        int heightPx = resolveHeightPx(baseConfiguration, null);
        PerAppDisplayEnvironment environment = resolveAlreadyAppliedRelativeScaleEnvironment(
                packageName, baseConfiguration, widthPx, heightPx, config);
        if (environment == null) {
            environment = PerAppDisplayOverrideCalculator.calculate(
                    baseConfiguration, widthPx, heightPx, config.targetViewportSpec);
        }
        environment = resolveMarkerGatedEnvironment(
                "launch-activity-item",
                packageName,
                new Snapshot(baseConfiguration, null, null),
                environment,
                config);
        if (environment == null) {
            return;
        }
        if (applyLaunchActivityItemConfigurationFields(launchActivityItem, environment)
                && DpisLog.isLoggingEnabled()) {
            logIfChanged("launch-activity-item-object|" + packageName,
                    "system_server launch-activity-item object apply: package=" + packageName
                            + ", target=" + describeEnvironment(environment),
                    resolveLogMinIntervalMs("launch-activity-item"));
        }
    }

    private static ActivityInfo readLaunchActivityInfo(Object launchActivityItem) {
        Object value = readField(launchActivityItem, "mInfo");
        return value instanceof ActivityInfo info ? info : null;
    }

    private static Configuration readLaunchActivityConfiguration(Object launchActivityItem) {
        Object override = readField(launchActivityItem, "mOverrideConfig");
        if (override instanceof Configuration configuration) {
            return configuration;
        }
        Object current = readField(launchActivityItem, "mCurConfig");
        return current instanceof Configuration configuration ? configuration : null;
    }

    private static Object findLaunchActivityItem(List<Object> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }
        for (Object arg : args) {
            if (arg != null
                    && "android.app.servertransaction.LaunchActivityItem".equals(
                    arg.getClass().getName())) {
                return arg;
            }
        }
        return null;
    }

    private static boolean applyLaunchActivityItemConfigurationFields(
            Object launchActivityItem,
            PerAppDisplayEnvironment environment) {
        if (launchActivityItem == null || environment == null) {
            return false;
        }
        boolean changed = false;
        changed |= applyConfigurationField(launchActivityItem, "mCurConfig", environment);
        changed |= applyConfigurationField(launchActivityItem, "mOverrideConfig", environment);
        return changed;
    }

    private static boolean applyConfigurationField(Object target,
                                                   String fieldName,
                                                   PerAppDisplayEnvironment environment) {
        Object value = readField(target, fieldName);
        if (!(value instanceof Configuration configuration)) {
            return false;
        }
        return applyConfiguration(configuration, environment);
    }

    private static boolean applyConfigDispatchObject(String entryName,
                                                     Object target,
                                                     PerAppDisplayEnvironment environment) {
        if (!SystemServerEntryRoute.isConfigDispatch(entryName) || target == null || environment == null) {
            return false;
        }
        boolean changed = false;
        for (String fieldName : WINDOW_CONTAINER_CONFIGURATION_FIELD_NAMES) {
            changed |= applyConfigurationField(target, fieldName, environment);
        }
        for (String fieldName : CONFIGURATION_FIELD_NAMES) {
            Object value = readField(target, fieldName);
            changed |= applyMergedConfigurationFields(value, environment);
        }
        return changed;
    }

    private static boolean applyMergedConfigurationFields(Object target,
                                                          PerAppDisplayEnvironment environment) {
        if (target == null || environment == null) {
            return false;
        }
        boolean changed = false;
        for (String fieldName : MERGED_CONFIGURATION_FIELD_NAMES) {
            changed |= applyConfigurationField(target, fieldName, environment);
        }
        return changed;
    }

    private static String describeConfigurationArgs(List<Object> args) {
        StringJoiner joiner = new StringJoiner(",");
        int index = 0;
        for (Object arg : args) {
            if (arg instanceof Configuration configuration) {
                joiner.add("#" + index + "="
                        + SystemServerDisplayDiagnostics.describeConfiguration(configuration));
            }
            index++;
        }
        return joiner.toString();
    }

    private static Set<ClassLoader> resolveCandidateClassLoaders() {
        Set<ClassLoader> classLoaders = new LinkedHashSet<>();
        classLoaders.add(Thread.currentThread().getContextClassLoader());
        classLoaders.add(SystemServerDisplayEnvironmentInstaller.class.getClassLoader());
        classLoaders.add(ClassLoader.getSystemClassLoader());
        classLoaders.add(null);
        return classLoaders;
    }

    private static Class<?> resolveClass(String className, ClassLoader classLoader) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean matchesAnyMethodName(Method method, String[] methodNames) {
        for (String methodName : methodNames) {
            if (methodName.equals(method.getName())) {
                return true;
            }
        }
        return false;
    }

    private static void logProbe(String entryName, String packageName, String originalSummary,
                                 String targetSummary, String actualSummary) {
        String message = SystemServerDisplayDiagnostics.buildProbeLog(
                entryName, packageName, originalSummary, targetSummary, actualSummary);
        String key = entryName + "|" + packageName;
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName));
    }

    private static void logPackageResolveMiss(String entryName, Object self, List<Object> args) {
        String selfClass = describeClassName(self);
        String argClasses = describeArgClasses(args, 6);
        String argPreview = describeArgPreview(args, 3);
        String textPackages = describeTextPackages(self, args, 4);
        String message = SystemServerDisplayDiagnostics.buildPackageResolveMissLog(
                entryName, selfClass, argClasses, argPreview, textPackages);
        // Keep unresolved log keys low-cardinality to avoid burst noise on hot paths.
        String key = "unresolved|" + entryName + "|" + selfClass;
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName));
    }

    private static void logConfigMiss(String entryName,
                                      String packageName,
                                      String candidatePackages,
                                      int configuredPackageCount) {
        String sourceState = "configuredPackages=" + configuredPackageCount
                + ", configNull=true";
        String message = SystemServerDisplayDiagnostics.buildConfigMissLog(
                entryName, packageName, candidatePackages, sourceState);
        // Candidate list may fluctuate frequently; key by entry+package for stable sampling.
        String key = "cfg-miss|" + entryName + "|" + packageName;
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName));
    }

    private static void logConfigFallback(String entryName,
                                          String fromPackageName,
                                          String selectedPackageName,
                                          String candidatePackages) {
        String message = SystemServerDisplayDiagnostics.buildConfigFallbackLog(
                entryName, fromPackageName, selectedPackageName, candidatePackages);
        String key = "cfg-fallback|" + entryName + "|" + fromPackageName + "|" + selectedPackageName;
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName));
    }

    private static void logEnvironmentNull(String entryName,
                                           String packageName,
                                           String beforeSummary,
                                           String afterSummary) {
        String message = "system_server environment null: entry=" + safeToString(entryName)
                + ", package=" + safeToString(packageName)
                + ", before=" + safeToString(beforeSummary)
                + ", after=" + safeToString(afterSummary);
        String key = "env-null|" + entryName + "|" + packageName;
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName));
    }

    private static void logApplySkipped(String entryName,
                                        String packageName,
                                        String stateSummary) {
        String message = "system_server apply skipped: entry=" + safeToString(entryName)
                + ", package=" + safeToString(packageName)
                + ", state=" + safeToString(stateSummary);
        String key = "apply-skipped|" + entryName + "|" + packageName;
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName));
    }

    private static void logTargetComputation(String entryName,
                                             String packageName,
                                             PerAppDisplayEnvironment preEnvironment,
                                             PerAppDisplayEnvironment postEnvironment,
                                             PerAppDisplayEnvironment effectiveEnvironment) {
        String message = "system_server target env: entry=" + safeToString(entryName)
                + ", package=" + safeToString(packageName)
                + ", pre=" + describeEnvironment(preEnvironment)
                + ", post=" + describeEnvironment(postEnvironment)
                + ", effective=" + describeEnvironment(effectiveEnvironment);
        String key = "target-env|" + entryName + "|" + packageName;
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName));
    }

    private static String describeEnvironment(PerAppDisplayEnvironment environment) {
        if (environment == null) {
            return "null";
        }
        return "wDp=" + environment.widthDp
                + ",hDp=" + environment.heightDp
                + ",swDp=" + environment.smallestWidthDp
                + ",dpi=" + environment.densityDpi
                + ",wPx=" + environment.widthPx
                + ",hPx=" + environment.heightPx;
    }

    private static void logInterceptEnter(String entryName, Object self, List<Object> args) {
        String selfClass = describeClassName(self);
        String argClasses = describeArgClasses(args, 6);
        String argPreview = describeArgPreview(args, 2);
        String message = SystemServerDisplayDiagnostics.buildInterceptEnterLog(
                entryName, selfClass, argClasses, argPreview);
        // Do not key by arg classes to avoid excessive cardinality.
        String key = "enter|" + entryName + "|" + selfClass;
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName));
    }

    private static void logIfChanged(String key, String message, long minIntervalMs) {
        SystemServerHookLogGate.logIfChanged(key, message, minIntervalMs);
    }

    private static long resolveLogMinIntervalMs(String entryName) {
        return SystemServerHookLogGate.resolveLogMinIntervalMs(entryName);
    }

    private static boolean shouldEmitLog(String previousMessage,
                                         String currentMessage,
                                         long nowMs,
                                         Long lastLogMs,
                                         long minIntervalMs) {
        return SystemServerHookLogGate.shouldEmitLog(
                previousMessage, currentMessage, nowMs, lastLogMs, minIntervalMs);
    }

    private static String describeClassName(Object value) {
        return value != null ? value.getClass().getName() : "null";
    }

    private static String describeArgClasses(List<Object> args, int maxArgs) {
        if (args == null || args.isEmpty()) {
            return "none";
        }
        StringJoiner joiner = new StringJoiner("|");
        int count = Math.min(args.size(), Math.max(maxArgs, 0));
        for (int i = 0; i < count; i++) {
            joiner.add(i + ":" + describeClassName(args.get(i)));
        }
        if (args.size() > count) {
            joiner.add("more=" + (args.size() - count));
        }
        return joiner.toString();
    }

    private static String describeArgPreview(List<Object> args, int maxArgs) {
        if (args == null || args.isEmpty()) {
            return "none";
        }
        StringJoiner joiner = new StringJoiner(" | ");
        int count = Math.min(args.size(), Math.max(maxArgs, 0));
        for (int i = 0; i < count; i++) {
            joiner.add(i + ":" + summarizeValue(args.get(i), 140));
        }
        if (args.size() > count) {
            joiner.add("more=" + (args.size() - count));
        }
        return joiner.toString();
    }

    private static String describeTextPackages(Object self, List<Object> args, int maxCount) {
        return describePackageCandidates(collectTextPackages(self, args, maxCount));
    }

    private static String describePackageCandidates(Set<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return "none";
        }
        return String.join("|", packages);
    }

    private static Set<String> collectTextPackages(Object self, List<Object> args, int maxCount) {
        Set<String> packages = new LinkedHashSet<>();
        if (maxCount <= 0) {
            return packages;
        }
        collectPackagesFromText(safeToString(self), packages, maxCount);
        if (args != null) {
            for (Object arg : args) {
                collectPackagesFromText(safeToString(arg), packages, maxCount);
                if (packages.size() >= maxCount) {
                    break;
                }
            }
        }
        return packages;
    }

    private static ResolvedPackage resolveConfiguredPackage(Object self,
                                                            List<Object> args,
                                                            ConfigLookup lookup) {
        String primaryPackage = findPackageName(self, args);
        Set<String> candidatePackages = new LinkedHashSet<>();
        if (primaryPackage != null) {
            candidatePackages.add(primaryPackage);
        }
        candidatePackages.addAll(collectTextPackages(self, args, 6));
        String candidateSummary = describePackageCandidates(candidatePackages);
        if (lookup == null) {
            return new ResolvedPackage(primaryPackage, null, candidateSummary, null);
        }
        if (primaryPackage != null) {
            PerAppDisplayConfig primaryConfig = lookup.find(primaryPackage);
            if (primaryConfig != null) {
                return new ResolvedPackage(primaryPackage, primaryConfig, candidateSummary, null);
            }
        }
        for (String candidate : candidatePackages) {
            if (Objects.equals(candidate, primaryPackage)) {
                continue;
            }
            PerAppDisplayConfig config = lookup.find(candidate);
            if (config != null) {
                return new ResolvedPackage(candidate, config, candidateSummary, primaryPackage);
            }
        }
        return new ResolvedPackage(primaryPackage, null, candidateSummary, null);
    }

    private static String summarizeValue(Object value, int maxLength) {
        String summary = safeToString(value).replace('\n', ' ').replace('\r', ' ');
        if (summary.length() <= maxLength) {
            return summary;
        }
        int clippedLength = Math.max(0, maxLength - 3);
        return summary.substring(0, clippedLength) + "...";
    }

    private static String safeToString(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return String.valueOf(value);
        } catch (Throwable throwable) {
            return "toString-failed:" + throwable.getClass().getName();
        }
    }

    private static String buildTargetSummary(PerAppDisplayEnvironment environment,
                                             Rect frame,
                                             PerAppDisplayConfig config) {
        if (environment == null || config == null) {
            return "targetWidthDp=" + (config != null ? config.targetViewportWidthDp() : -1);
        }
        return SystemServerDisplayDiagnostics.describeState(toConfiguration(environment), frame);
    }

    private static PerAppDisplayEnvironment resolveTargetEnvironment(String packageName,
                                                                     Snapshot before,
                                                                     Snapshot after,
                                                                     PerAppDisplayConfig config) {
        if (config == null || !hasSystemServerViewportOverride(config)) {
            return null;
        }
        Configuration configuration = after.configuration != null
                ? after.configuration : before.configuration;
        Rect frame = after.frame != null ? after.frame : before.frame;
        if (configuration == null) {
            return null;
        }
        int widthPx = resolveWidthPx(configuration, frame);
        int heightPx = resolveHeightPx(configuration, frame);
        PerAppDisplayEnvironment alreadyApplied = resolveAlreadyAppliedRelativeScaleEnvironment(
                packageName, configuration, widthPx, heightPx, config);
        if (alreadyApplied != null) {
            return alreadyApplied;
        }
        return PerAppDisplayOverrideCalculator.calculate(
                configuration, widthPx, heightPx, config.targetViewportSpec);
    }

    private static PerAppDisplayEnvironment resolveAlreadyAppliedRelativeScaleEnvironment(
            String packageName,
            Configuration configuration,
            int widthPx,
            int heightPx,
            PerAppDisplayConfig config) {
        if (packageName == null || configuration == null || config == null
                || !config.targetViewportSpec.isRelativeScale()) {
            return null;
        }
        String scope = ViewportConfigurationScope.isWindowScoped(configuration)
                ? ViewportSourceSnapshot.SCOPE_WINDOW
                : ViewportSourceSnapshot.SCOPE_DISPLAY;
        ViewportRuntimeMarkerBridge.ParseResult marker = ViewportRuntimeMarkerBridge.read(
                packageName,
                config.targetViewportSpec.fingerprint(),
                RuntimeClock.crossProcessMarkerMillis());
        if (!isAlreadyAppliedRelativeScaleMarker(configuration, scope, marker)) {
            return null;
        }
        return new PerAppDisplayEnvironment(
                configuration.screenWidthDp,
                configuration.screenHeightDp,
                configuration.smallestScreenWidthDp,
                configuration.densityDpi,
                widthPx,
                heightPx);
    }

    private static boolean isAlreadyAppliedRelativeScaleMarker(
            Configuration configuration,
            String scope,
            ViewportRuntimeMarkerBridge.ParseResult marker) {
        if (configuration == null || marker == null || !marker.hit || marker.record == null) {
            return false;
        }
        String sourceSignature = ViewportRuntimeMarkerBridge.configurationSignature(
                configuration.screenWidthDp,
                configuration.screenHeightDp,
                configuration.smallestScreenWidthDp,
                configuration.densityDpi,
                scope);
        return sourceSignature.equals(marker.record.resultSignature);
    }

    private static PerAppDisplayEnvironment resolveMarkerGatedEnvironment(
            String entryName,
            String packageName,
            Snapshot source,
            PerAppDisplayEnvironment environment,
            PerAppDisplayConfig config) {
        if (environment == null || config == null || !config.targetViewportSpec.isRelativeScale()) {
            return environment;
        }
        boolean published = publishViewportRuntimeMarker(
                packageName,
                source,
                environment,
                config);
        if (published) {
            return environment;
        }
        if (DpisLog.isLoggingEnabled()) {
            logIfChanged("marker-gate|" + entryName + "|" + packageName,
                    "system_server viewport skip: reason=marker-publish-failed"
                            + ", entry=" + entryName
                            + ", package=" + safeToString(packageName)
                            + ", target=" + config.targetViewportSpec,
                    resolveLogMinIntervalMs(entryName));
        }
        return null;
    }

    private static boolean publishViewportRuntimeMarker(String packageName,
                                                        Snapshot source,
                                                        PerAppDisplayEnvironment environment,
                                                        PerAppDisplayConfig config) {
        if (source == null || source.configuration == null
                || environment == null || config == null
                || !config.targetViewportSpec.isEnabled()) {
            return false;
        }
        if (config.targetViewportSpec.isRelativeScale()
                && ViewportConfigurationScope.isWindowScoped(source.configuration)) {
            return false;
        }
        return ViewportRuntimeMarkerBridge.publishSystemServerRecord(
                packageName,
                config.targetViewportSpec,
                new ConfigurationMarker(source.configuration),
                new EnvironmentMarker(environment),
                ViewportConfigurationScope.isWindowScoped(source.configuration)
                        ? ViewportSourceSnapshot.SCOPE_WINDOW
                        : ViewportSourceSnapshot.SCOPE_DISPLAY,
                RuntimeClock.crossProcessMarkerMillis());
    }

    private static boolean shouldInspectHotEntry(String entryName,
                                                 Object self,
                                                 List<Object> args,
                                                 Set<String> configuredPackages) {
        return SystemServerHotPathInspector.shouldInspectHotEntry(
                entryName, self, args, configuredPackages);
    }

    private static PerAppDisplayConfig selectConfigForSystemServer(
            PerAppDisplayConfig config) {
        return selectConfigForSystemServerEntry(null, config);
    }

    private static PerAppDisplayConfig selectConfigForSystemServerEntry(
            String entryName,
            PerAppDisplayConfig config) {
        if (config == null) {
            return null;
        }
        boolean applyViewport = hasSystemServerMutationForEntry(
                entryName, config, SystemServerMutationField.VIEWPORT);
        boolean applyFont = hasSystemServerMutationForEntry(
                entryName, config, SystemServerMutationField.FONT_SCALE);
        if (!applyViewport && !applyFont) {
            return null;
        }
        return config;
    }

    private static boolean hasSystemServerMutationForEntry(String entryName,
                                                           PerAppDisplayConfig config,
                                                           SystemServerMutationField field) {
        if (entryName != null
                && !SystemServerMutationPolicy.shouldApplyMutationField(entryName, field)) {
            return false;
        }
        return switch (field) {
            case VIEWPORT -> hasSystemServerViewportOverride(config);
            case FONT_SCALE -> hasSystemServerFontOverride(config);
        };
    }

    private static boolean hasSystemServerViewportOverride(PerAppDisplayConfig config) {
        if (config == null || !config.hasViewportOverride()) {
            return false;
        }
        String mode = ViewportApplyMode.normalize(config.targetViewportMode);
        if (ViewportApplyMode.SYSTEM.equals(mode)) {
            return true;
        }
        return ViewportApplyMode.AUTO.equals(mode)
                && !config.targetViewportSpec.isRelativeScale();
    }

    private static boolean hasSystemServerFontOverride(PerAppDisplayConfig config) {
        return config != null
                && FontApplyMode.SYSTEM_EMULATION.equals(config.targetFontMode)
                && config.targetFontScalePercent != null
                && config.targetFontScalePercent > 0;
    }

    static boolean hasSystemServerFontOverrideForTest(PerAppDisplayConfig config) {
        return hasSystemServerFontOverride(config);
    }

    private static void applyDisplayManagerInfoResult(PerAppDisplayConfigSource source,
                                                       String entryName,
                                                       Object displayInfo) {
        if (displayInfo == null || source == null) {
            logDisplayManagerInfoSkip(entryName, "empty-result", -1, null, displayInfo);
            return;
        }
        int callingUid = Binder.getCallingUid();
        String packageName = resolveCallingUidConfiguredPackage(source, callingUid);
        if (packageName == null) {
            logDisplayManagerInfoSkip(entryName, "uid-not-configured", callingUid, null, displayInfo);
            return;
        }
        PerAppDisplayConfig config = selectConfigForSystemServerEntry(
                entryName, source.get(packageName));
        if (config == null) {
            logDisplayManagerInfoSkip(entryName, "no-viewport-config", callingUid, packageName, displayInfo);
            return;
        }
        if (!hasSystemServerViewportOverride(config)) {
            logDisplayManagerInfoSkip(entryName, "no-viewport-mutation", callingUid, packageName, displayInfo);
            return;
        }
        PerAppDisplayEnvironment environment = resolveDisplayInfoEnvironment(displayInfo, config);
        if (environment == null) {
            logDisplayManagerInfoSkip(entryName, "env-null", callingUid, packageName, displayInfo);
            return;
        }
        String beforeSummary = DpisLog.isLoggingEnabled() ? describeDisplayInfo(displayInfo) : null;
        Snapshot sourceSnapshot = new Snapshot(
                configurationFromDisplayInfo(displayInfo),
                null,
                displayInfo);
        PerAppDisplayEnvironment applyEnvironment = resolveMarkerGatedEnvironment(
                entryName,
                packageName,
                sourceSnapshot,
                environment,
                config);
        if (!applyDisplayInfo(displayInfo, applyEnvironment)) {
            return;
        }
        if (DpisLog.isLoggingEnabled()) {
            String message = "system_server display-manager-info apply: package=" + packageName
                    + ", before=" + safeToString(beforeSummary)
                    + ", after=" + safeToString(describeDisplayInfo(displayInfo))
                    + ", target=" + describeEnvironment(environment);
            logIfChanged("display-manager-info|" + packageName, message,
                    resolveLogMinIntervalMs(entryName));
        }
    }

    private static void logDisplayManagerInfoSkip(String entryName,
                                                  String reason,
                                                  int callingUid,
                                                  String packageName,
                                                  Object displayInfo) {
        if (!DpisLog.isLoggingEnabled()) {
            return;
        }
        String message = "system_server display-manager-info skip: reason=" + reason
                + ", uid=" + callingUid
                + ", package=" + safeToString(packageName)
                + ", displayInfo=" + safeToString(describeDisplayInfo(displayInfo));
        logIfChanged("display-manager-info-skip|" + reason + "|" + callingUid,
                message, resolveLogMinIntervalMs(entryName));
    }

    private static String resolveCallingUidConfiguredPackage(PerAppDisplayConfigSource source,
                                                             int callingUid) {
        if (callingUid <= 0) {
            return null;
        }
        String fallbackPackage = null;
        for (String packageName : source.getConfiguredPackages()) {
            if (PACKAGE_UID_RESOLVER.resolve(packageName, callingUid) != callingUid) {
                continue;
            }
            if (selectConfigForSystemServerEntry(
                    "display-manager-info", source.get(packageName)) != null) {
                return packageName;
            }
            fallbackPackage = packageName;
        }
        return fallbackPackage;
    }

    private static PerAppDisplayEnvironment resolveDisplayInfoEnvironment(Object displayInfo,
                                                                          PerAppDisplayConfig config) {
        Configuration configuration = configurationFromDisplayInfo(displayInfo);
        if (configuration == null) {
            return null;
        }
        return PerAppDisplayOverrideCalculator.calculate(
                configuration,
                readIntField(displayInfo, "logicalWidth"),
                readIntField(displayInfo, "logicalHeight"),
                config.targetViewportSpec);
    }

    private static Configuration configurationFromDisplayInfo(Object displayInfo) {
        Integer logicalWidth = readIntField(displayInfo, "logicalWidth");
        Integer logicalHeight = readIntField(displayInfo, "logicalHeight");
        Integer logicalDensityDpi = readIntField(displayInfo, "logicalDensityDpi");
        if (logicalWidth == null || logicalHeight == null || logicalDensityDpi == null
                || logicalWidth <= 0 || logicalHeight <= 0 || logicalDensityDpi <= 0) {
            return null;
        }
        Configuration configuration = new Configuration();
        configuration.densityDpi = logicalDensityDpi;
        configuration.screenWidthDp = Math.max(1,
                Math.round(logicalWidth / (logicalDensityDpi / 160.0f)));
        configuration.screenHeightDp = Math.max(1,
                Math.round(logicalHeight / (logicalDensityDpi / 160.0f)));
        configuration.smallestScreenWidthDp = Math.min(
                configuration.screenWidthDp,
                configuration.screenHeightDp);
        return configuration;
    }

    private static PerAppDisplayEnvironment chooseEffectiveEnvironment(
            PerAppDisplayEnvironment preEnvironment,
            PerAppDisplayEnvironment postEnvironment) {
        return postEnvironment != null ? postEnvironment : preEnvironment;
    }

    static String selectEnvironmentSourceForTest(boolean hasPre, boolean hasPost) {
        if (hasPost) {
            return "post";
        }
        return hasPre ? "pre" : "none";
    }

    static boolean shouldInspectHotEntryForTest(String entryName,
                                                Object self,
                                                Set<String> configuredPackages) {
        return shouldInspectHotEntry(entryName, self, List.of(), configuredPackages);
    }

    static boolean shouldUseConfigInSystemServerForTest(PerAppDisplayConfig config) {
        return selectConfigForSystemServer(config) != null;
    }

    static boolean shouldUseConfigInSystemServerEntryForTest(String entryName,
                                                             PerAppDisplayConfig config) {
        return selectConfigForSystemServerEntry(entryName, config) != null;
    }

    static boolean isAlreadyAppliedRelativeScaleMarkerForTest(Configuration configuration,
                                                             String scope,
                                                             ViewportRuntimeMarkerBridge.ParseResult marker) {
        return isAlreadyAppliedRelativeScaleMarker(configuration, scope, marker);
    }

    static boolean shouldEmitLogForTest(String previousMessage,
                                        String currentMessage,
                                        long nowMs,
                                        Long lastLogMs,
                                        long minIntervalMs) {
        return shouldEmitLog(previousMessage, currentMessage, nowMs, lastLogMs, minIntervalMs);
    }

    private static boolean applyEnvironment(String entryName,
                                            Snapshot snapshot,
                                            PerAppDisplayEnvironment environment,
                                            PerAppDisplayConfig config) {
        boolean changed = false;
        // TODO(system-mutation-scheduler): give each field an explicit baseline
        // policy. VIEWPORT uses a marker-gated baseline model and can be applied
        // across multiple lifecycle entries; FONT_SCALE is launch-only here
        // because changing Configuration.fontScale during later config dispatch
        // can produce CONFIG_FONT_SCALE relaunches.
        boolean applyViewport = environment != null
                && SystemServerMutationPolicy.shouldApplyMutationField(
                        entryName, SystemServerMutationField.VIEWPORT)
                && hasSystemServerViewportOverride(config);
        if (snapshot.configuration != null && applyViewport) {
            changed |= applyConfiguration(snapshot.configuration, environment);
        }
        if (snapshot.configuration != null
                && SystemServerMutationPolicy.shouldApplyMutationField(
                        entryName, SystemServerMutationField.FONT_SCALE)) {
            boolean fontChanged = applyFontScale(snapshot.configuration, config);
            if (fontChanged) {
                HyperOsFlutterFontBridge.publishTarget(config.packageName, config);
            }
            changed |= fontChanged;
        }
        if (applyViewport && shouldApplyFrame(entryName) && snapshot.frame != null) {
            changed |= applyFrame(snapshot.frame, environment.widthPx, environment.heightPx);
        }
        if (applyViewport && shouldApplyDisplayInfo(entryName) && snapshot.displayInfo != null) {
            changed |= applyDisplayInfo(snapshot.displayInfo, environment);
        }
        return changed;
    }

    private static boolean shouldApplyFrame(String entryName) {
        return SystemServerEntryRoute.isRelayoutDispatch(entryName)
                || SystemServerEntryRoute.isDisplayPolicyLayout(entryName);
    }

    private static boolean shouldApplyDisplayInfo(String entryName) {
        return SystemServerEntryRoute.isDisplayContentConfig(entryName);
    }

    private static boolean applyConfiguration(Configuration configuration,
                                              PerAppDisplayEnvironment environment) {
        boolean changed = configuration.screenWidthDp != environment.widthDp
                || configuration.screenHeightDp != environment.heightDp
                || configuration.smallestScreenWidthDp != environment.smallestWidthDp
                || configuration.densityDpi != environment.densityDpi;
        if (!changed) {
            return false;
        }
        ViewportOverride.apply(configuration, new ViewportOverride.Result(
                environment.widthDp,
                environment.heightDp,
                environment.smallestWidthDp,
                environment.densityDpi));
        return true;
    }

    private static boolean applyFontScale(Configuration configuration,
                                          PerAppDisplayConfig config) {
        if (configuration == null || !hasSystemServerFontOverride(config)) {
            return false;
        }
        if (isSystemServerFontDisabledByDebugOverride(config.packageName)) {
            DpisLog.i("DPIS_FONT SystemServer config fontScale skipped: package="
                    + config.packageName + ", reason=debug-disable-system-server-font");
            return false;
        }
        if (shouldYieldSystemServerFontToAppProcessFallback(config)) {
            DpisLog.i("DPIS_FONT SystemServer config fontScale skipped: package="
                    + config.packageName + ", reason=debug-system-server-font-fallback-yield");
            return false;
        }
        float fontScale = config.targetFontScalePercent / 100.0f;
        if (Math.abs(configuration.fontScale - fontScale) < 0.0001f) {
            return false;
        }
        configuration.fontScale = fontScale;
        return true;
    }

    static boolean isSystemServerFontDisabledByDebugOverrideForTest(String packageName,
                                                                    String propertyValue) {
        return DebugPackageOverride.matchesForTest(
                PROP_DISABLE_SYSTEM_SERVER_FONT_PACKAGE,
                packageName,
                propertyValue);
    }

    static boolean shouldYieldSystemServerFontToAppProcessFallbackForTest(PerAppDisplayConfig config,
                                                                         String propertyValue) {
        return isSystemServerFontFallbackEnabledForPackage(config.packageName, propertyValue)
                && hasAppProcessSystemFontEmulationRoute(config);
    }

    private static boolean isSystemServerFontDisabledByDebugOverride(String packageName) {
        return DebugPackageOverride.matches(PROP_DISABLE_SYSTEM_SERVER_FONT_PACKAGE, packageName);
    }

    private static boolean shouldYieldSystemServerFontToAppProcessFallback(
            PerAppDisplayConfig config) {
        return isSystemServerFontFallbackEnabledForPackage(config.packageName)
                && hasAppProcessSystemFontEmulationRoute(config);
    }

    private static boolean isSystemServerFontFallbackEnabledForPackage(String packageName) {
        return DebugPackageOverride.matches(PROP_SYSTEM_SERVER_FONT_FALLBACK_PACKAGE, packageName);
    }

    private static boolean isSystemServerFontFallbackEnabledForPackage(String packageName,
                                                                       String propertyValue) {
        return DebugPackageOverride.matchesForTest(
                PROP_SYSTEM_SERVER_FONT_FALLBACK_PACKAGE,
                packageName,
                propertyValue);
    }

    private static boolean hasAppProcessSystemFontEmulationRoute(PerAppDisplayConfig config) {
        return config != null
                && FontApplyMode.SYSTEM_EMULATION.equals(config.targetFontMode)
                && config.targetFontScalePercent != null
                && config.targetFontScalePercent > 0;
    }

    private static void reportSystemServerFontConfig(String packageName,
                                                     float beforeFontScale,
                                                     float afterFontScale) {
        if (packageName == null || packageName.isEmpty()) {
            return;
        }
        DpisLog.i("DPIS_FONT SystemServer config fontScale: package=" + packageName
                + ", fontScale=" + beforeFontScale + "->" + afterFontScale);
    }

    private static boolean applyFrame(Rect frame, int targetWidthPx, int targetHeightPx) {
        if (targetWidthPx <= 0 || targetHeightPx <= 0) {
            return false;
        }
        int beforeWidth = frame.width();
        int beforeHeight = frame.height();
        if (beforeWidth == targetWidthPx && beforeHeight == targetHeightPx) {
            return false;
        }
        WindowFrameOverride.apply(frame, targetWidthPx, targetHeightPx);
        return true;
    }

    private static boolean applyDisplayInfo(Object displayInfo, PerAppDisplayEnvironment environment) {
        boolean changed = false;
        if (environment.widthPx > 0) {
            changed |= writeIntField(displayInfo, "logicalWidth", environment.widthPx);
        }
        if (environment.heightPx > 0) {
            changed |= writeIntField(displayInfo, "logicalHeight", environment.heightPx);
        }
        changed |= writeIntField(displayInfo, "logicalDensityDpi", environment.densityDpi);
        return changed;
    }

    private static int resolveWidthPx(Configuration configuration, Rect frame) {
        if (frame != null && frame.width() > 0) {
            return frame.width();
        }
        if (configuration == null || configuration.screenWidthDp <= 0 || configuration.densityDpi <= 0) {
            return 0;
        }
        return Math.round(configuration.screenWidthDp * (configuration.densityDpi / 160.0f));
    }

    private static int resolveHeightPx(Configuration configuration, Rect frame) {
        if (frame != null && frame.height() > 0) {
            return frame.height();
        }
        if (configuration == null || configuration.screenHeightDp <= 0 || configuration.densityDpi <= 0) {
            return 0;
        }
        return Math.round(configuration.screenHeightDp * (configuration.densityDpi / 160.0f));
    }

    private static Configuration toConfiguration(PerAppDisplayEnvironment environment) {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = environment.widthDp;
        configuration.screenHeightDp = environment.heightDp;
        configuration.smallestScreenWidthDp = environment.smallestWidthDp;
        configuration.densityDpi = environment.densityDpi;
        return configuration;
    }

    private static Snapshot captureSnapshot(Object self, List<Object> args) {
        return new Snapshot(findConfiguration(self, args), findFrame(self, args),
                findDisplayInfo(self, args));
    }

    private static String findPackageName(Object self, List<Object> args) {
        String packageName = findPackageNameRecursive(self, 0);
        if (packageName != null) {
            return packageName;
        }
        for (Object arg : args) {
            packageName = findPackageNameRecursive(arg, 0);
            if (packageName != null) {
                return packageName;
            }
        }
        return null;
    }

    private static String findPackageNameRecursive(Object target, int depth) {
        if (target == null || depth > MAX_PACKAGE_RECURSION_DEPTH) {
            return null;
        }
        if (target instanceof String value && isLikelyPackageName(value)) {
            return value;
        }
        for (String methodName : PACKAGE_STRING_METHOD_NAMES) {
            String fromMethod = invokeStringMethod(target, methodName);
            if (fromMethod != null) {
                return fromMethod;
            }
        }
        for (String methodName : PACKAGE_OBJECT_METHOD_NAMES) {
            Object value = invokeObjectMethod(target, methodName);
            String nestedPackage = findPackageNameRecursive(value, depth + 1);
            if (nestedPackage != null) {
                return nestedPackage;
            }
        }
        for (String fieldName : PACKAGE_STRING_FIELD_NAMES) {
            Object value = readField(target, fieldName);
            if (value instanceof String stringValue && isLikelyPackageName(stringValue)) {
                return stringValue;
            }
        }
        for (String fieldName : PACKAGE_OBJECT_FIELD_NAMES) {
            String nestedPackage = findPackageNameRecursive(readField(target, fieldName), depth + 1);
            if (nestedPackage != null) {
                return nestedPackage;
            }
        }
        for (Field field : getAllFields(target.getClass())) {
            if (field.getType().isPrimitive() || field.getType().isEnum()) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object nested = field.get(target);
                if (nested == null || nested == target || Objects.equals(field.getName(), "this$0")) {
                    continue;
                }
                String value = findPackageNameRecursive(nested, depth + 1);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException ignored) {
                // Continue probing.
            }
        }
        String fallback = extractPackageFromText(String.valueOf(target));
        if (fallback != null) {
            return fallback;
        }
        return null;
    }

    private static Configuration findConfiguration(Object self, List<Object> args) {
        Configuration configuration = findConfigurationRecursive(self, 0);
        if (configuration != null) {
            return configuration;
        }
        for (Object arg : args) {
            configuration = findConfigurationRecursive(arg, 0);
            if (configuration != null) {
                return configuration;
            }
        }
        return null;
    }

    private static Configuration findConfigurationRecursive(Object target, int depth) {
        if (target == null || depth > 3) {
            return null;
        }
        if (target instanceof Configuration configuration) {
            return configuration;
        }
        for (String fieldName : CONFIGURATION_FIELD_NAMES) {
            Object value = readField(target, fieldName);
            if (value instanceof Configuration configuration) {
                return configuration;
            }
            Configuration nestedConfiguration = findConfigurationRecursive(value, depth + 1);
            if (nestedConfiguration != null) {
                return nestedConfiguration;
            }
        }
        Configuration mergedConfiguration = invokeConfigurationMethod(target, "getMergedConfiguration");
        if (mergedConfiguration != null) {
            return mergedConfiguration;
        }
        Configuration fromMethod = invokeConfigurationMethod(target, "getConfiguration");
        if (fromMethod != null) {
            return fromMethod;
        }
        for (Field field : getAllFields(target.getClass())) {
            if (field.getType().isPrimitive() || field.getType().isEnum()) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object nested = field.get(target);
                if (nested == null || nested == target || Objects.equals(field.getName(), "this$0")) {
                    continue;
                }
                Configuration value = findConfigurationRecursive(nested, depth + 1);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException ignored) {
                // Continue probing.
            }
        }
        return null;
    }

    private static Rect findFrame(Object self, List<Object> args) {
        Rect frame = findFrameRecursive(self, 0);
        if (frame != null) {
            return frame;
        }
        for (Object arg : args) {
            frame = findFrameRecursive(arg, 0);
            if (frame != null) {
                return frame;
            }
        }
        return null;
    }

    private static Object findDisplayInfo(Object self, List<Object> args) {
        Object displayInfo = findDisplayInfoRecursive(self, 0);
        if (displayInfo != null) {
            return displayInfo;
        }
        for (Object arg : args) {
            displayInfo = findDisplayInfoRecursive(arg, 0);
            if (displayInfo != null) {
                return displayInfo;
            }
        }
        return null;
    }

    private static Object findDisplayInfoRecursive(Object target, int depth) {
        if (target == null || depth > 3) {
            return null;
        }
        if ("android.view.DisplayInfo".equals(target.getClass().getName())) {
            return target;
        }
        for (String fieldName : DISPLAY_INFO_FIELD_NAMES) {
            Object value = readField(target, fieldName);
            if (value != null && "android.view.DisplayInfo".equals(value.getClass().getName())) {
                return value;
            }
            Object nested = findDisplayInfoRecursive(value, depth + 1);
            if (nested != null) {
                return nested;
            }
        }
        Object fromMethod = invokeObjectMethod(target, "getDisplayInfo");
        if (fromMethod != null && "android.view.DisplayInfo".equals(fromMethod.getClass().getName())) {
            return fromMethod;
        }
        for (Field field : getAllFields(target.getClass())) {
            if (field.getType().isPrimitive() || field.getType().isEnum()) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object nested = field.get(target);
                if (nested == null || nested == target || Objects.equals(field.getName(), "this$0")) {
                    continue;
                }
                Object value = findDisplayInfoRecursive(nested, depth + 1);
                if (value != null) {
                    return value;
                }
            } catch (ReflectiveOperationException ignored) {
                // Continue probing.
            }
        }
        return null;
    }

    private static Rect findFrameRecursive(Object target, int depth) {
        if (target == null || depth > 4) {
            return null;
        }
        if (target instanceof Rect rect) {
            return rect;
        }
        for (String fieldName : FRAME_DIRECT_FIELD_NAMES) {
            Object value = readField(target, fieldName);
            if (value instanceof Rect rect) {
                return rect;
            }
        }
        for (String fieldName : FRAME_NESTED_FIELD_NAMES) {
            Object nested = readField(target, fieldName);
            Rect rect = findFrameRecursive(nested, depth + 1);
            if (rect != null) {
                return rect;
            }
        }
        for (Field field : getAllFields(target.getClass())) {
            if (field.getType().isPrimitive() || field.getType().isEnum()) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object nested = field.get(target);
                if (nested == null || nested == target || Objects.equals(field.getName(), "this$0")) {
                    continue;
                }
                Rect rect = findFrameRecursive(nested, depth + 1);
                if (rect != null) {
                    return rect;
                }
            } catch (ReflectiveOperationException ignored) {
                // Continue probing.
            }
        }
        return null;
    }

    private static String invokeStringMethod(Object target, String methodName) {
        try {
            Method method = REFLECTION_CACHE.findNoArgMethod(target.getClass(), methodName);
            if (method == null) {
                return null;
            }
            Object value = method.invoke(target);
            return value instanceof String stringValue ? stringValue : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Configuration invokeConfigurationMethod(Object target, String methodName) {
        try {
            Method method = REFLECTION_CACHE.findNoArgMethod(target.getClass(), methodName);
            if (method == null) {
                return null;
            }
            Object value = method.invoke(target);
            return value instanceof Configuration configuration ? configuration : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object invokeObjectMethod(Object target, String methodName) {
        try {
            Method method = REFLECTION_CACHE.findNoArgMethod(target.getClass(), methodName);
            if (method == null) {
                return null;
            }
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object readField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        Field field = REFLECTION_CACHE.findField(target.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        return REFLECTION_CACHE.getAllFields(clazz);
    }

    private static String extractPackageFromText(String value) {
        Set<String> candidates = new LinkedHashSet<>();
        collectPackagesFromText(value, candidates, 1);
        for (String candidate : candidates) {
            return candidate;
        }
        return null;
    }

    private static void collectPackagesFromText(String value, Set<String> output, int maxCount) {
        if (value == null || value.isEmpty() || output == null || maxCount <= 0) {
            return;
        }
        int index = value.indexOf("com.");
        while (index >= 0 && index < value.length()) {
            int end = index;
            while (end < value.length()) {
                char c = value.charAt(end);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                    end++;
                    continue;
                }
                break;
            }
            if (end > index) {
                String candidate = value.substring(index, end);
                if (isLikelyPackageName(candidate)) {
                    output.add(candidate);
                    if (output.size() >= maxCount) {
                        return;
                    }
                }
            }
            index = value.indexOf("com.", index + 4);
        }
    }

    private static boolean isLikelyPackageName(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()
                || trimmed.length() > 256
                || !trimmed.contains(".")
                || trimmed.contains(" ")
                || trimmed.contains("/")
                || trimmed.contains("{")
                || trimmed.contains("}")) {
            return false;
        }
        return Character.isLowerCase(trimmed.charAt(0));
    }

    private static final class Snapshot {
        final Configuration configuration;
        final Rect frame;
        final Object displayInfo;

        Snapshot(Configuration configuration, Rect frame, Object displayInfo) {
            this.configuration = configuration;
            this.frame = frame;
            this.displayInfo = displayInfo;
        }
    }

    private static final class ConfigurationMarker implements ViewportRuntimeMarkerBridge.ConfigurationLike {
        private final Configuration configuration;

        ConfigurationMarker(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public int widthDp() {
            return configuration.screenWidthDp;
        }

        @Override
        public int heightDp() {
            return configuration.screenHeightDp;
        }

        @Override
        public int smallestWidthDp() {
            return configuration.smallestScreenWidthDp;
        }

        @Override
        public int densityDpi() {
            return configuration.densityDpi;
        }
    }

    private static final class EnvironmentMarker implements ViewportRuntimeMarkerBridge.ConfigurationLike {
        private final PerAppDisplayEnvironment environment;

        EnvironmentMarker(PerAppDisplayEnvironment environment) {
            this.environment = environment;
        }

        @Override
        public int widthDp() {
            return environment.widthDp;
        }

        @Override
        public int heightDp() {
            return environment.heightDp;
        }

        @Override
        public int smallestWidthDp() {
            return environment.smallestWidthDp;
        }

        @Override
        public int densityDpi() {
            return environment.densityDpi;
        }
    }

    private interface ConfigLookup {
        PerAppDisplayConfig find(String packageName);
    }

    private static final class ResolvedPackage {
        final String packageName;
        final PerAppDisplayConfig config;
        final String candidatePackagesSummary;
        final String fallbackFromPackage;

        ResolvedPackage(String packageName,
                        PerAppDisplayConfig config,
                        String candidatePackagesSummary,
                        String fallbackFromPackage) {
            this.packageName = packageName;
            this.config = config;
            this.candidatePackagesSummary = candidatePackagesSummary;
            this.fallbackFromPackage = fallbackFromPackage;
        }
    }

    private static void logDisplayInfoProbe(String entryName, String packageName, Object displayInfo,
                                            Rect frame, Configuration configuration) {
        String displayInfoSummary = describeDisplayInfo(displayInfo);
        if (displayInfoSummary == null) {
            return;
        }
        String message = SystemServerDisplayDiagnostics.buildDisplayInfoProbeLog(
                entryName,
                packageName,
                displayInfoSummary,
                SystemServerDisplayDiagnostics.describeFrame(frame),
                SystemServerDisplayDiagnostics.describeConfiguration(configuration));
        String key = "display|" + entryName + "|" + packageName;
        logIfChanged(key, message, resolveLogMinIntervalMs(entryName));
    }

    private static String describeDisplayInfo(Object displayInfo) {
        if (displayInfo == null) {
            return null;
        }
        Integer logicalWidth = readIntField(displayInfo, "logicalWidth");
        Integer logicalHeight = readIntField(displayInfo, "logicalHeight");
        Integer logicalDensityDpi = readIntField(displayInfo, "logicalDensityDpi");
        if (logicalWidth == null && logicalHeight == null && logicalDensityDpi == null) {
            return displayInfo.getClass().getName();
        }
        return "displayInfo{logicalWidth=" + safeInt(logicalWidth)
                + ",logicalHeight=" + safeInt(logicalHeight)
                + ",logicalDensityDpi=" + safeInt(logicalDensityDpi) + "}";
    }

    private static Integer readIntField(Object target, String fieldName) {
        Object value = readField(target, fieldName);
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        return null;
    }

    private static boolean writeIntField(Object target, String fieldName, int value) {
        if (target == null) {
            return false;
        }
        Field field = resolveField(target.getClass(), fieldName);
        if (field == null) {
            return false;
        }
        try {
            field.setAccessible(true);
            int previous = field.getInt(target);
            if (previous == value) {
                return false;
            }
            field.setInt(target, value);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Field resolveField(Class<?> clazz, String fieldName) {
        return REFLECTION_CACHE.findField(clazz, fieldName);
    }

    private static int safeInt(Integer value) {
        return value != null ? value : -1;
    }
}
