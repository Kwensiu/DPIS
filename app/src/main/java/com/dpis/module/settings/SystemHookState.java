package com.dpis.module.settings;

public final class SystemHookState {
    public enum Reason {
        NONE,
        DISABLED_BY_USER,
        REQUEST_PENDING,
        SERVICE_UNAVAILABLE,
        SCOPE_MISSING
    }

    public final boolean desiredEnabled;
    public final boolean effectiveEnabled;
    public final boolean switchChecked;
    public final boolean switchEnabled;
    public final Reason reason;

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
