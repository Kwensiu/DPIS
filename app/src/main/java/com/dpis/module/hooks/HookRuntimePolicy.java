package com.dpis.module.hooks;

import com.dpis.module.config.ConfigSnapshot;

import com.dpis.module.DpisConfigStore;


public final class HookRuntimePolicy {
    public final boolean systemServerHooksEnabled;
    public final boolean systemServerHooksDesiredEnabled;
    public final boolean systemServerSafeModeEnabled;
    public final boolean globalLogEnabled;
    public final boolean probeHooksEnabled;

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

    public static HookRuntimePolicy fromStore(DpisConfigStore store) {
        return new HookRuntimePolicy(
                store.isSystemServerHooksEnabled(),
                store.isSystemServerHooksEnabled(),
                store.isSystemServerSafeModeEnabled(),
                store.isGlobalLogEnabled());
    }

    public static HookRuntimePolicy fromSnapshot(ConfigSnapshot snapshot) {
        if (snapshot == null) {
            return new HookRuntimePolicy(true, true, true, false);
        }
        return new HookRuntimePolicy(
                snapshot.isSystemServerHooksEnabled(),
                snapshot.isSystemServerHooksEnabled(),
                snapshot.isSystemServerSafeModeEnabled(),
                snapshot.isGlobalLogEnabled());
    }

    public static HookRuntimePolicy fromNullableStore(DpisConfigStore store) {
        if (store == null) {
            return new HookRuntimePolicy(true, true, true, false);
        }
        return fromStore(store);
    }

    public static HookRuntimePolicy fromEffectiveSystemHookState(DpisConfigStore store,
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
