package com.dpis.module;

final class HookRuntimePolicy {
    final boolean systemServerHooksEnabled;
    final boolean systemServerSafeModeEnabled;
    final boolean globalLogEnabled;
    final boolean probeHooksEnabled;

    private HookRuntimePolicy(boolean systemServerHooksEnabled,
                              boolean systemServerSafeModeEnabled,
                              boolean globalLogEnabled) {
        this.systemServerHooksEnabled = systemServerHooksEnabled;
        this.systemServerSafeModeEnabled = systemServerSafeModeEnabled;
        this.globalLogEnabled = globalLogEnabled;
        this.probeHooksEnabled = !systemServerSafeModeEnabled && globalLogEnabled;
    }

    static HookRuntimePolicy fromStore(DpiConfigStore store) {
        return new HookRuntimePolicy(
                store.isSystemServerHooksEnabled(),
                store.isSystemServerSafeModeEnabled(),
                store.isGlobalLogEnabled());
    }

    static HookRuntimePolicy fromNullableStore(DpiConfigStore store) {
        if (store == null) {
            return new HookRuntimePolicy(true, true, false);
        }
        return fromStore(store);
    }
}
