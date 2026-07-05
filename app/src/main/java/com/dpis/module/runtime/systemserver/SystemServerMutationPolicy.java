package com.dpis.module.runtime.systemserver;

import com.dpis.module.*;
import com.dpis.module.hooks.HookRuntimePolicy;

public final class SystemServerMutationPolicy {
    private SystemServerMutationPolicy() {
    }

    static boolean shouldApplyPreProceedMutations(String entryName) {
        return SystemServerEntryRoute.isPreProceedMutationEntry(entryName);
    }

    static boolean shouldApplyMutationField(String entryName,
                                            SystemServerMutationField field) {
        if (field == null) {
            return false;
        }
        // Keep this package-neutral. Hook domains express user intent; this
        // field policy decides where a requested mutation can safely write.
        //
        // FONT_SCALE is launch-only on purpose. Writing Configuration.fontScale
        // during config-dispatch (updateReportedConfigurationAndSend) flips
        // CONFIG_FONT_SCALE and triggers an Activity relaunch, which is worse
        // than leaving the running config untouched. The accepted tradeoff is a
        // runtime base/target split: scaledDensity reaches the target via the
        // app-process resources_font read path, while Configuration.fontScale
        // stays at the system base on later dispatches. Apps that need both
        // values consistent should use compat mode (it unifies them on the
        // app-process read path without going through config-dispatch). See
        // docs/font-routing.md "Why FONT_SCALE is launch-only".
        return switch (field) {
            case VIEWPORT -> true;
            case FONT_SCALE -> SystemServerEntryRoute.isLaunchOnlyMutationEntry(entryName);
        };
    }

    static boolean shouldApplyPostProceedMutations(String entryName) {
        return SystemServerEntryRoute.isPostProceedMutationEntry(entryName);
    }

    static boolean shouldInstallTarget(String entryName, boolean safeModeEnabled) {
        if (safeModeEnabled) {
            // Safe mode keeps only the lowest-risk system_server entry.
            return SystemServerEntryRoute.isLowRiskSystemServerEntry(entryName);
        }
        return true;
    }

    public static boolean shouldInstallSystemServerHooks(String processName,
                                                  String packageName,
                                                  HookRuntimePolicy policy) {
        if (!SystemServerProcess.isSystemServer(processName, packageName)) {
            return false;
        }
        return policy == null || policy.systemServerHooksEnabled;
    }

}
