package com.dpis.module.config

import android.content.SharedPreferences
import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType

/** Writes complete package and template values while keeping legacy keys mirrored. */
internal class PackageConfigWriter(
    private val persistence: PackageConfigPersistence,
    private val configuredPackages: () -> MutableSet<String?>,
    private val hasAnyAfterRemoving: (String?, Array<out String?>) -> Boolean,
    private val commit: (SharedPreferences.Editor.() -> Unit) -> Boolean,
    private val primaryPreferences: SharedPreferences,
    private val legacyWechatDpi: () -> Int?,
    private val containsPrimary: (String?) -> Boolean,
    private val containsEffective: (String?) -> Boolean,
    private val readPrimaryValue: (PackageConfigKeySpec, String?) -> Any?
) {
    fun setViewportWidth(packageName: String, widthDp: Int): Boolean {
        val normalized = persistence.normalizeViewportWidth(widthDp) ?: return clearViewport(packageName)
        return commitWithConfiguredPackage(packageName) {
            persistence.removePackageViewportValueKeys(this, packageName)
            putString(persistence.keyForViewportTargetType(packageName), ViewportTargetType.ABSOLUTE_DP)
            putString(persistence.keyForPackageViewportTargetType(packageName), ViewportTargetType.ABSOLUTE_DP)
            putInt(persistence.keyForViewportWidth(packageName), normalized)
            putInt(persistence.keyForPackageViewportWidth(packageName), normalized)
        }
    }

    fun setViewportSpec(packageName: String, spec: ViewportTargetSpec?): Boolean {
        val normalized = spec ?: ViewportTargetSpec.off()
        if (!normalized.isEnabled()) return clearViewport(packageName)
        return commitWithConfiguredPackage(packageName) {
            persistence.removePackageViewportValueKeys(this, packageName)
            putString(persistence.keyForViewportTargetType(packageName), normalized.type())
            putString(persistence.keyForPackageViewportTargetType(packageName), normalized.type())
            if (normalized.isRelativeScale()) {
                putViewportScale(packageName, normalized.scaleMilliPercent())
            } else {
                putInt(persistence.keyForViewportWidth(packageName), normalized.absoluteWidthDp())
                putInt(persistence.keyForPackageViewportWidth(packageName), normalized.absoluteWidthDp())
            }
        }
    }

    fun setViewportTypeDraft(packageName: String, type: String?): Boolean {
        val normalized = ViewportTargetType.normalize(type)
        if (normalized == ViewportTargetType.OFF) return clearViewportTypeDraft(packageName)
        return commitWithConfiguredPackage(packageName) {
            putString(persistence.keyForViewportTargetType(packageName), normalized)
            if (normalized == ViewportTargetType.ABSOLUTE_DP) {
                putString(persistence.keyForPackageViewportTargetType(packageName), normalized)
            } else {
                // Relative scale is an editor draft until a complete package config persists it.
                remove(persistence.keyForPackageViewportTargetType(packageName))
            }
        }
    }

    fun clearViewportTypeDraft(packageName: String): Boolean =
        removePackageValuesWhenEmpty(
            packageName,
            arrayOf(
                persistence.keyForViewportTargetType(packageName),
                persistence.keyForPackageViewportTargetType(packageName),
            ),
        ) {
            remove(persistence.keyForViewportTargetType(packageName))
            remove(persistence.keyForPackageViewportTargetType(packageName))
        }

    fun setViewportWidthDraft(packageName: String?, widthDp: Int?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (widthDp != null && widthDp <= 0) return true
        val keys = arrayOf(persistence.keyForViewportWidth(packageName), persistence.keyForPackageViewportWidth(packageName))
        if (widthDp == null) return removePackageValuesWhenEmpty(packageName, keys) { keys.forEach(::remove) }
        val normalized = persistence.normalizeViewportWidth(widthDp) ?: return true
        return commitWithConfiguredPackage(packageName) {
            putInt(keys[0], normalized)
            putInt(keys[1], normalized)
        }
    }

    fun setViewportScaleDraft(packageName: String?, scaleMilliPercent: Int?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (scaleMilliPercent != null && scaleMilliPercent <= 0) return true
        val keys = arrayOf(
            persistence.keyForViewportScaleMilliPercent(packageName),
            persistence.keyForPackageViewportScaleMilliPercent(packageName),
            persistence.keyForViewportScalePermille(packageName),
            persistence.keyForPackageViewportScalePermille(packageName),
        )
        if (scaleMilliPercent == null) return removePackageValuesWhenEmpty(packageName, keys) { keys.forEach(::remove) }
        val normalized = persistence.normalizeViewportScaleMilliPercent(scaleMilliPercent) ?: return true
        return commitWithConfiguredPackage(packageName) { putViewportScale(packageName, normalized) }
    }

    fun clearViewport(packageName: String): Boolean {
        val keys = persistence.allViewportConfigKeysForPackage(packageName)
        return removePackageValuesWhenEmpty(packageName, keys) { keys.forEach(::remove) }
    }

    fun clearViewportValue(packageName: String): Boolean {
        val keys = arrayOf(
            persistence.keyForViewportWidth(packageName), persistence.keyForViewportScalePermille(packageName),
            persistence.keyForViewportScaleMilliPercent(packageName), persistence.keyForViewportMode(packageName),
            persistence.keyForPackageViewportWidth(packageName), persistence.keyForPackageViewportScalePermille(packageName),
            persistence.keyForPackageViewportScaleMilliPercent(packageName), persistence.keyForPackageViewportMode(packageName),
        )
        return removePackageValuesWhenEmpty(packageName, keys) { keys.forEach(::remove) }
    }

    fun setViewportApplyMode(packageName: String, mode: String?): Boolean {
        val normalized = ViewportApplyMode.normalize(mode)
        val keys = arrayOf(persistence.keyForViewportMode(packageName), persistence.keyForPackageViewportMode(packageName))
        if (!persistence.isConfiguredViewportModeValue(normalized)) {
            return removePackageValuesWhenEmpty(packageName, keys) { keys.forEach(::remove) }
        }
        return commitWithConfiguredPackage(packageName) {
            putString(keys[0], normalized)
            putString(keys[1], normalized)
        }
    }

    fun setFontScale(packageName: String, percent: Int): Boolean {
        val normalized = persistence.normalizeFontScalePercent(percent) ?: return clearFontScale(packageName)
        return commitWithConfiguredPackage(packageName) {
            putInt(persistence.keyForFontScale(packageName), normalized)
            putInt(persistence.keyForPackageFontScale(packageName), normalized)
        }
    }

    fun setTypeface(packageName: String, typefaceId: String?): Boolean {
        val normalized = persistence.normalizeTypefaceId(typefaceId) ?: return clearTypeface(packageName)
        return commitWithConfiguredPackage(packageName) {
            putString(persistence.keyForTypefaceId(packageName), normalized)
            putString(persistence.keyForPackageTypefaceId(packageName), normalized)
        }
    }

    fun setWechatDpi(packageName: String?, dpi: Int?): Boolean {
        if (!WechatDpiConfig.appliesTo(packageName)) return true
        val normalized = WechatDpiConfig.normalize(dpi) ?: return clearWechatDpi(packageName)
        val target = requireNotNull(packageName)
        return commitWithConfiguredPackage(target) {
            putInt(persistence.keyForWechatDpi(target), normalized)
            putInt(persistence.keyForPackageWechatDpi(target), normalized)
        }
    }

    fun setFontApplyMode(packageName: String, mode: String?): Boolean {
        val normalized = FontApplyMode.normalize(mode)
        val keys = arrayOf(persistence.keyForFontMode(packageName), persistence.keyForPackageFontMode(packageName))
        if (!persistence.isConfiguredFontModeValue(normalized)) {
            return removePackageValuesWhenEmpty(packageName, keys) { keys.forEach(::remove) }
        }
        return commitWithConfiguredPackage(packageName) {
            putString(keys[0], normalized)
            putString(keys[1], normalized)
        }
    }

    fun clearFontScale(packageName: String): Boolean =
        removePackageValuesWhenEmpty(packageName, arrayOf(
            persistence.keyForFontScale(packageName), persistence.keyForPackageFontScale(packageName),
        )) {
            remove(persistence.keyForFontScale(packageName))
            remove(persistence.keyForPackageFontScale(packageName))
        }

    fun clearTypeface(packageName: String): Boolean =
        removePackageValuesWhenEmpty(packageName, arrayOf(
            persistence.keyForTypefaceId(packageName), persistence.keyForPackageTypefaceId(packageName),
        )) {
            remove(persistence.keyForTypefaceId(packageName))
            remove(persistence.keyForPackageTypefaceId(packageName))
        }

    fun clearWechatDpi(packageName: String?): Boolean {
        if (!WechatDpiConfig.appliesTo(packageName)) return true
        val target = requireNotNull(packageName)
        return removePackageValuesWhenEmpty(target, arrayOf(
            persistence.keyForWechatDpi(target), persistence.keyForPackageWechatDpi(target),
        )) {
            remove(persistence.keyForWechatDpi(target))
            remove(persistence.keyForPackageWechatDpi(target))
        }
    }

    fun setDpisEnabled(packageName: String, enabled: Boolean): Boolean {
        val keys = arrayOf(persistence.keyForDpisEnabled(packageName), persistence.keyForPackageDpisEnabled(packageName))
        if (enabled) return removePackageValuesWhenEmpty(packageName, keys) { keys.forEach(::remove) }
        return commitWithConfiguredPackage(packageName) {
            putBoolean(keys[0], false)
            putBoolean(keys[1], false)
        }
    }

    fun clearPackageConfig(packageName: String?): Boolean {
        val packages = LinkedHashSet(configuredPackages()).apply { remove(packageName) }
        return commit {
            putStringSet(persistence.KEY_TARGET_PACKAGES, packages)
            persistence.removePackageConfigKeys(this, packageName, persistence.PACKAGE_CONFIG_KEYS)
            persistence.removePackageConfigKeys(this, packageName, persistence.PACKAGE_AGGREGATED_CONFIG_KEYS)
        }
    }

    fun pruneDefaultPackage(packageName: String?, isEnabled: Boolean): Boolean {
        if (packageName.isNullOrBlank() || !isEnabled || hasAnyAfterRemoving(packageName, emptyArray())) return true
        return removePackageValuesWhenEmpty(packageName, arrayOf(
            persistence.keyForDpisEnabled(packageName), persistence.keyForPackageDpisEnabled(packageName),
        )) {
            remove(persistence.keyForDpisEnabled(packageName))
            remove(persistence.keyForPackageDpisEnabled(packageName))
        }
    }

    fun setFontHookDomains(packageName: String?, rawValue: String?): Boolean {
        if (packageName.isNullOrBlank() || rawValue == null) return false
        return commitWithConfiguredPackage(packageName) {
            putString(persistence.keyForFontHookDomains(packageName), rawValue)
            putString(persistence.keyForPackageFontHookDomains(packageName), rawValue)
        }
    }

    fun clearFontHookDomains(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return removePackageValuesWhenEmpty(packageName, arrayOf(
            persistence.keyForFontHookDomains(packageName), persistence.keyForPackageFontHookDomains(packageName),
        )) {
            remove(persistence.keyForFontHookDomains(packageName))
            remove(persistence.keyForPackageFontHookDomains(packageName))
        }
    }

    fun migrateLegacyWechatDpi(): Boolean {
        val legacy = legacyWechatDpi()
        val target = WechatDpiConfig.PACKAGE_NAME
        val officialKey = persistence.keyForWechatDpi(target)
        if (legacy == null || containsEffective(officialKey)) {
            return if (containsEffective(persistence.LEGACY_WECHAT_DPI_KEY)) commit { remove(persistence.LEGACY_WECHAT_DPI_KEY) } else true
        }
        return commitWithConfiguredPackage(target) {
            remove(persistence.LEGACY_WECHAT_DPI_KEY)
            putInt(officialKey, legacy)
            putInt(persistence.keyForPackageWechatDpi(target), legacy)
        }
    }

    fun migrateLegacyPackageConfigToAggregated(): Boolean {
        val packages = persistence.collectLegacyPackageConfigNames(primaryPreferences.all)
        if (packages.isEmpty()) return true
        return commit {
            packages.forEach { packageName ->
                packageName ?: return@forEach
                persistence.PACKAGE_CONFIG_KEYS.indices.forEach { index ->
                    val legacySpec = persistence.PACKAGE_CONFIG_KEYS[index]
                    val packageSpec = persistence.PACKAGE_AGGREGATED_CONFIG_KEYS[index]
                    val legacyKey = legacySpec.keyForPackage(packageName)
                    val packageKey = packageSpec.keyForPackage(packageName)
                    val value = persistence.normalizeLegacyPackageConfigValue(legacyKey, readPrimaryValue(legacySpec, legacyKey))
                    if (legacySpec.appliesTo(packageName) && value != null && !primaryPreferences.contains(packageKey)) {
                        persistence.putTypedValue(this, packageKey, value)
                    }
                }
                persistence.removePackageConfigKeys(this, packageName, persistence.PACKAGE_CONFIG_KEYS)
            }
        }
    }

    /** Seeds only missing legacy viewport values during the one-time bootstrap. */
    fun ensureSeedConfig(seedTargetViewportWidthDps: MutableMap<String?, Int?>): Boolean {
        val packages = LinkedHashSet(configuredPackages()).apply { addAll(seedTargetViewportWidthDps.keys) }
        return commit {
            putStringSet(persistence.KEY_TARGET_PACKAGES, packages)
            seedTargetViewportWidthDps.forEach { (packageName, widthDp) ->
                if (packageName != null && widthDp != null) {
                    val key = persistence.keyForViewportWidth(packageName)
                    if (!containsPrimary(key)) putInt(key, widthDp)
                }
            }
        }
    }

    fun writePackageConfig(packageName: String?, value: PackageConfigValue?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val normalized = value ?: PackageConfigValue.EMPTY
        val packages = LinkedHashSet(configuredPackages())
        if (normalized.hasAnyValue()) packages.add(packageName)
        else if (!hasAnyAfterRemoving(packageName, persistence.packageConfigKeysForPackage(packageName))) packages.remove(packageName)
        return commit {
            putStringSet(persistence.KEY_TARGET_PACKAGES, packages)
            persistence.removePackageConfigKeys(this, packageName, persistence.PACKAGE_CONFIG_KEYS)
            persistence.removePackageConfigKeys(this, packageName, persistence.PACKAGE_AGGREGATED_CONFIG_KEYS)
            writeCompleteValue(packageName, normalized, requireConfiguredModes = true, includeAppSpecific = true)
        }
    }

    fun writeTemplateConfig(packageName: String?, value: TemplateConfigValue?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val normalized = value ?: TemplateConfigValue.EMPTY
        val copyable = persistence.packageConfigValueFromTemplateConfigValue(normalized)
        val packages = LinkedHashSet(configuredPackages())
        if (!normalized.hasAnyValue()) {
            if (hasAnyAfterRemoving(packageName, persistence.allTemplateConfigKeysForPackage(packageName))) packages.add(packageName)
            else packages.remove(packageName)
            return commit {
                putStringSet(persistence.KEY_TARGET_PACKAGES, packages)
                persistence.removePackageTemplateConfigKeys(this, packageName)
                persistence.removePackageAggregatedTemplateConfigKeys(this, packageName)
            }
        }
        packages.add(packageName)
        return commit {
            putStringSet(persistence.KEY_TARGET_PACKAGES, packages)
            persistence.removePackageTemplateConfigKeys(this, packageName)
            persistence.removePackageAggregatedTemplateConfigKeys(this, packageName)
            writeCompleteValue(packageName, copyable, requireConfiguredModes = false, includeAppSpecific = false)
        }
    }

    private fun SharedPreferences.Editor.writeCompleteValue(
        packageName: String,
        value: PackageConfigValue,
        requireConfiguredModes: Boolean,
        includeAppSpecific: Boolean
    ) {
        val viewport = value.viewportTargetSpec()
        if (viewport.isEnabled()) {
            putString(persistence.keyForViewportTargetType(packageName), viewport.type())
            putString(persistence.keyForPackageViewportTargetType(packageName), viewport.type())
            if (viewport.isRelativeScale()) {
                val scale = viewport.scaleMilliPercent()
                putInt(persistence.keyForViewportScaleMilliPercent(packageName), scale)
                putInt(persistence.keyForPackageViewportScaleMilliPercent(packageName), scale)
                val legacy = AppConfigInputValidation.toLegacyScalePermille(scale)
                putInt(persistence.keyForViewportScalePermille(packageName), legacy)
                putInt(persistence.keyForPackageViewportScalePermille(packageName), legacy)
            } else {
                putInt(persistence.keyForViewportWidth(packageName), viewport.absoluteWidthDp())
                putInt(persistence.keyForPackageViewportWidth(packageName), viewport.absoluteWidthDp())
            }
        } else if (value.viewportTargetType() != ViewportTargetType.OFF) {
            putString(persistence.keyForViewportTargetType(packageName), value.viewportTargetType())
            putString(persistence.keyForPackageViewportTargetType(packageName), value.viewportTargetType())
        }
        val viewportMode = value.viewportApplyMode()
        if (if (requireConfiguredModes) persistence.isConfiguredViewportModeValue(viewportMode) else ViewportApplyMode.isEnabled(viewportMode)) {
            putString(persistence.keyForViewportMode(packageName), viewportMode)
            putString(persistence.keyForPackageViewportMode(packageName), viewportMode)
        }
        value.fontScalePercent()?.let {
            putInt(persistence.keyForFontScale(packageName), it)
            putInt(persistence.keyForPackageFontScale(packageName), it)
        }
        val fontMode = value.fontApplyMode()
        if (if (requireConfiguredModes) persistence.isConfiguredFontModeValue(fontMode) else FontApplyMode.isEnabled(fontMode)) {
            putString(persistence.keyForFontMode(packageName), fontMode)
            putString(persistence.keyForPackageFontMode(packageName), fontMode)
        }
        value.typefaceId()?.let {
            putString(persistence.keyForTypefaceId(packageName), it)
            putString(persistence.keyForPackageTypefaceId(packageName), it)
        }
        value.fontHookDomainsRaw()?.let {
            putString(persistence.keyForFontHookDomains(packageName), it)
            putString(persistence.keyForPackageFontHookDomains(packageName), it)
        }
        if (includeAppSpecific && value.dpisEnabled() == false) {
            putBoolean(persistence.keyForDpisEnabled(packageName), false)
            putBoolean(persistence.keyForPackageDpisEnabled(packageName), false)
        }
        val wechatDpi = value.wechatDpi()
        if (includeAppSpecific && wechatDpi != null && WechatDpiConfig.appliesTo(packageName)) {
            putInt(persistence.keyForWechatDpi(packageName), wechatDpi)
            putInt(persistence.keyForPackageWechatDpi(packageName), wechatDpi)
        }
    }

    private fun commitWithConfiguredPackage(
        packageName: String,
        action: SharedPreferences.Editor.() -> Unit
    ): Boolean {
        val packages = LinkedHashSet(configuredPackages()).apply { add(packageName) }
        return commit {
            putStringSet(persistence.KEY_TARGET_PACKAGES, packages)
            action()
        }
    }

    private fun removePackageValuesWhenEmpty(
        packageName: String,
        removedKeys: Array<out String?>,
        action: SharedPreferences.Editor.() -> Unit
    ): Boolean {
        val packages = LinkedHashSet(configuredPackages())
        if (!hasAnyAfterRemoving(packageName, removedKeys)) packages.remove(packageName)
        return commit {
            putStringSet(persistence.KEY_TARGET_PACKAGES, packages)
            action()
        }
    }

    private fun SharedPreferences.Editor.putViewportScale(packageName: String, scaleMilliPercent: Int) {
        putInt(persistence.keyForViewportScaleMilliPercent(packageName), scaleMilliPercent)
        putInt(persistence.keyForPackageViewportScaleMilliPercent(packageName), scaleMilliPercent)
        val legacyPermille = AppConfigInputValidation.toLegacyScalePermille(scaleMilliPercent)
        putInt(persistence.keyForViewportScalePermille(packageName), legacyPermille)
        putInt(persistence.keyForPackageViewportScalePermille(packageName), legacyPermille)
    }
}
