package com.dpis.module.appconfig

import com.dpis.module.DpisConfigStore
import com.dpis.module.R
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.PackageConfigValue
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.EffectiveModeResolver
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import com.google.android.material.textfield.TextInputEditText

class AppConfigSaveHandler {
    fun save(
        item: AppListItem,
        viewportInput: TextInputEditText,
        fontScaleInput: TextInputEditText,
        viewportTargetType: String?,
        currentViewportApplyMode: String?,
        viewportApplyModeResetRequested: Boolean,
        fontMode: String?,
        selectedTypefaceId: String?,
        draftFontHookDomainsRaw: String?,
        fontHookDomainsResetRequested: Boolean,
        viewportScaleInput: String?,
        viewportAbsoluteInput: String?,
        systemHooksEnabled: Boolean,
        store: DpisConfigStore?,
        onChanged: Runnable?
    ): Result {
        try {
            val viewportTargetSpec: ViewportTargetSpec? = parseViewportTargetSpecOrNull(
                viewportInput, viewportTargetType
            )
            val fontScalePercent: Int? = parseFontScalePercentOrNull(fontScaleInput)
            if (store == null) {
                return Result.Companion.failure(R.string.status_save_requires_init)
            }
            return saveResolved(
                item,
                viewportTargetSpec,
                viewportTargetType,
                currentViewportApplyMode,
                viewportApplyModeResetRequested,
                fontScalePercent,
                fontMode,
                selectedTypefaceId,
                draftFontHookDomainsRaw,
                fontHookDomainsResetRequested,
                viewportScaleInput,
                viewportAbsoluteInput,
                systemHooksEnabled,
                store,
                onChanged
            )
        } catch (exception: NumberFormatException) {
            return Result.Companion.failure(R.string.status_save_invalid)
        }
    }

    fun saveResolved(
        item: AppListItem,
        viewportTargetSpec: ViewportTargetSpec?,
        viewportTargetType: String?,
        currentViewportApplyMode: String?,
        viewportApplyModeResetRequested: Boolean,
        fontScalePercent: Int?,
        fontMode: String?,
        selectedTypefaceId: String?,
        draftFontHookDomainsRaw: String?,
        fontHookDomainsResetRequested: Boolean,
        viewportScaleInput: String?,
        viewportAbsoluteInput: String?,
        systemHooksEnabled: Boolean,
        store: DpisConfigStore?,
        onChanged: Runnable?
    ): Result {
        if (store == null) {
            return Result.Companion.failure(R.string.status_save_requires_init)
        }
        try {
            val originalPackageConfig = store.readPackageConfig(item.packageName)
            if (isUnchangedGlobalPrefillPreview(
                    item,
                    viewportTargetSpec,
                    currentViewportApplyMode,
                    fontScalePercent,
                    fontMode,
                    selectedTypefaceId,
                    draftFontHookDomainsRaw,
                    fontHookDomainsResetRequested
                )
            ) {
                val cleared = store.clearTargetPackageConfig(item.packageName)
                if (cleared && onChanged != null) {
                    onChanged.run()
                }
                return Result.Companion.success(0)
            }
            val viewportApplyMode: String? = resolveViewportApplyModeForSave(
                store, item.packageName, currentViewportApplyMode,
                viewportApplyModeResetRequested, viewportTargetSpec
            )
            val viewportEmulationIneffective =
                viewportTargetSpec != null && viewportTargetSpec.isEnabled()
                        && ViewportApplyMode.SYSTEM == ViewportApplyMode.normalize(viewportApplyMode)
                        && (ViewportApplyMode.SYSTEM != EffectiveModeResolver.resolveViewportMode(
                    viewportApplyMode,
                    systemHooksEnabled
                ))
            val fontEmulationIneffective =
                fontScalePercent != null && FontApplyMode.SYSTEM_EMULATION == FontApplyMode.normalize(
                    fontMode
                )
                        && (FontApplyMode.SYSTEM_EMULATION != EffectiveModeResolver.resolveFontMode(
                    fontMode,
                    systemHooksEnabled
                ))
            val emulationRequestedWithoutSystemScope =
                viewportEmulationIneffective || fontEmulationIneffective
            var saved = true
            var hint = 0
            saved = persistPreviewOnlyConfig(
                store, item, draftFontHookDomainsRaw, fontHookDomainsResetRequested
            )
                    && saved
            if (viewportTargetSpec == null || !viewportTargetSpec.isEnabled()) {
                saved = store.clearTargetViewportValue(item.packageName) && saved
                saved = store.setTargetViewportTypeDraft(
                    item.packageName,
                    ConfigDraftSaveSemantics.viewportTargetTypeForSave(viewportTargetType)
                )
                        && saved
                saved = store.setTargetViewportApplyMode(item.packageName, ViewportApplyMode.OFF)
                        && saved
            } else {
                saved = store.setTargetViewportSpec(item.packageName, viewportTargetSpec) && saved
                saved = saveInactiveViewportDraft(
                    store,
                    item.packageName,
                    viewportTargetSpec,
                    viewportScaleInput,
                    viewportAbsoluteInput
                ) && saved
                saved = store.setTargetViewportApplyMode(item.packageName, viewportApplyMode)
                        && saved
            }
            if (fontScalePercent == null) {
                saved = store.clearTargetFontScalePercent(item.packageName) && saved
                saved = store.setTargetFontApplyMode(
                    item.packageName,
                    ConfigDraftSaveSemantics.fontApplyModeForSave(fontMode)
                ) && saved
            } else {
                saved = store.setTargetFontScalePercent(item.packageName, fontScalePercent) && saved
                saved = store.setTargetFontApplyMode(item.packageName, fontMode) && saved
            }
            if (selectedTypefaceId == null || selectedTypefaceId.isBlank()) {
                saved = store.clearTargetTypefaceId(item.packageName) && saved
            } else {
                saved = store.setTargetTypefaceId(item.packageName, selectedTypefaceId) && saved
            }
            publishFontHookDomainsAfterSave(item.packageName, store)
            if (isDefaultPackageState(item, store)) {
                // A disabled target type or mode can be left behind by older saves. These
                // values have no runtime effect, but they must not keep the app in the
                // configured-app list after the user resets every visible field.
                saved = store.clearTargetPackageConfig(item.packageName) && saved
            } else {
                saved = store.prunePackageIfOnlyDefaultConfigRemains(item.packageName) && saved
            }
            if (!saved) {
                return Result.Companion.failure(R.string.system_settings_save_failed)
            }
            val expectedPackageConfig: PackageConfigValue = expectedPackageConfigAfterSave(
                viewportTargetSpec,
                viewportTargetType,
                viewportApplyMode,
                fontScalePercent,
                fontMode,
                selectedTypefaceId,
                draftFontHookDomainsRaw,
                fontHookDomainsResetRequested,
                originalPackageConfig
            )
            if (!didPersistExpectedPackageConfig(
                    store,
                    item.packageName,
                    expectedPackageConfig
                )
            ) {
                return Result.Companion.failure(R.string.system_settings_save_failed)
            }
            if (saved && onChanged != null) {
                onChanged.run()
            }
            if (saved && emulationRequestedWithoutSystemScope) {
                hint = R.string.system_mode_requires_system_scope_hint
            }
            return Result.Companion.success(hint)
        } catch (exception: NumberFormatException) {
            return Result.Companion.failure(R.string.status_save_invalid)
        }
    }

    class Result private constructor(@JvmField val success: Boolean, @JvmField val messageResId: Int) {
        companion object {
            @JvmStatic
            fun success(messageResId: Int): Result {
                return Result(true, messageResId)
            }

            @JvmStatic
            fun failure(messageResId: Int): Result {
                return Result(false, messageResId)
            }
        }
    }

    private class ViewportDraftValue(val valid: Boolean, val value: Int?) {
        companion object {
            fun valid(value: Int?): ViewportDraftValue {
                return ViewportDraftValue(true, value)
            }

            fun invalid(): ViewportDraftValue {
                return ViewportDraftValue(false, null)
            }
        }
    }

    companion object {
        @JvmStatic
        fun persistPreviewOnlyConfig(
            store: DpisConfigStore?,
            item: AppListItem?,
            draftFontHookDomainsRaw: String?,
            fontHookDomainsResetRequested: Boolean
        ): Boolean {
            if (store == null || item == null) {
                return true
            }
            if (fontHookDomainsResetRequested) {
                return HookDomainOverrideStore(store).restoreRecommended(item.packageName)
            }
            if (draftFontHookDomainsRaw == null) {
                return true
            }
            val normalizedRaw = FontHookDomainPresentation
                .forRecommendedTemplateRaw(draftFontHookDomainsRaw)
                .normalizedRawOrNull()
            if (normalizedRaw == null) {
                return HookDomainOverrideStore(store).restoreRecommended(item.packageName)
            }
            return store.setPackageFontHookDomainsRaw(item.packageName, normalizedRaw)
        }

        fun isUnchangedGlobalPrefillPreview(
            item: AppListItem?,
            viewportTargetSpec: ViewportTargetSpec?,
            viewportApplyMode: String?,
            fontScalePercent: Int?,
            fontMode: String?,
            selectedTypefaceId: String?,
            draftFontHookDomainsRaw: String?,
            fontHookDomainsResetRequested: Boolean
        ): Boolean {
            if (item == null || !item.previewFromGlobalPrefill) {
                return false
            }
            if (isResetPreviewDraft(
                    viewportTargetSpec, fontScalePercent,
                    selectedTypefaceId, draftFontHookDomainsRaw, fontHookDomainsResetRequested
                )
            ) {
                return true
            }
            val normalizedSpec = if (viewportTargetSpec != null)
                viewportTargetSpec
            else
                ViewportTargetSpec.off()
            val current = TemplateConfigValueAdapters.fromViewportTargetSpec(
                normalizedSpec,
                ViewportTargetType.normalize(item.viewportTargetType),
                if (normalizedSpec.isEnabled())
                    ViewportApplyMode.normalize(viewportApplyMode)
                else
                    ViewportApplyMode.OFF,
                fontScalePercent,
                ConfigDraftSaveSemantics.fontApplyModeForSave(fontMode),
                selectedTypefaceId,
                draftFontHookDomainsRaw
            )
            val preview = TemplateConfigValueAdapters.fromViewportTargetSpec(
                item.viewportTargetSpec,
                item.viewportTargetType,
                if (item.viewportTargetSpec.isEnabled())
                    item.viewportMode
                else
                    ViewportApplyMode.OFF,
                item.fontScalePercent,
                item.fontMode,
                item.typefaceId,
                item.previewFontHookDomainsRaw
            )
            return current == preview
        }

        private fun isResetPreviewDraft(
            viewportTargetSpec: ViewportTargetSpec?,
            fontScalePercent: Int?,
            selectedTypefaceId: String?,
            draftFontHookDomainsRaw: String?,
            fontHookDomainsResetRequested: Boolean
        ): Boolean {
            return fontHookDomainsResetRequested
                    && (viewportTargetSpec == null || !viewportTargetSpec.isEnabled())
                    && fontScalePercent == null && normalizeNullableString(selectedTypefaceId) == null && normalizeNullableString(
                draftFontHookDomainsRaw
            ) == null
        }

        private fun expectedPackageConfigAfterSave(
            viewportTargetSpec: ViewportTargetSpec?,
            viewportTargetType: String?,
            viewportApplyMode: String?,
            fontScalePercent: Int?,
            fontMode: String?,
            selectedTypefaceId: String?,
            draftFontHookDomainsRaw: String?,
            fontHookDomainsResetRequested: Boolean,
            originalPackageConfig: PackageConfigValue?
        ): PackageConfigValue {
            val original = if (originalPackageConfig != null)
                originalPackageConfig
            else
                PackageConfigValue.EMPTY
            val normalizedViewportSpec = if (viewportTargetSpec != null)
                viewportTargetSpec
            else
                ViewportTargetSpec.off()
            var savedViewportType = if (normalizedViewportSpec.isEnabled())
                normalizedViewportSpec.type()
            else
                ViewportTargetType.OFF
            var savedViewportMode = ConfigDraftSaveSemantics.viewportApplyModeForSave(
                viewportApplyMode,
                normalizedViewportSpec
            )
            var savedFontMode = if (fontScalePercent != null)
                FontApplyMode.normalize(fontMode)
            else
                ConfigDraftSaveSemantics.fontApplyModeForSave(fontMode)
            if (ViewportTargetType.ABSOLUTE_DP != savedViewportType) {
                savedViewportType = ViewportTargetType.OFF
            }
            if (ViewportApplyMode.SYSTEM != savedViewportMode && ViewportApplyMode.COMPAT != savedViewportMode) {
                savedViewportMode = ViewportApplyMode.OFF
            }
            if (FontApplyMode.FIELD_REWRITE != savedFontMode) {
                savedFontMode = FontApplyMode.OFF
            }
            return PackageConfigValue(
                normalizedViewportSpec,
                savedViewportType,
                savedViewportMode,
                fontScalePercent,
                savedFontMode,
                selectedTypefaceId,
                expectedFontHookDomainsRawAfterSave(
                    draftFontHookDomainsRaw,
                    fontHookDomainsResetRequested,
                    original.fontHookDomainsRaw()
                ),
                original.dpisEnabled(),
                original.wechatDpi()
            )
        }

        private fun expectedFontHookDomainsRawAfterSave(
            draftFontHookDomainsRaw: String?,
            fontHookDomainsResetRequested: Boolean,
            currentFontHookDomainsRaw: String?
        ): String? {
            if (fontHookDomainsResetRequested) {
                return null
            }
            if (draftFontHookDomainsRaw == null) {
                return currentFontHookDomainsRaw
            }
            val presentation = FontHookDomainPresentation
                .forRecommendedTemplateRaw(draftFontHookDomainsRaw)
            return presentation.normalizedRawOrNull()
        }

        private fun didPersistExpectedPackageConfig(
            store: DpisConfigStore?,
            packageName: String?,
            expectedPackageConfig: PackageConfigValue?
        ): Boolean {
            if (store == null || packageName == null || packageName.isBlank()) {
                return false
            }
            val expected = if (expectedPackageConfig != null)
                expectedPackageConfig
            else
                PackageConfigValue.EMPTY
            val actual: PackageConfigValue = normalizePersistedPackageConfigForVerification(
                store.readPackageConfig(packageName)
            )
            if (expected != actual) {
                return false
            }
            return if (expected.hasAnyValue())
                store.hasRealPackageConfig(packageName)
            else
                !store.hasRealPackageConfig(packageName)
        }

        private fun normalizePersistedPackageConfigForVerification(
            value: PackageConfigValue?
        ): PackageConfigValue {
            val actual = if (value != null) value else PackageConfigValue.EMPTY
            val viewportTargetType =
                if (ViewportTargetType.ABSOLUTE_DP == actual.viewportTargetType())
                    actual.viewportTargetType()
                else
                    ViewportTargetType.OFF
            val viewportApplyMode = if (ViewportApplyMode.SYSTEM == actual.viewportApplyMode()
                || ViewportApplyMode.COMPAT == actual.viewportApplyMode()
            )
                actual.viewportApplyMode()
            else
                ViewportApplyMode.OFF
            val fontApplyMode = if (FontApplyMode.FIELD_REWRITE == actual.fontApplyMode())
                actual.fontApplyMode()
            else
                FontApplyMode.OFF
            return PackageConfigValue(
                actual.viewportTargetSpec(),
                viewportTargetType,
                viewportApplyMode,
                actual.fontScalePercent(),
                fontApplyMode,
                actual.typefaceId(),
                actual.fontHookDomainsRaw(),
                actual.dpisEnabled(),
                actual.wechatDpi()
            )
        }

        private fun publishFontHookDomainsAfterSave(packageName: String?, store: DpisConfigStore?) {
            val override = HookDomainOverrideStore(store).read(packageName)
            if (override.customPathEnabled) {
                FontHookDomainPropertySyncer.publishTargetAsync(
                    packageName,
                    override.enabledKnownDomains
                )
                return
            }
            FontHookDomainPropertySyncer.clearTargetAsync(packageName)
        }

        private fun isDefaultPackageState(
            item: AppListItem?,
            store: DpisConfigStore?
        ): Boolean {
            if (item == null || store == null || !store.isTargetDpisEnabled(item.packageName)) {
                return false
            }
            return !store.getTargetViewportSpec(item.packageName)
                .isEnabled() && store.getTargetFontScalePercent(item.packageName) == null && (FontApplyMode.FIELD_REWRITE != FontApplyMode.normalize(
                store.getTargetFontApplyMode(item.packageName)
            )) && store.getTargetTypefaceId(item.packageName) == null && store.getTargetFontHookDomainsRaw(
                item.packageName
            ) == null && store.getWechatDpi(item.packageName) == null
        }

        @JvmStatic
        fun resolveViewportApplyModeForSave(
            store: DpisConfigStore?,
            packageName: String?,
            itemViewportMode: String?,
            viewportApplyModeResetRequested: Boolean,
            viewportTargetSpec: ViewportTargetSpec?
        ): String? {
            if (viewportTargetSpec == null || !viewportTargetSpec.isEnabled()) {
                return ViewportApplyMode.OFF
            }
            if (viewportApplyModeResetRequested) {
                return ViewportApplyMode.OFF
            }
            val draftMode = ViewportApplyMode.normalize(itemViewportMode)
            if (ViewportApplyMode.isEnabled(draftMode)) {
                return draftMode
            }
            val persistedMode = if (store != null)
                store.getTargetViewportApplyMode(packageName)
            else
                ViewportApplyMode.OFF
            return if (ViewportApplyMode.isEnabled(persistedMode))
                persistedMode
            else
                ViewportApplyMode.AUTO
        }

        private fun saveInactiveViewportDraft(
            store: DpisConfigStore?,
            packageName: String?,
            activeSpec: ViewportTargetSpec?,
            viewportScaleInput: String?,
            viewportAbsoluteInput: String?
        ): Boolean {
            if (store == null || packageName == null || packageName.isBlank()
                || activeSpec == null || !activeSpec.isEnabled()
            ) {
                return true
            }
            if (activeSpec.isRelativeScale()) {
                val draft: ViewportDraftValue = parseViewportWidthDraft(viewportAbsoluteInput)
                if (!draft.valid) {
                    return true
                }
                return store.setTargetViewportWidthDraft(
                    packageName, draft.value
                )
            }
            if (activeSpec.isAbsoluteDp()) {
                val draft: ViewportDraftValue =
                    parseViewportScaleMilliPercentDraft(viewportScaleInput)
                if (!draft.valid) {
                    return true
                }
                return store.setTargetViewportScaleMilliPercentDraft(
                    packageName, draft.value
                )
            }
            return true
        }

        private fun parseViewportWidthDraft(rawInput: String?): ViewportDraftValue {
            val raw = if (rawInput != null) rawInput.trim { it <= ' ' } else ""
            if (raw.isEmpty()) {
                return ViewportDraftValue.valid(null)
            }
            val value = AppConfigInputValidation.parsePositiveIntOrNull(raw)
            return if (value != null) ViewportDraftValue.valid(value) else ViewportDraftValue.invalid()
        }

        private fun parseViewportScaleMilliPercentDraft(rawInput: String?): ViewportDraftValue {
            val raw = if (rawInput != null) rawInput.trim { it <= ' ' } else ""
            if (raw.isEmpty()) {
                return ViewportDraftValue.valid(null)
            }
            val value = AppConfigInputValidation.parseViewportScaleMilliPercentOrNull(raw)
            return if (value != null) ViewportDraftValue.valid(value) else ViewportDraftValue.invalid()
        }

        @Throws(NumberFormatException::class)
        private fun parseViewportTargetSpecOrNull(
            inputView: TextInputEditText,
            viewportTargetType: String?
        ): ViewportTargetSpec? {
            val raw = if (inputView.getText() != null) inputView.getText().toString()
                .trim { it <= ' ' } else ""
            if (!AppConfigInputValidation.isViewportInputValid(raw, viewportTargetType)) {
                throw NumberFormatException("invalid viewport target")
            }
            val spec =
                AppConfigInputValidation.parseViewportTargetSpec(raw, viewportTargetType)
            return spec
        }

        @Throws(NumberFormatException::class)
        private fun parseFontScalePercentOrNull(inputView: TextInputEditText): Int? {
            val raw = if (inputView.getText() != null) inputView.getText().toString()
                .trim { it <= ' ' } else ""
            if (raw.isEmpty()) {
                return null
            }
            val value = AppConfigInputValidation.parseFontScalePercentOrNull(raw)
            if (value == null) {
                throw NumberFormatException("invalid font scale")
            }
            return value
        }

        private fun normalizeNullableString(value: String?): String? {
            if (value == null) {
                return null
            }
            val trimmed = value.trim { it <= ' ' }
            return if (trimmed.isEmpty()) null else trimmed
        }
    }
}
