package com.dpis.module;

import com.dpis.module.fonts.FontLibraryStore;

import android.content.Context;
import android.content.SharedPreferences;

import com.dpis.module.runtime.XSharedPreferencesAdapter;

import java.io.File;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.service.XposedService;

public final class ConfigStoreFactory {
    private static final File PUBLIC_FONT_DIRECTORY = new File("/data/local/tmp");

    private ConfigStoreFactory() {
    }

    static DpisConfigStore createLocalModuleConfigStore(Context context) {
        File legacySharedPrefsFile = legacySharedPrefsFile(context);
        DpisConfigStore store = new DpisConfigStore(
                context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE),
                legacySharedPrefsFile);
        if ("legacy".equals(BuildConfig.FLAVOR)) {
            store.importSharedPreferencesXml(legacySharedPrefsFile);
        }
        return store;
    }

    public static DpisConfigStore createLocalUiModuleConfigStore(Context context, XposedService service) {
        File legacySharedPrefsFile = legacySharedPrefsFile(context);
        SharedPreferences localPreferences =
                context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE);
        SharedPreferences preferences = localPreferences;
        boolean usingRemote = false;
        if ("modern".equals(BuildConfig.FLAVOR) && service != null) {
            try {
                SharedPreferences remotePreferences = service.getRemotePreferences(DpisConfigStore.GROUP);
                if (remotePreferences != null) {
                    preferences = remotePreferences;
                    usingRemote = true;
                }
            } catch (Throwable ignored) {
                // Remote preferences are unavailable; keep the app-local store.
            }
        }
        DpisConfigStore store = new DpisConfigStore(
                preferences, null, legacySharedPrefsFile, localPreferences);
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
                DpisConfigStore.GROUP + ".xml");
    }

    static DpisConfigStore createRuntimeDeliveryModuleConfigStore(XposedService service) {
        if (service == null) {
            return null;
        }
        try {
            SharedPreferences remotePreferences = service.getRemotePreferences(DpisConfigStore.GROUP);
            if (remotePreferences != null) {
                return new DpisConfigStore(remotePreferences);
            }
        } catch (Throwable ignored) {
            // Remote preferences are unavailable.
        }
        return null;
    }

    static FontLibraryStore createLocalFontLibraryStore(Context context) {
        SharedPreferences preferences =
                context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE);
        return new FontLibraryStore(preferences, new File(context.getFilesDir(), "fonts"),
                PUBLIC_FONT_DIRECTORY);
    }

    public static FontLibraryStore createLocalUiFontLibraryStore(Context context, XposedService service) {
        return createLocalFontLibraryStore(context);
    }

    static DpisConfigStore createForXposedHost(XposedInterface xposed) {
        SharedPreferences remotePreferences = null;
        if (xposed != null) {
            try {
                remotePreferences = xposed.getRemotePreferences(DpisConfigStore.GROUP);
            } catch (Throwable ignored) {
                // Fall back to legacy XSharedPreferences path when remote preferences are unavailable.
            }
        }
        if (remotePreferences != null) {
            return new DpisConfigStore(remotePreferences);
        }
        return new DpisConfigStore(
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpisConfigStore.GROUP));
    }

    public static FontLibraryStore createFontLibraryForXposedHost(XposedInterface xposed) {
        SharedPreferences remotePreferences = null;
        if (xposed != null) {
            try {
                remotePreferences = xposed.getRemotePreferences(DpisConfigStore.GROUP);
            } catch (Throwable ignored) {
                // Fall back to legacy XSharedPreferences path when remote preferences are unavailable.
            }
        }
        if (remotePreferences != null) {
            return new FontLibraryStore(remotePreferences, null);
        }
        return new FontLibraryStore(
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpisConfigStore.GROUP),
                null);
    }

    static FontLibraryStore createFontLibraryForLegacyHost() {
        return new FontLibraryStore(
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpisConfigStore.GROUP),
                null);
    }

    static DpisConfigStore createForLegacyHost() {
        return new DpisConfigStore(
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpisConfigStore.GROUP));
    }

    static DpisConfigStore createForLegacySystemServerHost() {
        // Long-lived system_server refresh is owned by RefreshingConfigSnapshotProvider.
        return createForLegacyHost();
    }

    static DpisConfigStore createForLegacyHost(String packageName) {
        return createForLegacyHost(packageName,
                RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.NONE);
    }

    static DpisConfigStore createForLegacyMainProcessHost(String packageName) {
        return createForLegacyHost(packageName,
                RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET);
    }

    private static DpisConfigStore createForLegacyHost(
            String packageName,
            RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute autoViewportRuntimeRoute) {
        SharedPreferences xSharedPreferences =
                new XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpisConfigStore.GROUP);
        if (packageName == null || packageName.isBlank()) {
            return new DpisConfigStore(xSharedPreferences);
        }
        // The Legacy classic-Xposed entrypoint has no libxposed remote preferences
        // service. Runtime app-process hooks read the per-package system-property
        // bridge first, with XSharedPreferences kept only as a startup fallback for
        // older or unsynced configuration.
        return new DpisConfigStore(
                new RuntimePropertyConfigPreferences(packageName, autoViewportRuntimeRoute),
                xSharedPreferences);
    }
}
