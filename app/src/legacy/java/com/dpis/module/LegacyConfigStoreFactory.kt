package com.dpis.module

import android.content.SharedPreferences
import android.os.Process
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.RuntimePropertyConfigPreferences
import com.dpis.module.fonts.FontLibraryStore
import com.dpis.module.runtime.XSharedPreferencesAdapter
import com.dpis.module.runtime.systemserver.selection.PackageUidResolver

/** Legacy-only owner of the classic Xposed preference compatibility boundary. */
object LegacyConfigStoreFactory {
    @JvmStatic
    fun createFontLibrary(): FontLibraryStore = FontLibraryStore(legacyPreferences(), null)

    @JvmStatic
    fun create(): DpisConfigStore = DpisConfigStore(legacyPreferences(), currentUserId())

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
        val ownerUserId = currentUserId()
        if (packageName.isNullOrBlank()) return DpisConfigStore(legacy, ownerUserId)
        val runtimePreferences = RuntimePropertyConfigPreferences(packageName, route, ownerUserId)
        return DpisConfigStore(runtimePreferences, legacy, null, runtimePreferences, ownerUserId)
    }

    private fun currentUserId(): Int =
        checkNotNull(PackageUidResolver.userIdFromUid(Process.myUid())) {
            "Cannot resolve Legacy runtime archive owner"
    }

    private fun legacyPreferences(): SharedPreferences =
        XSharedPreferencesAdapter(BuildConfig.APPLICATION_ID, DpisConfigStore.GROUP)
}
