package com.dpis.module.settings;

public final class SystemHookEffectiveView {
    public final boolean desiredEnabled;
    public final boolean effectiveEnabled;
    public final SystemHookState.Reason reason;

    private SystemHookEffectiveView(boolean desiredEnabled,
                                    boolean effectiveEnabled,
                                    SystemHookState.Reason reason) {
        this.desiredEnabled = desiredEnabled;
        this.effectiveEnabled = effectiveEnabled;
        this.reason = reason;
    }

    public static SystemHookEffectiveView resolve(boolean desiredEnabled,
                                                  boolean serviceAvailable,
                                                  boolean scopeSelected) {
        SystemHookState state = SystemHookStateResolver.resolve(
                desiredEnabled,
                false,
                serviceAvailable,
                scopeSelected);
        return new SystemHookEffectiveView(state.desiredEnabled, state.effectiveEnabled, state.reason);
    }
}
