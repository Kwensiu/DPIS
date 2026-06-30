package com.dpis.module;

final class HookRuntimePolicy {
    final boolean systemServerHooksEnabled;
    final boolean systemServerHooksDesiredEnabled;
    final boolean systemServerSafeModeEnabled;
    final boolean globalLogEnabled;
    final boolean probeHooksEnabled;

    private HookRuntimePolicy(boolean systemServerHooksEnabled,
                              boolean systemServerHooksDesiredEnabled,
                              boolean systemServerSafeModeEnabled,
                              boolean globalLogEnabled) {
        this.systemServerHooksEnabled = systemServerHooksEnabled;
        this.systemServerHooksDesiredEnabled = systemServerHooksDesiredEnabled;
        this.systemServerSafeModeEnabled = systemServerSafeModeEnabled;
        this.globalLogEnabled = globalLogEnabled;
        this.probeHooksEnabled = !systemServerSafeModeEnabled && globalLogEnabled;
    }

    static HookRuntimePolicy fromStore(DpisConfigStore store) {
        return new HookRuntimePolicy(
                store.isSystemServerHooksEnabled(),
                store.isSystemServerHooksEnabled(),
                store.isSystemServerSafeModeEnabled(),
                store.isGlobalLogEnabled());
    }

    static HookRuntimePolicy fromSnapshot(ConfigSnapshot snapshot) {
        if (snapshot == null) {
            return new HookRuntimePolicy(true, true, true, false);
        }
        return new HookRuntimePolicy(
                snapshot.isSystemServerHooksEnabled(),
                snapshot.isSystemServerHooksEnabled(),
                snapshot.isSystemServerSafeModeEnabled(),
                snapshot.isGlobalLogEnabled());
    }

    static HookRuntimePolicy fromNullableStore(DpisConfigStore store) {
        if (store == null) {
            return new HookRuntimePolicy(true, true, true, false);
        }
        return fromStore(store);
    }

    static HookRuntimePolicy fromEffectiveSystemHookState(DpisConfigStore store,
                                                          boolean systemServerHooksEffectiveEnabled) {
        if (store == null) {
            return new HookRuntimePolicy(systemServerHooksEffectiveEnabled, true, true, false);
        }
        return new HookRuntimePolicy(
                systemServerHooksEffectiveEnabled,
                store.isSystemServerHooksEnabled(),
                store.isSystemServerSafeModeEnabled(),
                store.isGlobalLogEnabled());
    }
}
