package com.dpis.module;

final class SystemServerMutationPolicy {
    private SystemServerMutationPolicy() {
    }

    static boolean shouldApplyPreProceedMutations(String entryName) {
        return "config-dispatch".equals(entryName)
                || "activity-start".equals(entryName);
    }

    static boolean shouldApplyPostProceedMutations(String entryName) {
        return !shouldApplyPreProceedMutations(entryName);
    }

    static boolean shouldInstallTarget(String entryName, boolean safeModeEnabled) {
        if (safeModeEnabled) {
            return "activity-start".equals(entryName);
        }
        return true;
    }
}
