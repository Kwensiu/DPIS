package com.dpis.module.appconfig.landdetail

import android.app.Activity
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.DpisApplication
import com.dpis.module.R
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder.AppConfigDialogState
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.SystemFontRegistry
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.viewport.ViewportApplyMode

/** User-visible landscape typeface and hook-chain labels. */
internal object LandAppDetailCopy {
    fun typefaceValue(activity: Activity, selectedTypefaceId: String?): String {
        val typefaceId = selectedTypefaceId?.takeIf { it.isNotBlank() }
            ?: return activity.getString(R.string.dialog_typeface_default)
        for (entry in SystemFontRegistry.listRecommendedFonts()) {
            if (typefaceId == entry.id()) {
                return entry.displayName()
            }
        }
        val imported = ConfigStoreFactory.createLocalUiFontLibraryStore(
            activity,
            DpisApplication.xposedService,
        ).findById(typefaceId)
        if (imported != null) {
            return imported.displayName ?: activity.getString(
                R.string.dialog_typeface_missing_named,
                typefaceId,
            )
        }
        return activity.getString(R.string.dialog_typeface_missing_named, typefaceId)
    }

    fun hookChainValue(
        activity: Activity,
        item: AppListItem?,
        state: AppConfigDialogState?,
    ): String {
        val parts = mutableListOf<String>()
        val viewportMode = ViewportApplyMode.normalize(state?.viewportApplyMode)
        if (ViewportApplyMode.SYSTEM == viewportMode) {
            parts += activity.getString(R.string.land_detail_hook_chain_viewport_system)
        } else if (ViewportApplyMode.COMPAT == viewportMode) {
            parts += activity.getString(R.string.land_detail_hook_chain_viewport_compat)
        }
        val hookDomains = FontHookDomainPresentation.forOverride(
            hookDomainOverride(activity, item, state),
            FontHookDomainRegistry.automaticCustomizableDomains(),
        )
        if (!hookDomains.displaysAsAutomatic()) {
            parts += activity.getString(
                R.string.land_detail_hook_chain_font_count,
                hookDomains.selectedDisplayCount(),
                hookDomains.totalDisplayCount(),
            )
        }
        if (parts.isEmpty()) {
            return activity.getString(R.string.land_detail_hook_chain_default)
        }
        return parts.joinToString(activity.getString(R.string.land_detail_hook_chain_separator))
    }

    private fun hookDomainOverride(
        activity: Activity?,
        item: AppListItem?,
        state: AppConfigDialogState?,
    ): HookDomainOverride {
        if (state != null && state.fontHookDomainsResetRequested) {
            return HookDomainOverride.automatic()
        }
        if (state != null && (state.previewFromGlobalPrefill || state.draftFontHookDomainsRaw != null)) {
            return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
                HookDomainOverrideStore.fromRaw(state.draftFontHookDomainsRaw),
                FontHookDomainRegistry.automaticCustomizableDomains(),
            )
        }
        val packageName = item?.packageName
        if (packageName.isNullOrBlank()) {
            return HookDomainOverride.automatic()
        }
        val store = DpisApplication.getActiveHookConfigStore(activity)
        return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
            HookDomainOverrideStore(store).read(packageName),
            FontHookDomainRegistry.automaticCustomizableDomains(),
        )
    }
}
