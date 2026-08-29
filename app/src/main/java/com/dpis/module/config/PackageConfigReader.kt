package com.dpis.module.config

import android.content.SharedPreferences
import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType

/** Reads the normalized package view from legacy and aggregated preference keys. */
internal class PackageConfigReader(
    private val preferences: SharedPreferences,
    private val fallbackPreferences: SharedPreferences?,
    private val persistence: PackageConfigPersistence
) {
    fun viewportWidth(packageName: String): Int? {
        val value = packageNullableInt(
            persistence.keyForViewportWidth(packageName),
            persistence.keyForPackageViewportWidth(packageName)
        )
        return persistence.normalizeViewportWidth(value)
    }

    fun viewportScaleMilliPercent(packageName: String): Int? {
        val key = persistence.keyForViewportScaleMilliPercent(packageName)
        val packageKey = persistence.keyForPackageViewportScaleMilliPercent(packageName)
        packageNullableInt(key, packageKey)?.let {
            return persistence.normalizeViewportScaleMilliPercent(it)
        }
        val legacy = packageNullableInt(
            persistence.keyForViewportScalePermille(packageName),
            persistence.keyForPackageViewportScalePermille(packageName)
        ) ?: return null
        return persistence.normalizeViewportScalePermille(legacy)?.let(
            AppConfigInputValidation::fromLegacyScalePermille
        )
    }

    fun viewportType(packageName: String): String {
        val key = persistence.keyForViewportTargetType(packageName)
        val packageKey = persistence.keyForPackageViewportTargetType(packageName)
        return if (containsPackageValue(key, packageKey)) {
            ViewportTargetType.normalize(packageString(key, packageKey, ViewportTargetType.OFF))
        } else ViewportTargetType.OFF
    }

    fun viewportSpec(packageName: String): ViewportTargetSpec {
        val type = viewportType(packageName)
        if (type == ViewportTargetType.RELATIVE_SCALE) {
            return viewportScaleMilliPercent(packageName)?.let(ViewportTargetSpec::relativeScale)
                ?: ViewportTargetSpec.off()
        }
        if (type == ViewportTargetType.ABSOLUTE_DP) {
            return viewportWidth(packageName)?.let(ViewportTargetSpec::absoluteDp)
                ?: ViewportTargetSpec.off()
        }
        return viewportWidth(packageName)?.let(ViewportTargetSpec::absoluteDp)
            ?: ViewportTargetSpec.off()
    }

    fun viewportApplyMode(packageName: String): String {
        val key = persistence.keyForViewportMode(packageName)
        val packageKey = persistence.keyForPackageViewportMode(packageName)
        if (containsPackageValue(key, packageKey)) {
            return ViewportApplyMode.normalize(packageString(key, packageKey, ViewportApplyMode.OFF))
        }
        if (viewportWidth(packageName) != null &&
            !containsPackageValue(
                persistence.keyForViewportTargetType(packageName),
                persistence.keyForPackageViewportTargetType(packageName)
            )
        ) return ViewportApplyMode.SYSTEM
        return if (viewportSpec(packageName).isEnabled()) ViewportApplyMode.AUTO
        else ViewportApplyMode.OFF
    }

    fun fontScalePercent(packageName: String): Int? = persistence.normalizeFontScalePercent(
        packageNullableInt(
            persistence.keyForFontScale(packageName),
            persistence.keyForPackageFontScale(packageName)
        )
    )

    fun typefaceId(packageName: String): String? = persistence.normalizeTypefaceId(
        packageString(
            persistence.keyForTypefaceId(packageName),
            persistence.keyForPackageTypefaceId(packageName),
            null
        )
    )

    fun fontHookDomains(packageName: String): String? =
        persistence.normalizeNonEmptyString(
            packageString(
                persistence.keyForFontHookDomains(packageName),
                persistence.keyForPackageFontHookDomains(packageName),
                null
            )
        )

    fun fontApplyMode(packageName: String): String {
        val key = persistence.keyForFontMode(packageName)
        val packageKey = persistence.keyForPackageFontMode(packageName)
        if (containsPackageValue(key, packageKey)) {
            return FontApplyMode.normalize(packageString(key, packageKey, FontApplyMode.OFF))
        }
        return if (fontScalePercent(packageName) != null) FontApplyMode.SYSTEM_EMULATION
        else FontApplyMode.OFF
    }

    fun wechatDpi(packageName: String?): Int? {
        if (!WechatDpiConfig.appliesTo(packageName)) return null
        return WechatDpiConfig.normalize(
            packageNullableInt(
                persistence.keyForWechatDpi(packageName!!),
                persistence.keyForPackageWechatDpi(packageName)
            )
        )
    }

    fun legacyWechatDpi(): Int? {
        if (!contains(persistence.LEGACY_WECHAT_DPI_KEY)) return null
        return WechatDpiConfig.normalize(
            nullableInt(persistence.LEGACY_WECHAT_DPI_KEY)
        )
    }

    fun packageConfig(packageName: String?): PackageConfigValue {
        if (packageName == null || packageName.isBlank()) return PackageConfigValue.EMPTY
        return PackageConfigValue(
            viewportSpec(packageName),
            viewportType(packageName),
            explicitViewportApplyMode(packageName),
            fontScalePercent(packageName),
            explicitFontApplyMode(packageName),
            typefaceId(packageName),
            fontHookDomains(packageName),
            if (containsPackageValue(
                    persistence.keyForDpisEnabled(packageName),
                    persistence.keyForPackageDpisEnabled(packageName)
                )
            ) packageBoolean(
                persistence.keyForDpisEnabled(packageName),
                persistence.keyForPackageDpisEnabled(packageName),
                true
            ) else null,
            wechatDpi(packageName)
        )
    }

    private fun explicitViewportApplyMode(packageName: String): String =
        if (containsPackageValue(
                persistence.keyForViewportMode(packageName),
                persistence.keyForPackageViewportMode(packageName)
            )
        ) ViewportApplyMode.normalize(
            packageString(
                persistence.keyForViewportMode(packageName),
                persistence.keyForPackageViewportMode(packageName),
                ViewportApplyMode.OFF
            )
        ) else ViewportApplyMode.OFF

    private fun explicitFontApplyMode(packageName: String): String =
        if (containsPackageValue(
                persistence.keyForFontMode(packageName),
                persistence.keyForPackageFontMode(packageName)
            )
        ) FontApplyMode.normalize(
            packageString(
                persistence.keyForFontMode(packageName),
                persistence.keyForPackageFontMode(packageName),
                FontApplyMode.OFF
            )
        ) else FontApplyMode.OFF

    fun templateConfig(packageName: String?): TemplateConfigValue {
        val config = packageConfig(packageName)
        return TemplateConfigValueAdapters.fromViewportTargetSpec(
            config.viewportTargetSpec(),
            config.viewportTargetType(),
            config.viewportApplyMode(),
            config.fontScalePercent(),
            config.fontApplyMode(),
            config.typefaceId(),
            config.fontHookDomainsRaw()
        )
    }

    private fun contains(key: String?): Boolean =
        key != null && (preferences.contains(key) || fallbackPreferences?.contains(key) == true)

    private fun containsPackageValue(legacyKey: String?, packageKey: String?): Boolean =
        contains(legacyKey) || contains(packageKey)

    private fun <T> read(key: String?, reader: (SharedPreferences) -> T): T? {
        val source = when {
            key != null && preferences.contains(key) -> preferences
            key != null && fallbackPreferences?.contains(key) == true -> fallbackPreferences
            else -> null
        } ?: return null
        return try { reader(source) } catch (_: ClassCastException) { null }
    }

    private fun packageNullableInt(legacyKey: String?, packageKey: String?): Int? =
        nullableInt(if (contains(legacyKey)) legacyKey else packageKey)

    private fun nullableInt(key: String?): Int? =
        read(key) { it.getInt(key, 0) }

    private fun packageString(legacyKey: String?, packageKey: String?, default: String?): String? =
        read(if (contains(legacyKey)) legacyKey else packageKey) {
            it.getString(if (contains(legacyKey)) legacyKey else packageKey, default)
        } ?: default

    private fun packageBoolean(legacyKey: String?, packageKey: String?, default: Boolean): Boolean =
        read(if (contains(legacyKey)) legacyKey else packageKey) {
            it.getBoolean(if (contains(legacyKey)) legacyKey else packageKey, default)
        } ?: default
}
