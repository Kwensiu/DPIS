package com.dpis.module;

final class SystemHookEffectiveView {
    final boolean desiredEnabled;
    final boolean effectiveEnabled;
    final SystemHookState.Reason reason;

    private SystemHookEffectiveView(boolean desiredEnabled,
                                    boolean effectiveEnabled,
                                    SystemHookState.Reason reason) {
        this.desiredEnabled = desiredEnabled;
        this.effectiveEnabled = effectiveEnabled;
        this.reason = reason;
    }

    static SystemHookEffectiveView resolve(boolean desiredEnabled,
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
