package com.dpis.module;

import android.content.res.Configuration;
import android.graphics.Rect;

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
    private static final int MAX_PACKAGE_RECURSION_DEPTH = 5;
    private static final HookTarget[] HOOK_TARGETS = new HookTarget[]{
            new HookTarget("config-dispatch",
                    new String[]{
                            "com.android.server.wm.ActivityRecord"
                    },
                    new String[]{
                            "updateReportedConfigurationAndSend"
                    }),
            new HookTarget("activity-start",
                    new String[]{
                            "com.android.server.wm.ActivityStarter",
                            "com.android.server.am.ActivityStarter"
                    },
                    new String[]{
                            "execute",
                            "startActivityMayWait"
                    }),
            new HookTarget("relayout-dispatch",
                    new String[]{
                            "com.android.server.wm.WindowManagerService"
                    },
                    new String[]{
                            "relayoutWindow"
                    }),
            new HookTarget("display-policy-layout",
                    new String[]{
                            "com.android.server.wm.DisplayPolicy"
                    },
                    new String[]{
                            "layoutWindowLw"
                    }),
            new HookTarget("display-content-config",
                    new String[]{
                            "com.android.server.wm.DisplayContent"
                    },
                    new String[]{
                            "computeScreenConfiguration",
                            "updateDisplayAndOrientation",
                            "getDisplayInfo"
                    })
    };
    private static volatile boolean installed;

    private SystemServerDisplayEnvironmentInstaller() {
    }

    static void install(XposedInterface xposed, DpiConfigStore store) {
        if (installed) {
            SystemServerDisplayDiagnostics.recordPending("system_server install skipped: reason=already-installed-fast-path");
            DpisLog.i(SystemServerDisplayDiagnostics.buildInstallSkipLog("already-installed-fast-path"));
            return;
        }
        synchronized (SystemServerDisplayEnvironmentInstaller.class) {
            if (installed) {
                SystemServerDisplayDiagnostics.recordPending("system_server install skipped: reason=already-installed-synchronized");
                DpisLog.i(SystemServerDisplayDiagnostics.buildInstallSkipLog("already-installed-synchronized"));
                return;
            }
            SystemServerDisplayDiagnostics.recordPending(
                    SystemServerDisplayDiagnostics.buildInstallEnterLog(store == null, installed));
            DpisLog.i(SystemServerDisplayDiagnostics.buildInstallEnterLog(
                    store == null, installed));
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
                    installed = true;
                    return;
                }
                PerAppDisplayConfigSource source = new PerAppDisplayConfigSource(store);
                Set<String> configuredPackages = source.getConfiguredPackages();
                int installedCount = 0;
                int missingCount = 0;
                for (HookTarget target : HOOK_TARGETS) {
                    if (!SystemServerMutationPolicy.shouldInstallTarget(
                            target.entryName, policy.systemServerSafeModeEnabled)) {
                        continue;
                    }
                    if (installTargetHooks(xposed, source, target, configuredPackages)) {
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
                installed = true;
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
                                              HookTarget target,
                                              Set<String> configuredPackages) {
        boolean hooked = false;
        for (ClassLoader classLoader : resolveCandidateClassLoaders()) {
            for (String className : target.classNames) {
                Class<?> clazz = resolveClass(className, classLoader);
                if (clazz == null) {
                    continue;
                }
                for (Method method : clazz.getDeclaredMethods()) {
                    if (!matchesAnyMethodName(method, target.methodNames)
                            || Modifier.isAbstract(method.getModifiers())) {
                        continue;
                    }
                    xposed.hook(method)
                            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                            .intercept(chain -> {
                                Object result = null;
                                boolean proceeded = false;
                                try {
                                    Object thisObject = chain.getThisObject();
                                    List<Object> args = chain.getArgs();
                                    boolean loggingEnabled = DpisLog.isLoggingEnabled();
                                    if (!shouldInspectHotEntry(
                                            target.entryName,
                                            thisObject,
                                            args,
                                            configuredPackages)) {
                                        return chain.proceed();
                                    }
                                    if (loggingEnabled && shouldLogInterceptEnter(target.entryName)) {
                                        logInterceptEnter(target.entryName, thisObject, args);
                                    }
                                    ResolvedPackage resolvedPackage = resolveConfiguredPackage(
                                            thisObject,
                                            args,
                                            packageName -> selectViewportConfigForSystemServer(
                                                    source.get(packageName)));
                                    if (resolvedPackage.packageName == null) {
                                        if (loggingEnabled) {
                                            logPackageResolveMiss(target.entryName, thisObject, args);
                                        }
                                        return chain.proceed();
                                    }
                                    String packageName = resolvedPackage.packageName;
                                    PerAppDisplayConfig config = resolvedPackage.config;
                                    if (config == null) {
                                        if (loggingEnabled) {
                                            logConfigMiss(target.entryName, packageName,
                                                    resolvedPackage.candidatePackagesSummary);
                                        }
                                        return chain.proceed();
                                    }
                                    if (loggingEnabled && resolvedPackage.fallbackFromPackage != null) {
                                        logConfigFallback(target.entryName,
                                                resolvedPackage.fallbackFromPackage,
                                                packageName,
                                                resolvedPackage.candidatePackagesSummary);
                                    }
                                    Snapshot before = captureSnapshot(thisObject, args);
                                    PerAppDisplayEnvironment preEnvironment = resolveTargetEnvironment(
                                            before, before, config);
                                    if (preEnvironment != null
                                            && shouldApplyPreProceedMutations(target.entryName)) {
                                        applyEnvironment(target.entryName, before, preEnvironment);
                                    }
                                    result = chain.proceed();
                                    proceeded = true;
                                    Snapshot after = captureSnapshot(thisObject, args);
                                    PerAppDisplayEnvironment environment = resolveTargetEnvironment(
                                            before, after, config);
                                    PerAppDisplayEnvironment effectiveEnvironment = chooseEffectiveEnvironment(
                                            preEnvironment, environment);
                                    if (loggingEnabled) {
                                        logTargetComputation(target.entryName, packageName,
                                                preEnvironment, environment, effectiveEnvironment);
                                    }
                                    Snapshot mutated = after;
                                    if (effectiveEnvironment == null) {
                                        if (loggingEnabled) {
                                            logEnvironmentNull(target.entryName,
                                                    packageName,
                                                    SystemServerDisplayDiagnostics.describeState(
                                                            before.configuration, before.frame),
                                                    SystemServerDisplayDiagnostics.describeState(
                                                            after.configuration, after.frame));
                                        }
                                    }
                                    if (effectiveEnvironment != null && shouldApplyPostProceedMutations(target.entryName)) {
                                        String beforeApplySummary = loggingEnabled
                                                ? SystemServerDisplayDiagnostics.describeState(
                                                after.configuration, after.frame)
                                                : null;
                                        if (applyEnvironment(target.entryName, after, effectiveEnvironment)) {
                                            if (loggingEnabled) {
                                                String afterApplySummary = SystemServerDisplayDiagnostics.describeState(
                                                        mutated.configuration, mutated.frame);
                                                String message = SystemServerDisplayDiagnostics.buildApplyLog(
                                                        target.entryName,
                                                        packageName,
                                                        beforeApplySummary,
                                                        afterApplySummary);
                                                String key = "apply|" + target.entryName + "|" + packageName;
                                                logIfChanged(
                                                        key,
                                                        message,
                                                        resolveLogMinIntervalMs(target.entryName));
                                            }
                                        } else {
                                            if (loggingEnabled) {
                                                logApplySkipped(target.entryName, packageName, beforeApplySummary);
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
                                        logProbe(target.entryName, packageName, originalSummary,
                                                targetSummary, actualSummary);
                                        logDisplayInfoProbe(target.entryName, packageName,
                                                mutated.displayInfo, mutated.frame, mutated.configuration);
                                    }
                                    return result;
                                } catch (Throwable throwable) {
                                    DpisLog.e(SystemServerDisplayDiagnostics.buildInterceptErrorLog(
                                            target.entryName, throwable), throwable);
                                    if (proceeded) {
                                        return result;
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
                            target.entryName, target.describeClassNames(), target.describeMethodNames()));
            DpisLog.i(SystemServerDisplayDiagnostics.buildHookReadyLog(
                    target.entryName, target.describeClassNames(), target.describeMethodNames()));
            return true;
        }
        SystemServerDisplayDiagnostics.recordPending(
                SystemServerDisplayDiagnostics.buildHookMissingLog(
                        target.entryName, target.describeClassNames(), target.describeMethodNames()));
        DpisLog.i(SystemServerDisplayDiagnostics.buildHookMissingLog(
                target.entryName, target.describeClassNames(), target.describeMethodNames()));
        return false;
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
                                      String candidatePackages) {
        String message = SystemServerDisplayDiagnostics.buildConfigMissLog(
                entryName, packageName, candidatePackages);
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
            return "targetWidthDp=" + (config != null ? config.targetViewportWidthDp : -1);
        }
        return SystemServerDisplayDiagnostics.describeState(toConfiguration(environment), frame);
    }

    private static PerAppDisplayEnvironment resolveTargetEnvironment(Snapshot before,
                                                                     Snapshot after,
                                                                     PerAppDisplayConfig config) {
        if (config == null) {
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
        return PerAppDisplayOverrideCalculator.calculate(
                configuration, widthPx, heightPx, config.targetViewportWidthDp);
    }

    private static boolean shouldApplyPreProceedMutations(String entryName) {
        return SystemServerMutationPolicy.shouldApplyPreProceedMutations(entryName);
    }

    private static boolean shouldApplyPostProceedMutations(String entryName) {
        return SystemServerMutationPolicy.shouldApplyPostProceedMutations(entryName);
    }

    private static boolean shouldLogInterceptEnter(String entryName) {
        return SystemServerHookLogGate.shouldLogInterceptEnter(entryName);
    }

    private static boolean shouldInstallTarget(String entryName, boolean safeModeEnabled) {
        return SystemServerMutationPolicy.shouldInstallTarget(entryName, safeModeEnabled);
    }

    private static boolean isHotEntry(String entryName) {
        return SystemServerHookLogGate.isHotEntry(entryName);
    }

    private static boolean shouldInspectHotEntry(String entryName,
                                                 Object self,
                                                 List<Object> args,
                                                 Set<String> configuredPackages) {
        return SystemServerHotPathInspector.shouldInspectHotEntry(
                entryName, self, args, configuredPackages);
    }

    private static PerAppDisplayConfig selectViewportConfigForSystemServer(
            PerAppDisplayConfig config) {
        if (config == null || !config.hasViewportOverride()) {
            return null;
        }
        return config;
    }

    private static PerAppDisplayEnvironment chooseEffectiveEnvironment(
            PerAppDisplayEnvironment preEnvironment,
            PerAppDisplayEnvironment postEnvironment) {
        return postEnvironment != null ? postEnvironment : preEnvironment;
    }

    static boolean shouldApplyPreProceedMutationsForTest(String entryName) {
        return shouldApplyPreProceedMutations(entryName);
    }

    static boolean shouldApplyPostProceedMutationsForTest(String entryName) {
        return shouldApplyPostProceedMutations(entryName);
    }

    static String selectEnvironmentSourceForTest(boolean hasPre, boolean hasPost) {
        if (hasPost) {
            return "post";
        }
        return hasPre ? "pre" : "none";
    }

    static boolean shouldLogInterceptEnterForTest(String entryName) {
        return shouldLogInterceptEnter(entryName);
    }

    static boolean shouldInspectHotEntryForTest(String entryName,
                                                Object self,
                                                Set<String> configuredPackages) {
        return shouldInspectHotEntry(entryName, self, List.of(), configuredPackages);
    }

    static boolean shouldUseConfigInSystemServerForTest(PerAppDisplayConfig config) {
        return selectViewportConfigForSystemServer(config) != null;
    }

    static boolean shouldEmitLogForTest(String previousMessage,
                                        String currentMessage,
                                        long nowMs,
                                        Long lastLogMs,
                                        long minIntervalMs) {
        return shouldEmitLog(previousMessage, currentMessage, nowMs, lastLogMs, minIntervalMs);
    }

    static boolean shouldInstallTargetForTest(String entryName, boolean safeModeEnabled) {
        return shouldInstallTarget(entryName, safeModeEnabled);
    }

    private static boolean applyEnvironment(String entryName,
                                            Snapshot snapshot,
                                            PerAppDisplayEnvironment environment) {
        boolean changed = false;
        if (snapshot.configuration != null) {
            changed |= applyConfiguration(snapshot.configuration, environment);
        }
        if (shouldApplyFrame(entryName) && snapshot.frame != null) {
            changed |= applyFrame(snapshot.frame, environment.widthPx, environment.heightPx);
        }
        if (shouldApplyDisplayInfo(entryName) && snapshot.displayInfo != null) {
            changed |= applyDisplayInfo(snapshot.displayInfo, environment);
        }
        return changed;
    }

    private static boolean shouldApplyFrame(String entryName) {
        return "relayout-dispatch".equals(entryName)
                || "display-policy-layout".equals(entryName);
    }

    private static boolean shouldApplyDisplayInfo(String entryName) {
        return "display-content-config".equals(entryName);
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
        for (String methodName : new String[]{
                "getOwningPackage",
                "getPackageName",
                "getPackage",
                "getOpPackageName"}) {
            String fromMethod = invokeStringMethod(target, methodName);
            if (fromMethod != null) {
                return fromMethod;
            }
        }
        for (String methodName : new String[]{
                "getIntent",
                "getComponent",
                "getActivityInfo",
                "getApplicationInfo",
                "getRequest",
                "getTargetActivity",
                "getOrigActivity",
                "getRealActivity"}) {
            Object value = invokeObjectMethod(target, methodName);
            String nestedPackage = findPackageNameRecursive(value, depth + 1);
            if (nestedPackage != null) {
                return nestedPackage;
            }
        }
        for (String fieldName : new String[]{
                "packageName", "mPackageName", "package", "launchedFromPackage"}) {
            Object value = readField(target, fieldName);
            if (value instanceof String stringValue && isLikelyPackageName(stringValue)) {
                return stringValue;
            }
        }
        for (String fieldName : new String[]{
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
                "realActivity"}) {
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
        for (String fieldName : new String[]{
                "mergedConfiguration",
                "mLastReportedConfiguration",
                "mTmpConfig",
                "configuration",
                "mConfiguration"}) {
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
        for (String fieldName : new String[]{
                "displayInfo",
                "mDisplayInfo",
                "mTmpDisplayInfo",
                "mLastDisplayInfo"}) {
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
        for (String fieldName : new String[]{"frame", "mFrame", "displayFrame"}) {
            Object value = readField(target, fieldName);
            if (value instanceof Rect rect) {
                return rect;
            }
        }
        for (String fieldName : new String[]{"frames", "windowFrames", "clientWindowFrames", "outFrames", "result"}) {
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
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value instanceof String stringValue ? stringValue : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Configuration invokeConfigurationMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value instanceof Configuration configuration ? configuration : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object invokeObjectMethod(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Object readField(Object target, String fieldName) {
        if (target == null) {
            return null;
        }
        Field field = resolveField(target.getClass(), fieldName);
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
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null) {
            Field[] declared = current.getDeclaredFields();
            for (Field field : declared) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
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

    private static final class HookTarget {
        final String entryName;
        final String[] classNames;
        final String[] methodNames;

        HookTarget(String entryName, String[] classNames, String[] methodNames) {
            this.entryName = entryName;
            this.classNames = classNames;
            this.methodNames = methodNames;
        }

        String describeClassNames() {
            return String.join("|", classNames);
        }

        String describeMethodNames() {
            return String.join("|", methodNames);
        }
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
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static int safeInt(Integer value) {
        return value != null ? value : -1;
    }
}
