package com.dpis.module.config

import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType

/**
 * Describes one package-scoped preference key and the value shape that makes
 * the key count as configured state.
 */
fun interface PackageConfigKeyFactory {
    fun keyForPackage(packageName: String?): String?
}

class PackageConfigKeySpec(
    private val prefix: String,
    private val suffix: String,
    private val minIntValue: Int?,
    private val maxIntValue: Int?,
    private val requiredBooleanValue: Boolean?,
    private val keyFactory: (String) -> String,
    private val packagePredicate: (String) -> Boolean,
    private val configuredValuePredicate: ((Any?) -> Boolean)?
) {
    fun keyForPackage(packageName: String): String = keyFactory(packageName)

    fun packageNameFromKey(key: String, value: Any?): String? {
        val packageName = packageNameBetween(key, prefix, suffix)
        return if (packageName != null && appliesTo(packageName) && isConfiguredValue(value)) {
            packageName
        } else {
            null
        }
    }

    fun appliesTo(packageName: String?): Boolean =
        packageName != null && packagePredicate(packageName)

    fun packageNameFromStorageKey(key: String, requireApplicablePackage: Boolean): String? {
        val packageName = packageNameBetween(key, prefix, suffix) ?: return null
        return if (!requireApplicablePackage || appliesTo(packageName)) packageName else null
    }

    fun expectsInteger(): Boolean = minIntValue != null && maxIntValue != null

    fun expectsBoolean(): Boolean = requiredBooleanValue != null

    fun isConfiguredValue(value: Any?): Boolean {
        if (requiredBooleanValue != null) {
            return value is Boolean && requiredBooleanValue == value
        }
        if (configuredValuePredicate != null && !configuredValuePredicate(value)) {
            return false
        }
        if (minIntValue == null || maxIntValue == null) {
            return value != null
        }
        return value is Int && value >= minIntValue && value <= maxIntValue
    }

    companion object {
        fun any(prefix: String, suffix: String, keyFactory: (String) -> String) =
            PackageConfigKeySpec(prefix, suffix, null, null, null, keyFactory, { true }) { it != null }

        fun string(prefix: String, suffix: String, keyFactory: (String) -> String) =
            any(prefix, suffix, keyFactory)

        fun string(
            prefix: String,
            suffix: String,
            keyFactory: (String) -> String,
            configuredValuePredicate: (Any?) -> Boolean
        ) = PackageConfigKeySpec(
            prefix, suffix, null, null, null, keyFactory, { true }, configuredValuePredicate
        )

        fun positiveInteger(prefix: String, suffix: String, keyFactory: (String) -> String) =
            rangedInteger(prefix, suffix, 1, Int.MAX_VALUE, keyFactory)

        @JvmOverloads
        fun rangedInteger(
            prefix: String,
            suffix: String,
            minIntValue: Int,
            maxIntValue: Int,
            keyFactory: (String) -> String,
            packagePredicate: (String) -> Boolean = { true }
        ) = PackageConfigKeySpec(
            prefix, suffix, minIntValue, maxIntValue, null, keyFactory, packagePredicate
        ) { true }

        fun booleanValue(
            prefix: String,
            suffix: String,
            configuredValue: Boolean,
            keyFactory: (String) -> String
        ) = PackageConfigKeySpec(
            prefix, suffix, null, null, configuredValue, keyFactory, { true }
        ) { true }
    }
}

private fun packageNameBetween(key: String, prefix: String, suffix: String): String? {
    if (!key.startsWith(prefix) || !key.endsWith(suffix)) return null
    val packageName = key.substring(prefix.length, key.length - suffix.length)
    return packageName.takeUnless { it.isBlank() }
}

internal object PackageConfigRegistry {
    private const val MIN_FONT_SCALE_PERCENT = 50
    private const val MAX_FONT_SCALE_PERCENT = 300
    private val minScalePermille = ViewportTargetSpec.MIN_SCALE_PERMILLE
    private val maxScalePermille = ViewportTargetSpec.MAX_SCALE_PERMILLE
    private val minScaleMilliPercent = ViewportTargetSpec.MIN_SCALE_MILLI_PERCENT
    private val maxScaleMilliPercent = ViewportTargetSpec.MAX_SCALE_MILLI_PERCENT

    val legacyConfigKeys: Array<PackageConfigKeySpec> = arrayOf(
        PackageConfigKeySpec.positiveInteger("viewport.", ".width_dp", ::keyForViewportWidth),
        PackageConfigKeySpec.string("viewport.", ".target_type", ::keyForViewportTargetType, ::isAbsoluteTargetType),
        PackageConfigKeySpec.rangedInteger("viewport.", ".scale_permille", minScalePermille, maxScalePermille, ::keyForViewportScalePermille),
        PackageConfigKeySpec.rangedInteger("viewport.", ".scale_milli_percent", minScaleMilliPercent, maxScaleMilliPercent, ::keyForViewportScaleMilliPercent),
        PackageConfigKeySpec.string("viewport.", ".mode", ::keyForViewportMode, ::isEnabledViewportMode),
        PackageConfigKeySpec.rangedInteger("font.", ".scale_percent", MIN_FONT_SCALE_PERCENT, MAX_FONT_SCALE_PERCENT, ::keyForFontScale),
        PackageConfigKeySpec.string("font.", ".typeface_id", ::keyForTypefaceId),
        PackageConfigKeySpec.string("font.", ".mode", ::keyForFontMode, ::isFieldRewriteFontMode),
        PackageConfigKeySpec.string("font.", ".hook_domains", ::keyForFontHookDomains),
        PackageConfigKeySpec.booleanValue("target.", ".dpis_enabled", false, ::keyForDpisEnabled),
        PackageConfigKeySpec.rangedInteger("wechat.", ".dpi", WechatDpiConfig.MIN_DPI, WechatDpiConfig.MAX_DPI, ::keyForWechatDpi, WechatDpiConfig::appliesTo)
    )

    val aggregatedConfigKeys: Array<PackageConfigKeySpec> = arrayOf(
        PackageConfigKeySpec.positiveInteger("package_config.", ".viewport.width_dp", ::keyForPackageViewportWidth),
        PackageConfigKeySpec.string("package_config.", ".viewport.target_type", ::keyForPackageViewportTargetType, ::isAbsoluteTargetType),
        PackageConfigKeySpec.rangedInteger("package_config.", ".viewport.scale_permille", minScalePermille, maxScalePermille, ::keyForPackageViewportScalePermille),
        PackageConfigKeySpec.rangedInteger("package_config.", ".viewport.scale_milli_percent", minScaleMilliPercent, maxScaleMilliPercent, ::keyForPackageViewportScaleMilliPercent),
        PackageConfigKeySpec.string("package_config.", ".viewport.mode", ::keyForPackageViewportMode, ::isEnabledViewportMode),
        PackageConfigKeySpec.rangedInteger("package_config.", ".font.scale_percent", MIN_FONT_SCALE_PERCENT, MAX_FONT_SCALE_PERCENT, ::keyForPackageFontScale),
        PackageConfigKeySpec.string("package_config.", ".font.typeface_id", ::keyForPackageTypefaceId),
        PackageConfigKeySpec.string("package_config.", ".font.mode", ::keyForPackageFontMode, ::isFieldRewriteFontMode),
        PackageConfigKeySpec.string("package_config.", ".font.hook_domains", ::keyForPackageFontHookDomains),
        PackageConfigKeySpec.booleanValue("package_config.", ".target.dpis_enabled", false, ::keyForPackageDpisEnabled),
        PackageConfigKeySpec.rangedInteger("package_config.", ".app.wechat_dpi", WechatDpiConfig.MIN_DPI, WechatDpiConfig.MAX_DPI, ::keyForPackageWechatDpi, WechatDpiConfig::appliesTo)
    )

    val templateConfigKeys: Array<PackageConfigKeyFactory> = arrayOf(
        ::keyForViewportWidth, ::keyForViewportTargetType, ::keyForViewportScalePermille,
        ::keyForViewportScaleMilliPercent, ::keyForViewportMode, ::keyForFontScale,
        ::keyForTypefaceId, ::keyForFontMode, ::keyForFontHookDomains
    ).map { builder -> PackageConfigKeyFactory { packageName -> builder(packageName ?: "") } }.toTypedArray()

    val allTemplateConfigKeys: Array<PackageConfigKeyFactory> =
        templateConfigKeys + arrayOf(
            ::keyForPackageViewportWidth, ::keyForPackageViewportTargetType,
            ::keyForPackageViewportScalePermille, ::keyForPackageViewportScaleMilliPercent,
            ::keyForPackageViewportMode, ::keyForPackageFontScale, ::keyForPackageTypefaceId,
            ::keyForPackageFontMode, ::keyForPackageFontHookDomains
        ).map { builder -> PackageConfigKeyFactory { packageName -> builder(packageName ?: "") } }.toTypedArray()

    val aggregatedTemplateConfigKeys: Array<PackageConfigKeyFactory> =
        allTemplateConfigKeys.drop(templateConfigKeys.size).toTypedArray()

    val allViewportConfigKeys: Array<PackageConfigKeyFactory> = arrayOf(
        ::keyForViewportWidth, ::keyForViewportTargetType, ::keyForViewportScalePermille,
        ::keyForViewportScaleMilliPercent, ::keyForViewportMode, ::keyForPackageViewportWidth,
        ::keyForPackageViewportTargetType, ::keyForPackageViewportScalePermille,
        ::keyForPackageViewportScaleMilliPercent, ::keyForPackageViewportMode
    ).map { builder -> PackageConfigKeyFactory { packageName -> builder(packageName ?: "") } }.toTypedArray()

    private fun isAbsoluteTargetType(value: Any?): Boolean =
        value is String && ViewportTargetType.normalize(value) == ViewportTargetType.ABSOLUTE_DP

    private fun isEnabledViewportMode(value: Any?): Boolean {
        if (value !is String) return false
        val mode = ViewportApplyMode.normalize(value)
        return ViewportApplyMode.isEnabled(mode) && mode != ViewportApplyMode.AUTO
    }

    private fun isFieldRewriteFontMode(value: Any?): Boolean =
        value is String && FontApplyMode.normalize(value) == FontApplyMode.FIELD_REWRITE

    fun keyForViewportWidth(packageName: String) = "viewport.$packageName.width_dp"
    fun keyForPackageViewportWidth(packageName: String) = "package_config.$packageName.viewport.width_dp"
    fun keyForViewportTargetType(packageName: String) = "viewport.$packageName.target_type"
    fun keyForPackageViewportTargetType(packageName: String) = "package_config.$packageName.viewport.target_type"
    fun keyForViewportScalePermille(packageName: String) = "viewport.$packageName.scale_permille"
    fun keyForViewportScaleMilliPercent(packageName: String) = "viewport.$packageName.scale_milli_percent"
    fun keyForPackageViewportScalePermille(packageName: String) = "package_config.$packageName.viewport.scale_permille"
    fun keyForPackageViewportScaleMilliPercent(packageName: String) = "package_config.$packageName.viewport.scale_milli_percent"
    fun keyForViewportMode(packageName: String) = "viewport.$packageName.mode"
    fun keyForPackageViewportMode(packageName: String) = "package_config.$packageName.viewport.mode"
    fun keyForFontScale(packageName: String) = "font.$packageName.scale_percent"
    fun keyForPackageFontScale(packageName: String) = "package_config.$packageName.font.scale_percent"
    fun keyForTypefaceId(packageName: String) = "font.$packageName.typeface_id"
    fun keyForPackageTypefaceId(packageName: String) = "package_config.$packageName.font.typeface_id"
    fun keyForFontMode(packageName: String) = "font.$packageName.mode"
    fun keyForPackageFontMode(packageName: String) = "package_config.$packageName.font.mode"
    fun keyForDpisEnabled(packageName: String) = "target.$packageName.dpis_enabled"
    fun keyForPackageDpisEnabled(packageName: String) = "package_config.$packageName.target.dpis_enabled"
    fun keyForFontHookDomains(packageName: String) = "font.$packageName.hook_domains"
    fun keyForPackageFontHookDomains(packageName: String) = "package_config.$packageName.font.hook_domains"
    fun keyForWechatDpi(packageName: String) = "wechat.$packageName.dpi"
    fun keyForPackageWechatDpi(packageName: String) = "package_config.$packageName.app.wechat_dpi"
}
