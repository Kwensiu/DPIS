package com.dpis.module.appconfig.presentation

import android.app.Activity
import com.dpis.module.DpisApplication
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigDialogStateModel
import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontLibraryEntry
import com.dpis.module.fonts.FontLibraryStore
import com.dpis.module.fonts.SystemFontRegistry
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.viewport.ViewportTargetType

/** Compose editor contracts: process actions, Activity host, and dialog state. */
class AppConfigDialogBinder(
    private val activity: Activity,
) {
    enum class ProcessAction {
        START,
        RESTART,
        STOP,
    }

    interface Host {
        fun toggleScope(
            item: AppListItem?,
            currentlyInScope: Boolean,
            onTurnedInScope: Runnable?,
            onTurnedOutScope: Runnable?,
        )

        fun getFontHookDomainsButtonText(
            item: AppListItem?,
            state: AppConfigDialogState?,
        ): String?

        fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean
    }

    fun typefaceSelectorText(selectedTypefaceId: String?): String {
        return activity.getString(
            R.string.dialog_typeface_selector_value,
            resolveTypefaceDisplayText(selectedTypefaceId, listFontLibraryEntries()),
        )
    }

    private fun resolveTypefaceDisplayText(
        selectedTypefaceId: String?,
        entries: List<FontLibraryEntry>,
    ): String? {
        if (selectedTypefaceId.isNullOrBlank()) {
            return activity.getString(R.string.dialog_typeface_default)
        }
        for (entry in SystemFontRegistry.listRecommendedFonts()) {
            if (selectedTypefaceId == entry.id()) {
                return entry.displayName()
            }
        }
        for (entry in entries) {
            if (selectedTypefaceId == entry.id) {
                return entry.displayName
            }
        }
        val displayId = selectedTypefaceId
        return activity.getString(R.string.dialog_typeface_missing_named, displayId)
    }

    private fun listFontLibraryEntries(): List<FontLibraryEntry> {
        return createFontLibraryStore().listFonts()
    }

    private fun createFontLibraryStore(): FontLibraryStore {
        return ConfigStoreFactory.createLocalUiFontLibraryStore(
            activity,
            DpisApplication.xposedService,
        )
    }

    class AppConfigDialogState(
        scopeSelected: Boolean,
        scopeKnown: Boolean,
        dpisEnabled: Boolean,
        previewFromGlobalPrefill: Boolean,
        packageName: String,
        draftFontHookDomainsRaw: String?,
        viewportApplyMode: String?,
        selectedTypefaceId: String?,
        initialViewportType: String?,
        initialViewportInput: String?,
        initialViewportScaleInput: String?,
        initialViewportAbsoluteInput: String?,
    ) : AppConfigDialogStateModel(
        scopeSelected,
        scopeKnown,
        dpisEnabled,
        previewFromGlobalPrefill,
        packageName,
        draftFontHookDomainsRaw,
        viewportApplyMode,
        selectedTypefaceId,
        initialViewportType,
        initialViewportInput,
        initialViewportScaleInput,
        initialViewportAbsoluteInput,
    ) {
        @JvmField
        var scopeRequestPending: Boolean = false

        companion object {
            @JvmStatic
            fun fromItem(item: AppListItem): AppConfigDialogState {
                val viewportInput =
                    AppConfigInputValidation.formatViewportInput(item.viewportTargetSpec)
                val viewportTargetType = initialViewportTargetType(item)
                val viewportScaleInput = if (item.viewportScaleMilliPercent != null) {
                    AppConfigInputValidation.formatScaleMilliPercentInput(item.viewportScaleMilliPercent)
                } else if (item.viewportTargetSpec.isRelativeScale) {
                    viewportInput
                } else {
                    ""
                }
                val viewportAbsoluteInput = if (item.viewportWidthDp != null) {
                    item.viewportWidthDp.toString()
                } else if (item.viewportTargetSpec.isAbsoluteDp) {
                    viewportInput
                } else {
                    ""
                }
                return AppConfigDialogState(
                    item.inScope,
                    item.scopeKnown,
                    item.dpisEnabled,
                    item.previewFromGlobalPrefill,
                    item.packageName,
                    item.effectiveFontHookDomainsRaw(),
                    item.viewportMode,
                    item.typefaceId,
                    viewportTargetType,
                    viewportInput,
                    viewportScaleInput,
                    viewportAbsoluteInput,
                )
            }

            private fun initialViewportTargetType(item: AppListItem): String {
                val spec = item.viewportTargetSpec
                if (!spec.isEnabled &&
                    ViewportTargetType.OFF != ViewportTargetType.normalize(item.viewportTargetType)
                ) {
                    return ViewportTargetType.normalize(item.viewportTargetType)
                }
                return AppConfigInputValidation.initialViewportTargetType(spec)
            }
        }
    }
}
