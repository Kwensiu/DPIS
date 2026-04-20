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
        SharedPreferences localPreferences =
                context.getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE);
        if (service != null) {
            try {
                SharedPreferences remotePreferences = service.getRemotePreferences(DpiConfigStore.GROUP);
                if (remotePreferences != null) {
                    return new DpiConfigStore(remotePreferences, localPreferences);
                }
            } catch (Throwable ignored) {
                // Fall through to local storage.
            }
        }
        return new DpiConfigStore(localPreferences);
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
        return new DpiConfigStore(
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpiConfigStore.GROUP));
    }
}
