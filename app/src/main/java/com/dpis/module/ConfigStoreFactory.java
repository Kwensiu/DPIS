package com.dpis.module;

import com.dpis.module.config.RuntimePropertyConfigPreferences;


import com.dpis.module.fonts.FontLibraryStore;
import com.dpis.module.fonts.FontLibraryConfigStore;

import android.content.Context;
import android.content.SharedPreferences;

import com.dpis.module.runtime.XSharedPreferencesAdapter;

import java.io.File;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.service.XposedService;

public final class ConfigStoreFactory {
    private static final File PUBLIC_FONT_DIRECTORY = new File("/data/local/tmp");
    private static final String FONT_LIBRARY_GROUP = "font_library";

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

    public static DpisConfigStore createPackageLifecycleConfigStore(Context context) {
        return createLocalModuleConfigStore(context);
    }

    public static DpisConfigStore createDiagnosticLogGateConfigStore(Context context) {
        return createLocalModuleConfigStore(context);
    }

    public static boolean enableDiagnosticLogs(Context context) {
        DpisConfigStore store = createDiagnosticLogGateConfigStore(context);
        if (store == null || !store.setGlobalLogEnabled(true)) {
            return false;
        }
        DpisLog.setLoggingEnabled(true);
        return true;
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
        SharedPreferences fontLibraryPreferences =
                context.getSharedPreferences(FONT_LIBRARY_GROUP, Context.MODE_PRIVATE);
        SharedPreferences legacyPreferences =
                context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE);
        return new FontLibraryStore(fontLibraryPreferences, new File(context.getFilesDir(), "fonts"),
                PUBLIC_FONT_DIRECTORY, legacyPreferences);
    }

    public static FontLibraryStore createLocalUiFontLibraryStore(Context context, XposedService service) {
        return createLocalFontLibraryStore(context);
    }

    public static FontLibraryConfigStore createFontLibraryConfigStore(
            Context context,
            XposedService service) {
        DpisConfigStore store = createLocalUiModuleConfigStore(context, service);
        return new FontLibraryConfigStore(new FontLibraryConfigStore.Delegate() {
            @Override
            public boolean clearTargetTypefaceId(String packageName) {
                return store.clearTargetTypefaceId(packageName);
            }

            @Override
            public java.util.Set<String> getConfiguredPackages() {
                return store.getConfiguredPackages();
            }

            @Override
            public String getTargetTypefaceId(String packageName) {
                return store.getTargetTypefaceId(packageName);
            }

            @Override
            public boolean setTargetTypefaceId(String packageName, String typefaceId) {
                return store.setTargetTypefaceId(packageName, typefaceId);
            }
        });
    }

    public static DpisConfigStore createForXposedHost(XposedInterface xposed) {
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
        // Long-lived system_server refresh is owned by the display config source.
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
