package com.dpis.module;

final class SystemServerMutationPolicy {
    private static final String ENTRY_CONFIG_DISPATCH = "config-dispatch";
    private static final String ENTRY_ACTIVITY_START = "activity-start";
    private static final String ENTRY_LAUNCH_ACTIVITY_ITEM = "launch-activity-item";
    private static final String ENTRY_HYPEROS_RUST_PROCESS = "hyperos-rust-process";
    private static final String ENTRY_DISPLAY_MANAGER_INFO = "display-manager-info";

    private SystemServerMutationPolicy() {
    }

    static boolean shouldApplyPreProceedMutations(String entryName) {
        return ENTRY_CONFIG_DISPATCH.equals(entryName)
                || ENTRY_ACTIVITY_START.equals(entryName);
    }

    static boolean shouldApplyMutationField(String entryName,
                                            SystemServerMutationField field) {
        if (field == null) {
            return false;
        }
        return switch (field) {
            case VIEWPORT -> true;
            case FONT_SCALE -> ENTRY_LAUNCH_ACTIVITY_ITEM.equals(entryName);
        };
    }

    static boolean shouldApplyPostProceedMutations(String entryName) {
        return !ENTRY_CONFIG_DISPATCH.equals(entryName);
    }

    static boolean shouldInstallTarget(String entryName, boolean safeModeEnabled) {
        if (safeModeEnabled) {
            // Safe mode keeps only the lowest-risk system_server entry.
            return isLowRiskEntry(entryName);
        }
        return true;
    }

    static boolean shouldInstallSystemServerHooks(String processName,
                                                  String packageName,
                                                  HookRuntimePolicy policy) {
        if (!SystemServerProcess.isSystemServer(processName, packageName)) {
            return false;
        }
        return policy == null || policy.systemServerHooksEnabled;
    }

    private static boolean isLowRiskEntry(String entryName) {
        return ENTRY_ACTIVITY_START.equals(entryName)
                // Existing/resumed activities need the normal reported
                // configuration dispatch to refresh app-side resources. This
                // mutates only the target activity configuration and keeps the
                // hotter frame, relayout, and DisplayContent routes out of safe mode.
                || ENTRY_CONFIG_DISPATCH.equals(entryName)
                // LaunchActivityItem mutates only the target app's launch-time
                // configuration and is the system route that reliably reaches
                // apps whose normal activity-start path does not expose a
                // mutable Configuration object.
                || ENTRY_LAUNCH_ACTIVITY_ITEM.equals(entryName)
                // DisplayManagerService serves display metrics through Binder.
                // The hook is caller-UID gated before mutation, so it can
                // supplement WebView/Chromium display queries without enabling
                // global DisplayContent or relayout mutations.
                || ENTRY_DISPLAY_MANAGER_INFO.equals(entryName)
                // HyperOS native font replacement depends on rewriting the Rust
                // process launch environment. This path is package-gated by config
                // and does not mutate shared display state, so safe mode should keep it.
                || ENTRY_HYPEROS_RUST_PROCESS.equals(entryName);
    }
}
