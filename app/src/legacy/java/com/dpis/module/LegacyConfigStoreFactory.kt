package com.dpis.module

import android.content.SharedPreferences
import com.dpis.module.config.RuntimePropertyConfigPreferences
import com.dpis.module.fonts.FontLibraryStore
import com.dpis.module.runtime.XSharedPreferencesAdapter

/** Legacy-only owner of the classic Xposed preference compatibility boundary. */
object LegacyConfigStoreFactory {
    @JvmStatic
    fun createFontLibrary(): FontLibraryStore = FontLibraryStore(legacyPreferences(), null)

    @JvmStatic
    fun create(): DpisConfigStore = DpisConfigStore(legacyPreferences())

    @JvmStatic
    fun createSystemServer(): DpisConfigStore = create()

    @JvmStatic
    fun create(packageName: String?): DpisConfigStore =
        create(packageName, RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.NONE)

    @JvmStatic
    fun createMainProcess(packageName: String?): DpisConfigStore =
        create(packageName, RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET)

    private fun create(
        packageName: String?,
        route: RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute,
    ): DpisConfigStore {
        val legacy = legacyPreferences()
        if (packageName.isNullOrBlank()) return DpisConfigStore(legacy)
        return DpisConfigStore(RuntimePropertyConfigPreferences(packageName, route), legacy)
    }

    private fun legacyPreferences(): SharedPreferences =
        XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpisConfigStore.GROUP)
}
