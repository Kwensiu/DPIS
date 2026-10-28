package com.dpis.module.config

import android.content.SharedPreferences
import com.dpis.module.appconfig.AppConfigInput
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
    private val persistence: PackageConfigPersistence,
    private val ownerUserId: Int,
    private val identity: PackageIdentity? = null,
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
            AppConfigInput::fromLegacyScalePermille
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

    fun packageConfig(identity: PackageIdentity): PackageConfigValue {
        require(identity.userId == ownerUserId) { "identity owner does not match archive" }
        return PackageConfigReader(
            preferences,
            fallbackPreferences,
            persistence,
            ownerUserId,
            identity,
        ).packageConfig(identity.packageName)
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
        contains(scopedPackageKey(packageKey)) || contains(legacyKey) || contains(packageKey)

    private fun <T> read(key: String?, reader: (SharedPreferences) -> T): T? {
        val source = when {
            key != null && preferences.contains(key) -> preferences
            key != null && fallbackPreferences?.contains(key) == true -> fallbackPreferences
            else -> null
        } ?: return null
        return try { reader(source) } catch (_: ClassCastException) { null }
    }

    private fun packageNullableInt(legacyKey: String?, packageKey: String?): Int? =
        nullableInt(preferredPackageKey(legacyKey, packageKey))

    private fun nullableInt(key: String?): Int? = read(key) { it.getInt(key, 0) }

    private fun packageString(legacyKey: String?, packageKey: String?, default: String?): String? {
        val key = preferredPackageKey(legacyKey, packageKey)
        return read(key) { it.getString(key, default) } ?: default
    }

    private fun packageBoolean(legacyKey: String?, packageKey: String?, default: Boolean): Boolean {
        val key = preferredPackageKey(legacyKey, packageKey)
        return read(key) { it.getBoolean(key, default) } ?: default
    }

    private fun preferredPackageKey(legacyKey: String?, packageKey: String?): String? =
        scopedPackageKey(packageKey)?.takeIf(::contains)
            ?: legacyKey?.takeIf(::contains)
            ?: packageKey

    private fun scopedPackageKey(packageKey: String?): String? {
        if (packageKey == null) return null
        val spec = persistence.PACKAGE_AGGREGATED_CONFIG_KEYS.firstOrNull {
            it.packageNameFromStorageKey(packageKey, requireApplicablePackage = false) != null
        } ?: return null
        val parsedPackageName = spec.packageNameFromStorageKey(
            packageKey,
            requireApplicablePackage = false,
        ) ?: return null
        if (!PackageIdentity.isValidPackageName(parsedPackageName)) return null
        if (identity != null && identity.packageName != parsedPackageName) return null
        if (packageKey.endsWith(".app.wechat_dpi") && !WechatDpiConfig.appliesTo(parsedPackageName)) return null
        val prefix = "package_config.$parsedPackageName."
        val field = packageKey.removePrefix(prefix).takeIf { it.isNotEmpty() && it != packageKey }
            ?: return null
        return PackageIdentity(parsedPackageName, identity?.userId ?: ownerUserId)
            .packageConfigKey(field)
    }
}
