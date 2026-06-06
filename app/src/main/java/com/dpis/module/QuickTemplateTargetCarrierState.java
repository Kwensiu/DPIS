package com.dpis.module;

final class QuickTemplateTargetCarrierState {
    private QuickTemplateTargetCarrierState() {
    }

    enum CloseReason {
        ORIENTATION_MIGRATION,
        USER_BACK,
        SAVED,
        MISSING_TEMPLATE,
        UNKNOWN
    }

    static boolean shouldStartPortraitActivity(
            boolean landscapeDetailMode,
            boolean pendingTargets,
            boolean activityStarted
    ) {
        return !landscapeDetailMode && pendingTargets && !activityStarted;
    }

    static boolean shouldClearPendingAfterResult(
            boolean landscapeDetailMode,
            boolean pendingTargets,
            CloseReason closeReason
    ) {
        return !landscapeDetailMode
                && pendingTargets
                && closeReason != CloseReason.ORIENTATION_MIGRATION;
    }
}
