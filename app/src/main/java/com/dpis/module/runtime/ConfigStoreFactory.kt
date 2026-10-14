package com.dpis.module.runtime

import android.content.Context
import com.dpis.module.BuildConfig
import com.dpis.module.DpisApplication
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontLibraryConfigStore
import com.dpis.module.fonts.FontLibraryStore
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.service.XposedService
import java.io.File

object ConfigStoreFactory {
    private val PUBLIC_FONT_DIRECTORY = File("/data/local/tmp")
    private const val FONT_LIBRARY_GROUP = "font_library"

    @JvmStatic
    fun createLocalModuleConfigStore(context: Context): DpisConfigStore {
        val legacySharedPrefsFile = legacySharedPrefsFile(context)
        val store = DpisConfigStore(
            context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE),
            legacySharedPrefsFile,
        )
        if (BuildConfig.FLAVOR == "legacy") {
            store.importSharedPreferencesXml(legacySharedPrefsFile)
        }
        return store
    }

    @JvmStatic
    fun createPackageLifecycleConfigStore(context: Context): DpisConfigStore =
        createLocalModuleConfigStore(context)

    @JvmStatic
    fun createDiagnosticLogGateConfigStore(context: Context): DpisConfigStore {
        // Log pages must gate against the same active store as the UI, not a stale local copy.
        return DpisApplication.getActiveHookConfigStore(context)
            ?: createLocalModuleConfigStore(context)
    }

    @JvmStatic
    fun enableDiagnosticLogs(context: Context): Boolean {
        val store = createDiagnosticLogGateConfigStore(context)
        if (!store.setGlobalLogEnabled(true)) {
            return false
        }
        DpisLog.setLoggingEnabled(true)
        return true
    }

    @JvmStatic
    fun createLocalUiModuleConfigStore(
        context: Context,
        service: XposedService?,
    ): DpisConfigStore {
        val legacySharedPrefsFile = legacySharedPrefsFile(context)
        val localPreferences =
            context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE)
        var preferences = localPreferences
        var usingRemote = false
        if (BuildConfig.FLAVOR == "modern" && service != null) {
            try {
                service.getRemotePreferences(DpisConfigStore.GROUP)?.let {
                    preferences = it
                    usingRemote = true
                }
            } catch (_: Throwable) {
                // Remote preferences are unavailable; keep the app-local store.
            }
        }
        val store = DpisConfigStore(
            preferences,
            null,
            legacySharedPrefsFile,
            localPreferences,
        )
        if (BuildConfig.FLAVOR == "modern" && usingRemote &&
            !store.hasAnyUserVisiblePackageConfig()
        ) {
            store.importSharedPreferencesXml(legacySharedPrefsFile)
        }
        if (BuildConfig.FLAVOR == "legacy" && !usingRemote) {
            store.importSharedPreferencesXml(legacySharedPrefsFile)
        }
        return store
    }

    private fun legacySharedPrefsFile(context: Context): File =
        File(
            File(context.applicationInfo.dataDir, "shared_prefs"),
            "${DpisConfigStore.GROUP}.xml",
        )

    @JvmStatic
    fun createRuntimeDeliveryModuleConfigStore(service: XposedService?): DpisConfigStore? {
        if (service == null) return null
        return try {
            service.getRemotePreferences(DpisConfigStore.GROUP)?.let(::DpisConfigStore)
        } catch (_: Throwable) {
            // Remote preferences are unavailable.
            null
        }
    }

    @JvmStatic
    fun createLocalFontLibraryStore(context: Context): FontLibraryStore {
        val fontLibraryPreferences =
            context.getSharedPreferences(FONT_LIBRARY_GROUP, Context.MODE_PRIVATE)
        val legacyPreferences =
            context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE)
        return FontLibraryStore(
            fontLibraryPreferences,
            File(context.filesDir, "fonts"),
            PUBLIC_FONT_DIRECTORY,
            legacyPreferences,
        )
    }

    @JvmStatic
    fun createLocalUiFontLibraryStore(
        context: Context,
        @Suppress("UNUSED_PARAMETER") service: XposedService?,
    ): FontLibraryStore = createLocalFontLibraryStore(context)

    @JvmStatic
    fun createFontLibraryConfigStore(
        context: Context,
        service: XposedService?,
    ): FontLibraryConfigStore {
        val store = createLocalUiModuleConfigStore(context, service)
        return FontLibraryConfigStore(
            object : FontLibraryConfigStore.Delegate {
                override fun clearTargetTypefaceId(packageName: String?): Boolean =
                    packageName?.let(store::clearTargetTypefaceId) == true

                override fun getConfiguredPackages(): Set<String> =
                    store.getConfiguredPackages().filterNotNull().toSet()

                override fun getTargetTypefaceId(packageName: String?): String? =
                    packageName?.let(store::getTargetTypefaceId)

                override fun setTargetTypefaceId(
                    packageName: String?,
                    typefaceId: String?
                ): Boolean =
                    if (packageName == null) false else store.setTargetTypefaceId(
                        packageName,
                        typefaceId
                    )
            },
        )
    }

    @JvmStatic
    fun createForXposedHost(xposed: XposedInterface?): DpisConfigStore {
        val remotePreferences = try {
            xposed?.getRemotePreferences(DpisConfigStore.GROUP)
        } catch (_: Throwable) {
            // The Modern runtime has no classic-preferences fallback.
            null
        }
        return remotePreferences?.let(::DpisConfigStore)
            ?: throw UnsupportedOperationException(
                "Modern runtime requires libxposed remote preferences",
            )
    }

    @JvmStatic
    fun createFontLibraryForXposedHost(xposed: XposedInterface?): FontLibraryStore {
        val remotePreferences = try {
            xposed?.getRemotePreferences(DpisConfigStore.GROUP)
        } catch (_: Throwable) {
            // The Modern runtime has no classic-preferences fallback.
            null
        }
        return remotePreferences?.let { FontLibraryStore(it, null) }
            ?: throw UnsupportedOperationException(
                "Modern runtime requires libxposed remote preferences",
            )
    }
}
