package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.service.XposedService;

final class ConfigStoreFactory {
    private static final File PUBLIC_FONT_DIRECTORY = new File("/data/local/tmp");

    private ConfigStoreFactory() {
    }

    static DpiConfigStore createLocalModuleConfigStore(Context context) {
        File legacySharedPrefsFile = legacySharedPrefsFile(context);
        DpiConfigStore store = new DpiConfigStore(
                context.getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE),
                legacySharedPrefsFile);
        if ("legacy".equals(BuildConfig.FLAVOR)) {
            store.importSharedPreferencesXml(legacySharedPrefsFile);
        }
        return store;
    }

    static DpiConfigStore createLocalUiModuleConfigStore(Context context, XposedService service) {
        File legacySharedPrefsFile = legacySharedPrefsFile(context);
        SharedPreferences preferences = context.getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE);
        boolean usingRemote = false;
        if ("modern".equals(BuildConfig.FLAVOR) && service != null) {
            try {
                SharedPreferences remotePreferences = service.getRemotePreferences(DpiConfigStore.GROUP);
                if (remotePreferences != null) {
                    preferences = remotePreferences;
                    usingRemote = true;
                }
            } catch (Throwable ignored) {
                // Remote preferences are unavailable; keep the app-local store.
            }
        }
        DpiConfigStore store = new DpiConfigStore(preferences, legacySharedPrefsFile);
        if ("modern".equals(BuildConfig.FLAVOR)
                && usingRemote
                && !store.hasAnyUserVisiblePackageConfig()) {
            store.importSharedPreferencesXml(legacySharedPrefsFile);
        }
        if ("legacy".equals(BuildConfig.FLAVOR) && !usingRemote) {
            store.importSharedPreferencesXml(legacySharedPrefsFile);
        }
        return store;
    }

    private static File legacySharedPrefsFile(Context context) {
        return new File(
                new File(context.getApplicationInfo().dataDir, "shared_prefs"),
                DpiConfigStore.GROUP + ".xml");
    }

    static DpiConfigStore createRuntimeDeliveryModuleConfigStore(XposedService service) {
        if (service == null) {
            return null;
        }
        try {
            SharedPreferences remotePreferences = service.getRemotePreferences(DpiConfigStore.GROUP);
            if (remotePreferences != null) {
                return new DpiConfigStore(remotePreferences);
            }
        } catch (Throwable ignored) {
            // Remote preferences are unavailable.
        }
        return null;
    }

    static FontLibraryStore createLocalFontLibraryStore(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE);
        return new FontLibraryStore(preferences, new File(context.getFilesDir(), "fonts"),
                PUBLIC_FONT_DIRECTORY);
    }

    static FontLibraryStore createLocalUiFontLibraryStore(Context context, XposedService service) {
        return createLocalFontLibraryStore(context);
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

    static FontLibraryStore createFontLibraryForXposedHost(XposedInterface xposed) {
        SharedPreferences remotePreferences = null;
        if (xposed != null) {
            try {
                remotePreferences = xposed.getRemotePreferences(DpiConfigStore.GROUP);
            } catch (Throwable ignored) {
                // Fall back to legacy XSharedPreferences path when remote preferences are unavailable.
            }
        }
        if (remotePreferences != null) {
            return new FontLibraryStore(remotePreferences, null);
        }
        return new FontLibraryStore(
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpiConfigStore.GROUP),
                null);
    }

    static FontLibraryStore createFontLibraryForLegacyHost() {
        return new FontLibraryStore(
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpiConfigStore.GROUP),
                null);
    }

    static DpiConfigStore createForLegacyHost() {
        return new DpiConfigStore(
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpiConfigStore.GROUP));
    }

    static DpiConfigStore createForLegacySystemServerHost() {
        // Long-lived system_server refresh is owned by RefreshingConfigSnapshotProvider.
        return createForLegacyHost();
    }

    static DpiConfigStore createForLegacyHost(String packageName) {
        return createForLegacyHost(packageName,
                RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.NONE);
    }

    static DpiConfigStore createForLegacyMainProcessHost(String packageName) {
        return createForLegacyHost(packageName,
                RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ABSOLUTE_TARGETS_ONLY);
    }

    private static DpiConfigStore createForLegacyHost(
            String packageName,
            RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute autoViewportRuntimeRoute) {
        SharedPreferences xSharedPreferences =
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpiConfigStore.GROUP);
        if (packageName == null || packageName.isBlank()) {
            return new DpiConfigStore(xSharedPreferences);
        }
        // The Legacy classic-Xposed entrypoint has no libxposed remote preferences
        // service. Runtime app-process hooks read the per-package system-property
        // bridge first, with XSharedPreferences kept only as a startup fallback for
        // older or unsynced configuration.
        return new DpiConfigStore(
                new RuntimePropertyConfigPreferences(packageName, autoViewportRuntimeRoute),
                xSharedPreferences);
    }
}
