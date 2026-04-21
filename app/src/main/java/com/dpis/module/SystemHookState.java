package com.dpis.module;

final class SystemHookState {
    enum Reason {
        NONE,
        DISABLED_BY_USER,
        REQUEST_PENDING,
        SERVICE_UNAVAILABLE,
        SCOPE_MISSING
    }

    final boolean desiredEnabled;
    final boolean effectiveEnabled;
    final boolean switchChecked;
    final boolean switchEnabled;
    final Reason reason;

    SystemHookState(boolean desiredEnabled,
                    boolean effectiveEnabled,
                    boolean switchChecked,
                    boolean switchEnabled,
                    Reason reason) {
        this.desiredEnabled = desiredEnabled;
        this.effectiveEnabled = effectiveEnabled;
        this.switchChecked = switchChecked;
        this.switchEnabled = switchEnabled;
        this.reason = reason;
    }
}
