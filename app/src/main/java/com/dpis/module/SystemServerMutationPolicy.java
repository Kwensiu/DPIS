package com.dpis.module;

final class SystemServerMutationPolicy {
    private static final String ENTRY_CONFIG_DISPATCH = "config-dispatch";
    private static final String ENTRY_ACTIVITY_START = "activity-start";

    private SystemServerMutationPolicy() {
    }

    static boolean shouldApplyPreProceedMutations(String entryName) {
        return ENTRY_CONFIG_DISPATCH.equals(entryName)
                || ENTRY_ACTIVITY_START.equals(entryName);
    }

    static boolean shouldApplyPostProceedMutations(String entryName) {
        return !shouldApplyPreProceedMutations(entryName);
    }

    static boolean shouldInstallTarget(String entryName, boolean safeModeEnabled) {
        if (safeModeEnabled) {
            // Safe mode keeps only the lowest-risk system_server entry.
            return ENTRY_ACTIVITY_START.equals(entryName);
        }
        return true;
    }
}
