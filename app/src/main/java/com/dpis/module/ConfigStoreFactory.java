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

    static DpiConfigStore createForCompat100Host() {
        return new DpiConfigStore(
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpiConfigStore.GROUP));
    }

    static DpiConfigStore createForCompat100SystemServerHost() {
        // Long-lived system_server refresh is owned by RefreshingConfigSnapshotProvider.
        return createForCompat100Host();
    }

    static DpiConfigStore createForCompat100Host(String packageName) {
        SharedPreferences xSharedPreferences =
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpiConfigStore.GROUP);
        if (packageName == null || packageName.isBlank()) {
            return new DpiConfigStore(xSharedPreferences);
        }
        // API100 has no libxposed remote preferences service. Runtime app-process hooks
        // read the per-package system-property bridge first, with XSharedPreferences kept
        // only as a startup fallback for older or unsynced configuration.
        return new DpiConfigStore(
                new SystemPropertyConfigPreferences(packageName),
                xSharedPreferences);
    }
}
