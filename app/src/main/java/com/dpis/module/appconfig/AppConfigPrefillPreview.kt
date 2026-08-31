package com.dpis.module.appconfig

import android.content.Context
import com.dpis.module.DpisConfigStore
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.PackageConfigRepository
import com.dpis.module.templates.GlobalPrefillStore
import com.dpis.module.templates.TemplateConfigValue

/** Resolves the global prefill preview applied to an app editor without exposing its storage. */
object AppConfigPrefillPreview {
    @JvmStatic fun resolveForEditor(
        context: Context?,
        item: AppListItem?,
        store: DpisConfigStore?,
    ): AppListItem? {
        if (context == null) return item
        val globalPrefill = GlobalPrefillStore(
            context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE),
        ).read()
        return applyIfEligible(item, store, globalPrefill)
    }

    @JvmStatic fun applyIfEligible(
        item: AppListItem?,
        store: DpisConfigStore?,
        globalPrefill: TemplateConfigValue?,
    ): AppListItem? = if (store == null) item else applyIfEligible(
        item,
        PackageConfigRepository(store),
        globalPrefill,
    )

    private fun applyIfEligible(
        item: AppListItem?,
        packageConfigRepository: PackageConfigRepository?,
        globalPrefill: TemplateConfigValue?,
    ): AppListItem? {
        if (item == null || packageConfigRepository == null || globalPrefill?.hasAnyValue() != true) {
            return item
        }
        return if (packageConfigRepository.hasRealPackageConfig(item.packageName)) item
        else item.withGlobalPrefillPreview(globalPrefill)
    }
}
