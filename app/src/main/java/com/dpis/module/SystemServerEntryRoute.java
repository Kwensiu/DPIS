package com.dpis.module;

final class SystemServerEntryRoute {
    private static final String ENTRY_CONFIG_DISPATCH = "config-dispatch";
    private static final String ENTRY_ACTIVITY_START = "activity-start";
    private static final String ENTRY_LAUNCH_ACTIVITY_ITEM = "launch-activity-item";
    private static final String ENTRY_HYPEROS_RUST_PROCESS = "hyperos-rust-process";
    private static final String ENTRY_DISPLAY_MANAGER_INFO = "display-manager-info";
    private static final String ENTRY_RELAYOUT_DISPATCH = "relayout-dispatch";
    private static final String ENTRY_DISPLAY_POLICY_LAYOUT = "display-policy-layout";
    private static final String ENTRY_DISPLAY_CONTENT_CONFIG = "display-content-config";

    private SystemServerEntryRoute() {
    }

    static boolean isConfigDispatch(String entryName) {
        return ENTRY_CONFIG_DISPATCH.equals(entryName);
    }

    static boolean isActivityStart(String entryName) {
        return ENTRY_ACTIVITY_START.equals(entryName);
    }

    static boolean isLaunchActivityItem(String entryName) {
        return ENTRY_LAUNCH_ACTIVITY_ITEM.equals(entryName);
    }

    static boolean isHyperOsRustProcess(String entryName) {
        return ENTRY_HYPEROS_RUST_PROCESS.equals(entryName);
    }

    static boolean isDisplayManagerInfo(String entryName) {
        return ENTRY_DISPLAY_MANAGER_INFO.equals(entryName);
    }

    static boolean isRelayoutDispatch(String entryName) {
        return ENTRY_RELAYOUT_DISPATCH.equals(entryName);
    }

    static boolean isDisplayPolicyLayout(String entryName) {
        return ENTRY_DISPLAY_POLICY_LAYOUT.equals(entryName);
    }

    static boolean isDisplayContentConfig(String entryName) {
        return ENTRY_DISPLAY_CONTENT_CONFIG.equals(entryName);
    }

    static boolean isPreProceedMutationEntry(String entryName) {
        return isConfigDispatch(entryName) || isActivityStart(entryName);
    }

    static boolean isPostProceedMutationEntry(String entryName) {
        return !isConfigDispatch(entryName);
    }

    static boolean isLaunchOnlyMutationEntry(String entryName) {
        return isLaunchActivityItem(entryName);
    }

    static boolean isLowRiskSystemServerEntry(String entryName) {
        return isActivityStart(entryName)
                || isConfigDispatch(entryName)
                || isLaunchActivityItem(entryName)
                || isDisplayManagerInfo(entryName)
                || isHyperOsRustProcess(entryName);
    }

    static boolean isHotEntry(String entryName) {
        return isDisplayPolicyLayout(entryName) || isRelayoutDispatch(entryName);
    }

    static boolean isCoreLogEntry(String entryName) {
        return isActivityStart(entryName)
                || isConfigDispatch(entryName)
                || isDisplayContentConfig(entryName);
    }
}
