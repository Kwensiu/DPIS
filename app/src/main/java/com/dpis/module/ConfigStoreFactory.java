package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.service.XposedService;

final class ConfigStoreFactory {
    private ConfigStoreFactory() {
    }

    static DpiConfigStore createForModuleApp(Context context) {
        return new DpiConfigStore(context.getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE));
    }

    static DpiConfigStore createForModuleApp(Context context, XposedService service) {
        if (service != null) {
            try {
                SharedPreferences remotePreferences = service.getRemotePreferences(DpiConfigStore.GROUP);
                if (remotePreferences != null) {
                    return new DpiConfigStore(remotePreferences);
                }
            } catch (Throwable ignored) {
                // Fall through to local storage.
            }
        }
        return createForModuleApp(context);
    }

    static DpiConfigStore createForXposedHost(XposedInterface xposed) {
        SharedPreferences remotePreferences = null;
        if (xposed != null) {
            try {
                remotePreferences = xposed.getRemotePreferences(DpiConfigStore.GROUP);
            } catch (Throwable ignored) {
                // Fall back to legacy XSharedPreferences path when remote preferences are unavailable.
            }
        }
        if (remotePreferences != null) {
            return new DpiConfigStore(remotePreferences);
        }
        return new DpiConfigStore(new XSharedPreferencesAdapter("com.dpis.module", DpiConfigStore.GROUP));
    }
}
