package com.dpis.module.runtime.systemserver;

import android.content.res.Configuration;

import com.dpis.module.config.ConfigSnapshotLoader;
import com.dpis.module.config.DpisConfigStore;
import com.dpis.module.config.PerAppDisplayConfig;
import com.dpis.module.config.PerAppDisplayConfigSource;
import com.dpis.module.diagnostics.DpisLog;
import com.dpis.module.hooks.HookRuntimePolicy;
import com.dpis.module.runtime.ConfigStoreFactory;
import com.dpis.module.runtime.ProcessScopedInstallGate;
import com.dpis.module.runtime.hookapi.ModernApiCapabilities;
import com.dpis.module.viewport.PerAppDisplayEnvironment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

import io.github.libxposed.api.XposedInterface;

@SuppressWarnings("java:S1872")
public final class HookInstaller {
    static final String[] CONFIGURATION_FIELD_NAMES = new String[]{
            "mergedConfiguration",
            "mLastReportedConfiguration",
            "mTmpConfig",
            "configuration",
            "mConfiguration"
    };
    private static final PackageUidResolver PACKAGE_UID_RESOLVER =
            new PackageUidResolver(
                    ConfigRefreshTtl.SYSTEM_SERVER_TTL_MILLIS);
    private static final Set<String> installedEntries = new LinkedHashSet<>();
    private static volatile int installedPid = -1;

    private HookInstaller() {
    }

    public static void resetForHotReload() {
        synchronized (HookInstaller.class) {
            installedPid = -1;
            installedEntries.clear();
        }
    }

    public static InstallResult install(XposedInterface xposed,
                                        DpisConfigStore store,
                                        ModernApiCapabilities apiCapabilities) {
        return install(xposed, store, apiCapabilities, null);
    }

    public static InstallResult install(XposedInterface xposed,
                                        DpisConfigStore store,
                                        ModernApiCapabilities apiCapabilities,
                                        ClassLoader systemServerClassLoader) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            HookDiagnostics.recordPending("system_server install skipped: reason=already-installed-fast-path");
            DpisLog.i(HookDiagnostics.buildInstallSkipLog("already-installed-fast-path"));
            return new InstallResult(0, 0, true);
        }
        synchronized (HookInstaller.class) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                HookDiagnostics.recordPending("system_server install skipped: reason=already-installed-synchronized");
                DpisLog.i(HookDiagnostics.buildInstallSkipLog("already-installed-synchronized"));
                return new InstallResult(0, 0, true);
            }
            HookDiagnostics.recordPending(
                    HookDiagnostics.buildInstallEnterLog(store == null, installedPid != -1));
            DpisLog.i(HookDiagnostics.buildInstallEnterLog(
                    store == null, installedPid != -1));
            try {
                HookDiagnostics.recordPending(
                        HookDiagnostics.buildBootstrapLog());
                DpisLog.i(HookDiagnostics.buildBootstrapLog());
                HookRuntimePolicy policy = HookRuntimePolicy.fromStore(store);
                if (!policy.systemServerHooksEnabled) {
                    HookDiagnostics.recordPending(
                            HookDiagnostics.buildGateDisabledLog(
                                    false, policy.systemServerSafeModeEnabled));
                    DpisLog.i(HookDiagnostics.buildGateDisabledLog(
                            false, policy.systemServerSafeModeEnabled));
                    installedPid = ProcessScopedInstallGate.currentPid();
                    return new InstallResult(0, 0, false);
                }
                PerAppDisplayConfigSource source =
                        PerAppDisplayConfigSource.withLegacyRuntimePropertyFallback(
                                new PerAppDisplayConfigSource.RefreshingSnapshotProvider(
                                        () -> ConfigSnapshotLoader.fromStore(
                                                ConfigStoreFactory.createForXposedHost(xposed)),
                                        ConfigRefreshTtl.SYSTEM_SERVER_TTL_MILLIS));
                Set<String> configuredPackages = source.getConfiguredPackages();
                int installedCount = 0;
                int missingCount = 0;
                StringBuilder installedEntrySummary = new StringBuilder();
                StringBuilder missingEntrySummary = new StringBuilder();
                if (MutationPolicy.shouldInstallTarget(
                        "launch-activity-item", policy.systemServerSafeModeEnabled)) {
                    if (isEntryInstalled("launch-activity-item")) {
                        installedCount++;
                        appendEntry(installedEntrySummary, "launch-activity-item");
                    } else if (installLaunchActivityItemHook(
                            xposed,
                            source,
                            HookCatalog.LAUNCH_ACTIVITY_ITEM,
                            apiCapabilities)) {
                        markEntryInstalled("launch-activity-item");
                        installedCount++;
                        appendEntry(installedEntrySummary, "launch-activity-item");
                    } else {
                        missingCount++;
                        appendEntry(missingEntrySummary, "launch-activity-item");
                    }
                }
                if (MutationPolicy.shouldInstallTarget(
                        "hyperos-rust-process", policy.systemServerSafeModeEnabled)) {
                    if (isEntryInstalled("hyperos-rust-process")) {
                        installedCount++;
                        appendEntry(installedEntrySummary, "hyperos-rust-process");
                    } else if (HyperOsRustProcessHookInstaller.install(xposed, source)) {
                        markEntryInstalled("hyperos-rust-process");
                        installedCount++;
                        appendEntry(installedEntrySummary, "hyperos-rust-process");
                    } else {
                        missingCount++;
                        appendEntry(missingEntrySummary, "hyperos-rust-process");
                    }
                }
                for (HookSpec hookSpec : HookCatalog.methodHookSpecs()) {
                    if (!MutationPolicy.shouldInstallTarget(
                            hookSpec.entryName, policy.systemServerSafeModeEnabled)) {
                        continue;
                    }
                    if (isEntryInstalled(hookSpec.entryName)) {
                        installedCount++;
                        appendEntry(installedEntrySummary, hookSpec.entryName);
                    } else if (installTargetHooks(
                            xposed,
                            source,
                            hookSpec,
                            configuredPackages,
                            apiCapabilities,
                            systemServerClassLoader)) {
                        markEntryInstalled(hookSpec.entryName);
                        installedCount++;
                        appendEntry(installedEntrySummary, hookSpec.entryName);
                    } else {
                        missingCount++;
                        appendEntry(missingEntrySummary, hookSpec.entryName);
                    }
                }
                HookDiagnostics.recordPending(
                        HookDiagnostics.buildInstallSummaryLog(
                                installedCount, missingCount));
                DpisLog.i(HookDiagnostics.buildInstallSummaryLog(
                        installedCount, missingCount));
                markInstalledWhenComplete(missingCount);
                return new InstallResult(installedCount, missingCount, false,
                        installedEntrySummary.toString(), missingEntrySummary.toString());
            } catch (Throwable throwable) {
                HookDiagnostics.recordPending(
                        "system_server install failed: throwable=" + throwable.getClass().getName());
                DpisLog.e("system_server install failed", throwable);
                throw throwable;
            }
        }
    }

    private static void markInstalledWhenComplete(int missingCount) {
        if (missingCount == 0) {
            installedPid = ProcessScopedInstallGate.currentPid();
        }
    }

    private static boolean isEntryInstalled(String entryName) {
        return installedEntries.contains(entryName);
    }

    private static void markEntryInstalled(String entryName) {
        installedEntries.add(entryName);
    }

    private static void appendEntry(StringBuilder builder, String entryName) {
        if (builder.length() > 0) {
            builder.append(',');
        }
        builder.append(entryName);
    }

    private static boolean installTargetHooks(XposedInterface xposed,
                                              PerAppDisplayConfigSource source,
                                              HookSpec hookSpec,
                                              Set<String> configuredPackages,
                                              ModernApiCapabilities apiCapabilities,
                                              ClassLoader systemServerClassLoader) {
        boolean hooked = false;
        for (ClassLoader classLoader : resolveCandidateClassLoaders(systemServerClassLoader)) {
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
                            .intercept(chain -> HookCallback.handle(
                                    chain, source, hookSpec.entryName));
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
            HookDiagnostics.recordPending(
                    HookDiagnostics.buildHookReadyLog(
                            hookSpec.entryName, hookSpec.describeClassNames(), hookSpec.describeMethodNames()));
            DpisLog.i(HookDiagnostics.buildHookReadyLog(
                    hookSpec.entryName, hookSpec.describeClassNames(), hookSpec.describeMethodNames()));
            return true;
        }
        HookDiagnostics.recordPending(
                HookDiagnostics.buildHookMissingLog(
                        hookSpec.entryName, hookSpec.describeClassNames(), hookSpec.describeMethodNames()));
        DpisLog.i(HookDiagnostics.buildHookMissingLog(
                hookSpec.entryName, hookSpec.describeClassNames(), hookSpec.describeMethodNames()));
        return false;
    }

    private static boolean installLaunchActivityItemHook(XposedInterface xposed,
                                                         PerAppDisplayConfigSource source,
                                                         HookSpec hookSpec,
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
                    .intercept(chain -> LaunchActivityItemApply.handleConstructor(source, chain));
            hooked = true;
        }
        if (hooked) {
            DpisLog.i("system_server hook ready: entry=launch-activity-item, class=LaunchActivityItem");
        }
        return hooked;
    }

    private static Set<ClassLoader> resolveCandidateClassLoaders(ClassLoader systemServerClassLoader) {
        Set<ClassLoader> classLoaders = new LinkedHashSet<>();
        classLoaders.add(systemServerClassLoader);
        classLoaders.add(Thread.currentThread().getContextClassLoader());
        classLoaders.add(HookInstaller.class.getClassLoader());
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

    static PerAppDisplayEnvironment resolveLaunchViewportEnvironment(
            String packageName,
            Configuration baseConfiguration,
            int widthPx,
            int heightPx,
            PerAppDisplayConfig config) {
        return LaunchActivityItemViewport.resolve(
                packageName, baseConfiguration, widthPx, heightPx, config);
    }

    static int configuredUid(String packageName, int callingUid) {
        return PACKAGE_UID_RESOLVER.resolve(packageName, callingUid);
    }

    public static final class InstallResult {
        public final int installedCount;
        public final int missingCount;
        public final boolean alreadyInstalled;
        public final String installedEntries;
        public final String missingEntries;

        InstallResult(int installedCount, int missingCount, boolean alreadyInstalled) {
            this(installedCount, missingCount, alreadyInstalled, "", "");
        }

        InstallResult(int installedCount, int missingCount, boolean alreadyInstalled,
                      String installedEntries, String missingEntries) {
            this.installedCount = installedCount;
            this.missingCount = missingCount;
            this.alreadyInstalled = alreadyInstalled;
            this.installedEntries = installedEntries;
            this.missingEntries = missingEntries;
        }

        public boolean hasInstalledHooks() {
            return alreadyInstalled || installedCount > 0;
        }

        public boolean isComplete() {
            return alreadyInstalled || missingCount == 0;
        }
    }
}
