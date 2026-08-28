package com.dpis.module.config

import android.content.SharedPreferences
import com.dpis.module.backup.BackupReplaceResult
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.viewport.ViewportTargetSpec
import java.io.File

open class PackageConfigStore internal constructor(
    private val preferences: SharedPreferences,
    private val fallbackPreferences: SharedPreferences?,
    private val legacySharedPrefsMirrorFile: File?,
    localOnlyPreferences: SharedPreferences?
) : ConfigSnapshotStore {
    private val localOnlyPreferences: SharedPreferences
    private val legacyPreferencesBridge = LegacySharedPreferencesBridge(preferences, legacySharedPrefsMirrorFile)
    private val preferenceAccess by lazy {
        ConfigPreferenceAccess(
            preferences,
            fallbackPreferences,
            this.localOnlyPreferences,
            legacyPreferencesBridge,
        )
    }
    private val packageCatalog by lazy {
        PackageCatalog(preferences, fallbackPreferences, KEY_TARGET_PACKAGES, ::hasUserVisiblePackageConfig)
    }
    private val packagePresence by lazy {
        PackageConfigPresence(PACKAGE_CONFIG_KEYS, PACKAGE_AGGREGATED_CONFIG_KEYS, ::hasConfiguredValue)
    }
    private val packageReader by lazy {
        PackageConfigReader(preferences, fallbackPreferences, PackageConfigPersistence())
    }
    private val packageWriter by lazy {
        PackageConfigWriter(
            PackageConfigPersistence(),
            ::getConfiguredPackages,
            { packageName, removedKeys -> hasAnyPackageConfigAfterRemoving(packageName, *removedKeys) },
            preferenceAccess::commitBoth,
            preferences,
            { legacyWechatDpiForMigration },
            preferenceAccess::containsPrimary,
            preferenceAccess::contains,
            preferenceAccess::readPrimaryPackageConfigValue
        )
    }
    private val globalConfigStore by lazy {
        GlobalConfigStore(
            preferenceAccess::containsPrimary,
            preferenceAccess::int,
            preferenceAccess::boolean,
            preferenceAccess::localOnlyInt,
            preferenceAccess::localOnlyBoolean,
            preferenceAccess::commitBoth,
            preferenceAccess::commitLocalOnly
        )
    }
    private val snapshotRepository by lazy {
        ConfigSnapshotRepository(
            preferences,
            KEY_TARGET_PACKAGES,
            ::normalizeValue,
            Companion::putTypedValue,
            Companion::isLegacyPackageConfigKey,
            ::migrateLegacyPackageConfigToAggregated,
            legacyPreferencesBridge::mirror,
            preferenceAccess::commitBoth
        )
    }

    constructor(preferences: SharedPreferences) : this(preferences, null, null)

    internal constructor(preferences: SharedPreferences, legacySharedPrefsMirrorFile: File?) : this(
        preferences,
        null,
        legacySharedPrefsMirrorFile
    )

    internal constructor(
        preferences: SharedPreferences,
        fallbackPreferences: SharedPreferences?
    ) : this(preferences, fallbackPreferences, null)

    init {
        this.localOnlyPreferences =
            if (localOnlyPreferences != null) localOnlyPreferences else preferences
    }

    private constructor(
        preferences: SharedPreferences,
        fallbackPreferences: SharedPreferences?,
        legacySharedPrefsMirrorFile: File?
    ) : this(preferences, fallbackPreferences, legacySharedPrefsMirrorFile, preferences)

    override fun getConfiguredPackages(): MutableSet<String?> {
        return packageCatalog.configuredPackages()
    }

    fun hasAnyUserVisiblePackageConfig(): Boolean {
        return packageCatalog.hasAnyUserVisibleConfig()
    }

    fun getTargetViewportWidthDp(packageName: String): Int? {
        return packageReader.viewportWidth(packageName)
    }

    fun getTargetViewportScaleMilliPercent(packageName: String): Int? {
        return packageReader.viewportScaleMilliPercent(packageName)
    }

    fun getTargetViewportType(packageName: String): String? {
        return packageReader.viewportType(packageName)
    }

    override fun getTargetViewportSpec(packageName: String): ViewportTargetSpec {
        return packageReader.viewportSpec(packageName)
    }

    override fun getTargetViewportApplyMode(packageName: String): String? {
        return packageReader.viewportApplyMode(packageName)
    }

    override fun getTargetFontScalePercent(packageName: String): Int? {
        return packageReader.fontScalePercent(packageName)
    }

    override fun getTargetTypefaceId(packageName: String): String? {
        return packageReader.typefaceId(packageName)
    }

    fun getTargetFontHookDomainsRaw(packageName: String): String? {
        return packageReader.fontHookDomains(packageName)
    }

    fun getWechatDpi(packageName: String?): Int? {
        return packageReader.wechatDpi(packageName)
    }

    val legacyWechatDpiForMigration: Int?
        get() = packageReader.legacyWechatDpi()

    fun hasTargetAppSpecificConfig(packageName: String?): Boolean {
        return getWechatDpi(packageName) != null
    }

    override fun getTargetFontApplyMode(packageName: String): String? {
        return packageReader.fontApplyMode(packageName)
    }

    override fun isSystemServerHooksEnabled(): Boolean {
        return globalConfigStore.systemServerHooksEnabled()
    }

    override fun hasSystemServerHooksEnabled(): Boolean {
        return globalConfigStore.hasSystemServerHooksEnabled()
    }

    override fun isSystemServerSafeModeEnabled(): Boolean {
        return globalConfigStore.systemServerSafeModeEnabled()
    }

    override fun hasSystemServerSafeModeEnabled(): Boolean {
        return globalConfigStore.hasSystemServerSafeModeEnabled()
    }

    fun setSystemServerHooksEnabled(enabled: Boolean): Boolean {
        return globalConfigStore.setSystemServerHooksEnabled(enabled)
    }

    fun setSystemServerSafeModeEnabled(enabled: Boolean): Boolean {
        return globalConfigStore.setSystemServerSafeModeEnabled(enabled)
    }

    override fun isGlobalLogEnabled(): Boolean {
        return globalConfigStore.globalLogEnabled()
    }

    override fun hasGlobalLogEnabled(): Boolean {
        return globalConfigStore.hasGlobalLogEnabled()
    }

    fun setGlobalLogEnabled(enabled: Boolean): Boolean {
        return globalConfigStore.setGlobalLogEnabled(enabled)
    }

    val interfaceScalePercent: Int
        get() = globalConfigStore.interfaceScalePercent()

    fun setInterfaceScalePercent(percent: Int): Boolean {
        return globalConfigStore.setInterfaceScalePercent(percent)
    }

    val isStartupDisclaimerAccepted: Boolean
        get() = globalConfigStore.startupDisclaimerAccepted()

    fun setStartupDisclaimerAccepted(accepted: Boolean): Boolean {
        return globalConfigStore.setStartupDisclaimerAccepted(accepted)
    }

    val isFontDebugOverlayEnabled: Boolean
        get() = globalConfigStore.fontDebugOverlayEnabled()

    fun setFontDebugOverlayEnabled(enabled: Boolean): Boolean {
        return globalConfigStore.setFontDebugOverlayEnabled(enabled)
    }

    val fontDebugSelectedMode: Int
        get() = globalConfigStore.fontDebugSelectedMode()

    fun setFontDebugSelectedMode(mode: Int): Boolean {
        return globalConfigStore.setFontDebugSelectedMode(mode)
    }

    val fontDebugSelectedWindow: Int
        get() = globalConfigStore.fontDebugSelectedWindow()

    fun setFontDebugSelectedWindow(window: Int): Boolean {
        return globalConfigStore.setFontDebugSelectedWindow(window)
    }

    val isHyperOsFlutterFontHookEnabled: Boolean
        get() = globalConfigStore.hyperOsFlutterFontHookEnabled()

    val isFlutterFontHookEnabled: Boolean
        get() = globalConfigStore.flutterFontHookEnabled()

    val isFlutterSettingsFontHookEnabled: Boolean
        get() = globalConfigStore.flutterSettingsFontHookEnabled()

    fun hasFlutterSettingsFontHookEnabled(): Boolean {
        return globalConfigStore.hasFlutterSettingsFontHookEnabled()
    }

    fun setFlutterSettingsFontHookEnabled(enabled: Boolean): Boolean {
        return globalConfigStore.setFlutterSettingsFontHookEnabled(enabled)
    }

    fun hasFlutterFontHookEnabled(): Boolean {
        return globalConfigStore.hasFlutterFontHookEnabled()
    }

    fun setFlutterFontHookEnabled(enabled: Boolean): Boolean {
        return globalConfigStore.setFlutterFontHookEnabled(enabled)
    }

    fun hasHyperOsFlutterFontHookEnabled(): Boolean {
        return globalConfigStore.hasHyperOsFlutterFontHookEnabled()
    }

    fun setHyperOsFlutterFontHookEnabled(enabled: Boolean): Boolean {
        return globalConfigStore.setHyperOsFlutterFontHookEnabled(enabled)
    }

    fun getDebugInt(key: String?, defaultValue: Int): Int {
        return globalConfigStore.getDebugInt(key, defaultValue)
    }

    fun setDebugInt(key: String?, value: Int): Boolean {
        return globalConfigStore.setDebugInt(key, value)
    }

    fun getDebugString(key: String?, defaultValue: String?): String? {
        return preferenceAccess.string(key, defaultValue)
    }

    fun setDebugString(key: String?, value: String?): Boolean {
        return preferenceAccess.commitBoth { putString(key, value) }
    }

    fun setTargetViewportWidthDp(packageName: String, widthDp: Int): Boolean {
        return packageWriter.setViewportWidth(packageName, widthDp)
    }

    fun setTargetViewportSpec(packageName: String, spec: ViewportTargetSpec?): Boolean {
        return packageWriter.setViewportSpec(packageName, spec)
    }

    fun setTargetViewportTypeDraft(packageName: String, viewportTargetType: String?): Boolean {
        return packageWriter.setViewportTypeDraft(packageName, viewportTargetType)
    }

    fun clearTargetViewportTypeDraft(packageName: String): Boolean {
        return packageWriter.clearViewportTypeDraft(packageName)
    }

    fun setTargetViewportWidthDraft(packageName: String?, widthDp: Int?): Boolean {
        return packageWriter.setViewportWidthDraft(packageName, widthDp)
    }

    fun setTargetViewportScaleMilliPercentDraft(
        packageName: String?,
        scaleMilliPercent: Int?
    ): Boolean {
        return packageWriter.setViewportScaleDraft(packageName, scaleMilliPercent)
    }

    fun clearTargetViewportWidthDp(packageName: String): Boolean {
        return packageWriter.clearViewport(packageName)
    }

    fun clearTargetViewportValue(packageName: String): Boolean {
        return packageWriter.clearViewportValue(packageName)
    }

    fun setTargetViewportApplyMode(packageName: String, mode: String?): Boolean {
        return packageWriter.setViewportApplyMode(packageName, mode)
    }

    fun setTargetFontScalePercent(packageName: String, percent: Int): Boolean {
        return packageWriter.setFontScale(packageName, percent)
    }

    fun setTargetTypefaceId(packageName: String, typefaceId: String?): Boolean {
        return packageWriter.setTypeface(packageName, typefaceId)
    }

    fun setWechatDpi(packageName: String?, dpi: Int?): Boolean {
        return packageWriter.setWechatDpi(packageName, dpi)
    }

    fun setTargetFontApplyMode(packageName: String, mode: String?): Boolean {
        return packageWriter.setFontApplyMode(packageName, mode)
    }

    fun clearTargetFontScalePercent(packageName: String): Boolean {
        return packageWriter.clearFontScale(packageName)
    }

    fun clearTargetTypefaceId(packageName: String): Boolean {
        return packageWriter.clearTypeface(packageName)
    }

    fun clearWechatDpi(packageName: String?): Boolean {
        return packageWriter.clearWechatDpi(packageName)
    }

    fun migrateLegacyWechatDpi(): Boolean {
        return packageWriter.migrateLegacyWechatDpi()
    }

    fun migrateLegacyPackageConfigToAggregated(): Boolean {
        return packageWriter.migrateLegacyPackageConfigToAggregated()
    }

    fun hasPrimaryTargetViewportWidthDp(packageName: String): Boolean {
        return preferenceAccess.containsPrimary(keyForViewportWidth(packageName))
    }

    fun hasPrimaryTargetViewportApplyMode(packageName: String): Boolean {
        return preferenceAccess.containsPrimary(keyForViewportMode(packageName))
    }

    fun hasPrimaryTargetFontScalePercent(packageName: String): Boolean {
        return preferenceAccess.containsPrimary(keyForFontScale(packageName))
    }

    fun hasPrimaryTargetTypefaceId(packageName: String): Boolean {
        return preferenceAccess.containsPrimary(keyForTypefaceId(packageName))
    }

    fun hasPrimaryTargetFontApplyMode(packageName: String): Boolean {
        return preferenceAccess.containsPrimary(keyForFontMode(packageName))
    }

    override fun isTargetDpisEnabled(packageName: String): Boolean {
        return preferenceAccess.packageBoolean(
            keyForDpisEnabled(packageName),
            keyForPackageDpisEnabled(packageName),
            true
        )
    }

    fun setTargetDpisEnabled(packageName: String, enabled: Boolean): Boolean {
        return packageWriter.setDpisEnabled(packageName, enabled)
    }

    fun clearTargetPackageConfig(packageName: String?): Boolean {
        return packageWriter.clearPackageConfig(packageName)
    }

    fun prunePackageIfOnlyDefaultConfigRemains(packageName: String?): Boolean {
        return packageWriter.pruneDefaultPackage(packageName, packageName != null && isTargetDpisEnabled(packageName))
    }

    override fun getPackageFontHookDomainsRaw(packageName: String?): String? {
        if (packageName == null || packageName.isBlank()) {
            return null
        }
        val key: String = keyForFontHookDomains(packageName)
        val packageKey: String = keyForPackageFontHookDomains(packageName)
        if (!preferenceAccess.contains(key) && !preferenceAccess.contains(packageKey)) {
            return null
        }
        return preferenceAccess.packageString(key, packageKey, null)
    }

    fun setPackageFontHookDomainsRaw(packageName: String?, rawValue: String?): Boolean {
        return packageWriter.setFontHookDomains(packageName, rawValue)
    }

    fun clearPackageFontHookDomainsRaw(packageName: String?): Boolean {
        return packageWriter.clearFontHookDomains(packageName)
    }

    fun hasRealPackageConfig(packageName: String?): Boolean {
        if (packageName == null || packageName.isBlank()) {
            return false
        }
        return hasAnyPackageConfigAfterRemoving(packageName)
    }

    fun hasUserVisiblePackageConfig(packageName: String?): Boolean {
        if (packageName == null || packageName.isBlank()) {
            return false
        }
        // A disabled-only record has runtime meaning, but it is equivalent to
        // the default user-facing state when no viewport, font, or app-specific
        // value remains. Keep it for runtime delivery while excluding it from
        // the configured-app list and home count.
        return hasAnyPackageConfigAfterRemoving(
            packageName,
            keyForDpisEnabled(packageName),
            keyForPackageDpisEnabled(packageName)
        )
    }

    fun readPackageConfig(packageName: String?): PackageConfigValue {
        return packageReader.packageConfig(packageName)
    }

    fun writePackageConfig(packageName: String?, value: PackageConfigValue?): Boolean {
        return packageWriter.writePackageConfig(packageName, value)
    }

    fun readPackageTemplateConfigValue(packageName: String?): TemplateConfigValue {
        return packageReader.templateConfig(packageName)
    }

    fun writePackageTemplateConfigValue(
        packageName: String?,
        value: TemplateConfigValue?
    ): Boolean {
        return packageWriter.writeTemplateConfig(packageName, value)
    }

    private fun hasAnyPackageConfigAfterRemoving(
        packageName: String?,
        vararg removedKeys: String?
    ): Boolean = packagePresence.hasAny(packageName, *removedKeys)

    private fun hasConfiguredValue(
        spec: PackageConfigKeySpec,
        packageName: String?,
        key: String?
    ): Boolean {
        if (!spec.appliesTo(packageName) || !preferenceAccess.contains(key)) {
            return false
        }
        return spec.isConfiguredValue(readPackageConfigValue(spec, key))
    }

    private fun readPackageConfigValue(spec: PackageConfigKeySpec, key: String?): Any? {
        if (spec.expectsInteger()) {
            return preferenceAccess.nullableInt(key)
        }
        if (spec.expectsBoolean()) {
            return preferenceAccess.boolean(key, false)
        }
        return preferenceAccess.string(key, null)
    }

    private fun readPrimaryPackageConfigValue(spec: PackageConfigKeySpec, key: String?): Any? {
        return preferenceAccess.readPrimaryPackageConfigValue(spec, key)
    }

    fun ensureSeedConfig(seedTargetViewportWidthDps: MutableMap<String?, Int?>): Boolean {
        return packageWriter.ensureSeedConfig(seedTargetViewportWidthDps)
    }

    fun snapshotAll(): MutableMap<String, Any?> {
        return snapshotRepository.snapshotAll()
    }

    fun snapshotRuntimeDelivery(): MutableMap<String?, Any?> {
        return snapshotRepository.snapshotRuntimeDelivery()
    }

    fun snapshotBackup(): MutableMap<String, Any?> {
        return snapshotRepository.snapshotBackup()
    }

    fun replaceAll(entries: MutableMap<String?, Any?>?): Boolean {
        return snapshotRepository.replaceAll(entries)
    }

    fun importSharedPreferencesXml(sourceFile: File?): Boolean {
        return legacyPreferencesBridge.importXml(sourceFile, ::replaceAll)
    }

    fun replaceBackup(entries: MutableMap<String, Any?>?): Boolean {
        return snapshotRepository.replaceBackup(entries)
    }

    fun replaceBackupResult(entries: MutableMap<String, Any?>?): BackupReplaceResult {
        return snapshotRepository.replaceBackupResult(entries)
    }

    companion object : PackageConfigPersistence()
}
