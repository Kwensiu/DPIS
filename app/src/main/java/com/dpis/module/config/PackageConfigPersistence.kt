package com.dpis.module.config

import android.content.SharedPreferences
import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.backup.BackupKeyPolicy.isImportable
import com.dpis.module.backup.BackupKeyPolicy.isLocalOnly
import com.dpis.module.backup.BackupReplaceResult
import com.dpis.module.backup.BackupReplaceResult.Companion.failed
import com.dpis.module.backup.BackupReplaceResult.Companion.success
import com.dpis.module.backup.BackupReplaceStage
import com.dpis.module.config.ConfigPreferenceValueCodec
import com.dpis.module.config.LegacySharedPreferencesBridge
import com.dpis.module.config.ConfigPreferenceKeys
import com.dpis.module.config.ConfigSnapshotStore
import com.dpis.module.config.ConfigSnapshotRepository
import com.dpis.module.config.GlobalConfigStore
import com.dpis.module.config.PackageConfigValue
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.FontDebugStatsStore
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import java.io.File
import java.io.IOException
/** Shared key metadata, value normalization, and XML-compatible config helpers. */
open class PackageConfigPersistence {
val MIN_VIEWPORT_WIDTH_DP = 1
val MIN_VIEWPORT_SCALE_MILLI_PERCENT = ViewportTargetSpec.MIN_SCALE_MILLI_PERCENT
val MAX_VIEWPORT_SCALE_MILLI_PERCENT = ViewportTargetSpec.MAX_SCALE_MILLI_PERCENT
// Legacy constants for backward compatibility
val MIN_VIEWPORT_SCALE_PERMILLE = ViewportTargetSpec.MIN_SCALE_PERMILLE
val MAX_VIEWPORT_SCALE_PERMILLE = ViewportTargetSpec.MAX_SCALE_PERMILLE
val MIN_FONT_SCALE_PERCENT = 50
val MAX_FONT_SCALE_PERCENT = 300
val GROUP: String = "dpi_config"
@JvmField
val KEY_TARGET_PACKAGES: String = ConfigPreferenceKeys.TARGET_PACKAGES
val KEY_SYSTEM_SERVER_HOOKS_ENABLED: String =
    ConfigPreferenceKeys.SYSTEM_SERVER_HOOKS_ENABLED
val KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED: String =
    ConfigPreferenceKeys.SYSTEM_SERVER_SAFE_MODE_ENABLED
@JvmField
val KEY_GLOBAL_LOG_ENABLED: String = ConfigPreferenceKeys.GLOBAL_LOG_ENABLED
val KEY_FONT_DEBUG_OVERLAY_ENABLED: String = ConfigPreferenceKeys.FONT_DEBUG_OVERLAY_ENABLED
val KEY_FONT_DEBUG_SELECTED_MODE: String = "font.debug.selected_mode"
val KEY_FONT_DEBUG_SELECTED_WINDOW: String = "font.debug.selected_window"
val KEY_FLUTTER_FONT_HOOK_ENABLED: String = "font.flutter_hook_enabled"
val KEY_FLUTTER_SETTINGS_FONT_HOOK_ENABLED: String =
    "font.flutter_settings_hook_enabled"
val KEY_HYPEROS_FLUTTER_FONT_HOOK_ENABLED: String =
    "font.hyperos_flutter_hook_enabled"
val KEY_HIDE_LAUNCHER_ICON: String = "ui.hide_launcher_icon"
val KEY_INTERFACE_SCALE_PERCENT: String = "ui.interface_scale_percent"
val KEY_STARTUP_DISCLAIMER_ACCEPTED: String = "ui.startup_disclaimer_accepted"
// TODO: Remove after temporary WeChat DPI test builds are no longer upgrade sources.
val LEGACY_WECHAT_DPI_KEY = ("wechat."
        + WechatDpiConfig.PACKAGE_NAME + ".wekit_dpi")
val LOCAL_ONLY_RUNTIME_DELIVERY_KEYS = arrayOf<String?>(
    KEY_INTERFACE_SCALE_PERCENT,
    KEY_STARTUP_DISCLAIMER_ACCEPTED
)
val LOCAL_ONLY_RUNTIME_DELIVERY_PREFIXES = arrayOf<String?>(
    "default_config.",
    "template."
)
val PACKAGE_CONFIG_KEYS = PackageConfigRegistry.legacyConfigKeys
val PACKAGE_AGGREGATED_CONFIG_KEYS = PackageConfigRegistry.aggregatedConfigKeys
val PACKAGE_TEMPLATE_CONFIG_KEYS = PackageConfigRegistry.templateConfigKeys
val PACKAGE_ALL_TEMPLATE_CONFIG_KEYS = PackageConfigRegistry.allTemplateConfigKeys
val PACKAGE_AGGREGATED_TEMPLATE_CONFIG_KEYS = PackageConfigRegistry.aggregatedTemplateConfigKeys
val PACKAGE_ALL_VIEWPORT_CONFIG_KEYS = PackageConfigRegistry.allViewportConfigKeys
/* IDE conversion duplicated a large method-reference block below. The concise
 * registry above is the single owner of package config key definitions. */
fun templateConfigValueFromPackageConfig(
    value: PackageConfigValue?
): TemplateConfigValue {
    val normalized = if (value != null) value else PackageConfigValue.EMPTY
    return TemplateConfigValueAdapters.fromViewportTargetSpec(
        normalized.viewportTargetSpec(),
        normalized.viewportTargetType(),
        normalized.viewportApplyMode(),
        normalized.fontScalePercent(),
        normalized.fontApplyMode(),
        normalized.typefaceId(),
        normalized.fontHookDomainsRaw()
    )
}
fun packageConfigValueFromTemplateConfigValue(
    value: TemplateConfigValue?
): PackageConfigValue {
    val normalized = if (value != null) value else TemplateConfigValue.EMPTY
    return TemplateConfigValueAdapters.toPackageConfigValue(normalized)
}
fun removePackageTemplateConfigKeys(
    editor: SharedPreferences.Editor,
    packageName: String?
) {
    for (key in templateConfigKeysForPackage(packageName)) {
        editor.remove(key)
    }
}
fun removePackageAggregatedTemplateConfigKeys(
    editor: SharedPreferences.Editor,
    packageName: String?
) {
    for (key in aggregatedTemplateConfigKeysForPackage(packageName)) {
        editor.remove(key)
    }
}
fun removePackageViewportConfigKeys(
    editor: SharedPreferences.Editor,
    packageName: String?
) {
    for (key in allViewportConfigKeysForPackage(packageName)) {
        editor.remove(key)
    }
}
fun removePackageViewportValueKeys(
    editor: SharedPreferences.Editor,
    packageName: String
) {
    editor.remove(keyForViewportWidth(packageName))
        .remove(keyForViewportScalePermille(packageName))
        .remove(keyForViewportScaleMilliPercent(packageName))
        .remove(keyForPackageViewportWidth(packageName))
        .remove(keyForPackageViewportScalePermille(packageName))
        .remove(keyForPackageViewportScaleMilliPercent(packageName))
}
fun templateConfigKeysForPackage(packageName: String?): Array<String?> {
    return keysForPackage(packageName, PACKAGE_TEMPLATE_CONFIG_KEYS)
}
fun aggregatedTemplateConfigKeysForPackage(packageName: String?): Array<String?> {
    return keysForPackage(packageName, PACKAGE_AGGREGATED_TEMPLATE_CONFIG_KEYS)
}
fun allTemplateConfigKeysForPackage(packageName: String?): Array<String?> {
    return keysForPackage(packageName, PACKAGE_ALL_TEMPLATE_CONFIG_KEYS)
}
fun allViewportConfigKeysForPackage(packageName: String?): Array<String?> {
    return keysForPackage(packageName, PACKAGE_ALL_VIEWPORT_CONFIG_KEYS)
}
fun packageConfigKeysForPackage(packageName: String?): Array<String?> {
    if (packageName == null) return emptyArray()
    val keys =
        arrayOfNulls<String>(PACKAGE_CONFIG_KEYS.size + PACKAGE_AGGREGATED_CONFIG_KEYS.size)
    var index = 0
    for (spec in PACKAGE_CONFIG_KEYS) {
        keys[index++] = spec.keyForPackage(packageName)
    }
    for (spec in PACKAGE_AGGREGATED_CONFIG_KEYS) {
        keys[index++] = spec.keyForPackage(packageName)
    }
    return keys
}
fun isRemovedKey(key: String, vararg removedKeys: String?): Boolean {
    if (removedKeys == null) {
        return false
    }
    for (removedKey in removedKeys) {
        if (key == removedKey) {
            return true
        }
    }
    return false
}
fun collectPackageNamesFromSavedState(
    packages: LinkedHashSet<String?>,
    values: MutableMap<String, *>?
) {
    if (values == null || values.isEmpty()) {
        return
    }
    for (key in values.keys) {
        val packageName: String? = packageNameFromSavedPackageKey(key, values.get(key))
        if (packageName != null) {
            packages.add(packageName)
        }
    }
}
fun collectLegacyPackageConfigNames(values: MutableMap<String, *>?): LinkedHashSet<String?> {
    val packages = LinkedHashSet<String?>()
    if (values == null || values.isEmpty()) {
        return packages
    }
    for (key in values.keys) {
        for (spec in PACKAGE_CONFIG_KEYS) {
            val packageName = spec.packageNameFromStorageKey(key, false)
            if (packageName != null) {
                packages.add(packageName)
            }
        }
    }
    return packages
}
fun normalizeLegacyPackageConfigValue(key: String?, value: Any?): Any? {
    if (key == null || value == null) {
        return null
    }
    if (key.startsWith("viewport.") && key.endsWith(".width_dp")) {
        return if (value is Int) normalizeViewportWidth(value) else null
    }
    if (key.startsWith("viewport.") && key.endsWith(".target_type")) {
        val normalized = if (value is String)
            ViewportTargetType.normalize(value)
        else
            ViewportTargetType.OFF
        return if (ViewportTargetType.OFF == normalized) null else normalized
    }
    if (key.startsWith("viewport.") && key.endsWith(".scale_permille")) {
        return if (value is Int)
            normalizeViewportScalePermille(value)
        else
            null
    }
    if (key.startsWith("viewport.") && key.endsWith(".scale_milli_percent")) {
        return if (value is Int)
            normalizeViewportScaleMilliPercent(value)
        else
            null
    }
    if (key.startsWith("viewport.") && key.endsWith(".mode")) {
        val normalized = if (value is String)
            ViewportApplyMode.normalize(value)
        else
            ViewportApplyMode.OFF
        return if (isConfiguredViewportModeValue(normalized)) normalized else null
    }
    if (key.startsWith("font.") && key.endsWith(".scale_percent")) {
        return if (value is Int) normalizeFontScalePercent(value) else null
    }
    if (key.startsWith("font.") && key.endsWith(".typeface_id")) {
        return if (value is String) normalizeTypefaceId(value) else null
    }
    if (key.startsWith("font.") && key.endsWith(".mode")) {
        val normalized = if (value is String)
            FontApplyMode.normalize(value)
        else
            FontApplyMode.OFF
        return if (isConfiguredFontModeValue(normalized)) normalized else null
    }
    if (key.startsWith("font.") && key.endsWith(".hook_domains")) {
        return if (value is String) normalizeNonEmptyString(value) else null
    }
    if (key.startsWith("target.") && key.endsWith(".dpis_enabled")) {
        return if (java.lang.Boolean.FALSE == value) java.lang.Boolean.FALSE else null
    }
    if (key.startsWith("wechat.") && key.endsWith(".dpi")) {
        return if (value is Int) WechatDpiConfig.normalize(value) else null
    }
    return null
}
fun packageNameFromSavedPackageKey(key: String?, value: Any?): String? {
    if (key == null || key.isEmpty()) {
        return null
    }
    for (spec in PACKAGE_CONFIG_KEYS) {
        val packageName = spec.packageNameFromKey(key, value)
        if (packageName != null) {
            return packageName
        }
    }
    for (spec in PACKAGE_AGGREGATED_CONFIG_KEYS) {
        val packageName = spec.packageNameFromKey(key, value)
        if (packageName != null) {
            return packageName
        }
    }
    return null
}
fun keysForPackage(
    packageName: String?,
    keyFactories: Array<PackageConfigKeyFactory>
): Array<String?> {
    val keys = arrayOfNulls<String>(keyFactories.size)
    for (index in keyFactories.indices) {
        keys[index] = keyFactories[index].keyForPackage(packageName)
    }
    return keys
}
fun isConfiguredViewportTargetTypeValue(value: Any?): Boolean {
    val normalized = if (value is String)
        ViewportTargetType.normalize(value)
    else
        ViewportTargetType.OFF
    // Relative scale is the default editor draft; fixed-width selection is
    // an explicit user preference even before its numeric value is entered.
    return ViewportTargetType.ABSOLUTE_DP == normalized
}
fun isConfiguredPackageViewportTargetTypeValue(value: Any?): Boolean {
    val normalized = if (value is String)
        ViewportTargetType.normalize(value)
    else
        ViewportTargetType.OFF
    return ViewportTargetType.ABSOLUTE_DP == normalized
}
fun isConfiguredViewportModeValue(value: Any?): Boolean {
    val normalized = if (value is String)
        ViewportApplyMode.normalize(value)
    else
        ViewportApplyMode.OFF
    return ViewportApplyMode.isEnabled(normalized)
            && ViewportApplyMode.AUTO != normalized
}
fun isConfiguredFontModeValue(value: Any?): Boolean {
    val normalized = if (value is String)
        FontApplyMode.normalize(value)
    else
        FontApplyMode.OFF
    return FontApplyMode.FIELD_REWRITE == normalized
}
fun removePackageConfigKeys(
    editor: SharedPreferences.Editor,
    packageName: String?,
    specs: Array<PackageConfigKeySpec>
) {
    for (spec in specs) {
        packageName?.let { editor.remove(spec.keyForPackage(it)) }
    }
}
fun isLegacyPackageConfigKey(key: String?): Boolean {
    if (key == null) {
        return false
    }
    for (spec in PACKAGE_CONFIG_KEYS) {
        if (spec.packageNameFromStorageKey(key, false) != null) {
            return true
        }
    }
    return false
}
fun <T> readPreferenceValue(
    source: SharedPreferences?,
    reader: PreferenceReader<T?>
): T? {
    try {
        return reader.read(source)
    } catch (ignored: ClassCastException) {
        return null
    }
}

fun interface PreferenceReader<T> {
    fun read(preferences: SharedPreferences?): T?
}
@Throws(IOException::class)
fun writeSharedPreferencesXmlForTest(entries: MutableMap<String, *>?, targetFile: File) {
    LegacySharedPreferencesBridge.writeSharedPreferencesXmlForTest(entries, targetFile)
}
@Throws(Exception::class)
fun readSharedPreferencesXmlForTest(sourceFile: File?): MutableMap<String?, Any?> {
    return LegacySharedPreferencesBridge.readSharedPreferencesXmlForTest(sourceFile)
}
fun putTypedValue(editor: SharedPreferences.Editor, key: String?, value: Any?) {
    if (key != null) ConfigPreferenceValueCodec.put(editor, key, value)
}
fun normalizeValue(value: Any?): Any? {
    return ConfigPreferenceValueCodec.normalize(value)
}
fun normalizeViewportWidth(widthDp: Int?): Int? {
    if (widthDp == null || widthDp < MIN_VIEWPORT_WIDTH_DP) {
        return null
    }
    return widthDp
}
fun normalizeViewportScaleMilliPercent(scaleMilliPercent: Int?): Int? {
    if (scaleMilliPercent == null || scaleMilliPercent < MIN_VIEWPORT_SCALE_MILLI_PERCENT || scaleMilliPercent > MAX_VIEWPORT_SCALE_MILLI_PERCENT) {
        return null
    }
    return scaleMilliPercent
}
fun normalizeViewportScalePermille(scalePermille: Int?): Int? {
    if (scalePermille == null || scalePermille < MIN_VIEWPORT_SCALE_PERMILLE || scalePermille > MAX_VIEWPORT_SCALE_PERMILLE) {
        return null
    }
    return scalePermille
}
fun normalizeFontScalePercent(percent: Int?): Int? {
    if (percent == null || percent < MIN_FONT_SCALE_PERCENT || percent > MAX_FONT_SCALE_PERCENT) {
        return null
    }
    return percent
}
fun normalizeTypefaceId(typefaceId: String?): String? {
    return normalizeNonEmptyString(typefaceId)
}
fun normalizeNonEmptyString(value: String?): String? {
    if (value == null) {
        return null
    }
    val normalizedValue = value.trim { it <= ' ' }
    if (normalizedValue.isEmpty()) {
        return null
    }
    return normalizedValue
}
fun keyForViewportWidth(packageName: String): String = PackageConfigRegistry.keyForViewportWidth(packageName)
fun keyForPackageViewportWidth(packageName: String): String = PackageConfigRegistry.keyForPackageViewportWidth(packageName)
fun keyForViewportTargetType(packageName: String): String = PackageConfigRegistry.keyForViewportTargetType(packageName)
fun keyForPackageViewportTargetType(packageName: String): String = PackageConfigRegistry.keyForPackageViewportTargetType(packageName)
fun keyForViewportScalePermille(packageName: String): String = PackageConfigRegistry.keyForViewportScalePermille(packageName)
fun keyForViewportScaleMilliPercent(packageName: String): String = PackageConfigRegistry.keyForViewportScaleMilliPercent(packageName)
fun keyForPackageViewportScalePermille(packageName: String): String = PackageConfigRegistry.keyForPackageViewportScalePermille(packageName)
fun keyForPackageViewportScaleMilliPercent(packageName: String): String = PackageConfigRegistry.keyForPackageViewportScaleMilliPercent(packageName)
fun keyForViewportMode(packageName: String): String = PackageConfigRegistry.keyForViewportMode(packageName)
fun keyForPackageViewportMode(packageName: String): String = PackageConfigRegistry.keyForPackageViewportMode(packageName)
fun keyForFontScale(packageName: String): String = PackageConfigRegistry.keyForFontScale(packageName)
fun keyForPackageFontScale(packageName: String): String = PackageConfigRegistry.keyForPackageFontScale(packageName)
fun keyForTypefaceId(packageName: String): String = PackageConfigRegistry.keyForTypefaceId(packageName)
fun keyForPackageTypefaceId(packageName: String): String = PackageConfigRegistry.keyForPackageTypefaceId(packageName)
fun keyForFontMode(packageName: String): String = PackageConfigRegistry.keyForFontMode(packageName)
fun keyForPackageFontMode(packageName: String): String = PackageConfigRegistry.keyForPackageFontMode(packageName)
fun keyForDpisEnabled(packageName: String): String = PackageConfigRegistry.keyForDpisEnabled(packageName)
fun keyForPackageDpisEnabled(packageName: String): String = PackageConfigRegistry.keyForPackageDpisEnabled(packageName)
fun keyForFontHookDomains(packageName: String): String = PackageConfigRegistry.keyForFontHookDomains(packageName)
fun keyForPackageFontHookDomains(packageName: String): String = PackageConfigRegistry.keyForPackageFontHookDomains(packageName)
fun keyForWechatDpi(packageName: String): String = PackageConfigRegistry.keyForWechatDpi(packageName)
fun keyForPackageWechatDpi(packageName: String): String = PackageConfigRegistry.keyForPackageWechatDpi(packageName)
}
