package com.dpis.module;

import com.dpis.module.templates.TemplateConfigValueAdapters;

import com.dpis.module.templates.TemplateConfigValue;

import com.dpis.module.appconfig.WechatDpiConfig;

import android.content.SharedPreferences;

import com.dpis.module.settings.AppUiScaleManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class DpisConfigStore {
    private static final int MIN_VIEWPORT_WIDTH_DP = 1;
    private static final int MIN_VIEWPORT_SCALE_MILLI_PERCENT = ViewportTargetSpec.MIN_SCALE_MILLI_PERCENT;
    private static final int MAX_VIEWPORT_SCALE_MILLI_PERCENT = ViewportTargetSpec.MAX_SCALE_MILLI_PERCENT;
    // Legacy constants for backward compatibility
    private static final int MIN_VIEWPORT_SCALE_PERMILLE = ViewportTargetSpec.MIN_SCALE_PERMILLE;
    private static final int MAX_VIEWPORT_SCALE_PERMILLE = ViewportTargetSpec.MAX_SCALE_PERMILLE;
    private static final int MIN_FONT_SCALE_PERCENT = 50;
    private static final int MAX_FONT_SCALE_PERCENT = 300;

    public static final String GROUP = "dpi_config";
    static final String KEY_TARGET_PACKAGES = "target_packages";
    static final String KEY_SYSTEM_SERVER_HOOKS_ENABLED = "system_server.hooks_enabled";
    static final String KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED = "system_server.safe_mode_enabled";
    static final String KEY_GLOBAL_LOG_ENABLED = "global.log_enabled";
    static final String KEY_FONT_DEBUG_OVERLAY_ENABLED = "font.debug.overlay_enabled";
    static final String KEY_FONT_DEBUG_SELECTED_MODE = "font.debug.selected_mode";
    static final String KEY_FONT_DEBUG_SELECTED_WINDOW = "font.debug.selected_window";
    static final String KEY_FLUTTER_FONT_HOOK_ENABLED = "font.flutter_hook_enabled";
    static final String KEY_FLUTTER_SETTINGS_FONT_HOOK_ENABLED =
            "font.flutter_settings_hook_enabled";
    static final String KEY_HYPEROS_FLUTTER_FONT_HOOK_ENABLED = "font.hyperos_flutter_hook_enabled";
    static final String KEY_TTC_FONT_IMPORT_ENABLED = "font.ttc_import_enabled";
    static final String KEY_HIDE_LAUNCHER_ICON = "ui.hide_launcher_icon";
    static final String KEY_INTERFACE_SCALE_PERCENT = "ui.interface_scale_percent";
    static final String KEY_STARTUP_DISCLAIMER_ACCEPTED = "ui.startup_disclaimer_accepted";
    // TODO: Remove after temporary WeChat DPI test builds are no longer upgrade sources.
    private static final String LEGACY_WECHAT_DPI_KEY = "wechat."
            + WechatDpiConfig.PACKAGE_NAME + ".wekit_dpi";
    private static final String[] LOCAL_ONLY_RUNTIME_DELIVERY_KEYS = {
            KEY_INTERFACE_SCALE_PERCENT,
            KEY_STARTUP_DISCLAIMER_ACCEPTED
    };
    private static final String[] LOCAL_ONLY_RUNTIME_DELIVERY_PREFIXES = {
            "default_config.",
            "template."
    };
    private static final String[] BACKUP_EXCLUDED_PREFIXES = {
            "font.library.",
            "font.debug.",
            "runtime."
    };
    private static final PackageConfigKeySpec[] PACKAGE_CONFIG_KEYS = {
            PackageConfigKeySpec.positiveInteger("viewport.", ".width_dp",
                    DpisConfigStore::keyForViewportWidth),
            PackageConfigKeySpec.string("viewport.", ".target_type",
                    DpisConfigStore::keyForViewportTargetType,
                    DpisConfigStore::isConfiguredViewportTargetTypeValue),
            PackageConfigKeySpec.rangedInteger("viewport.", ".scale_permille",
                    MIN_VIEWPORT_SCALE_PERMILLE, MAX_VIEWPORT_SCALE_PERMILLE,
                    DpisConfigStore::keyForViewportScalePermille),
            PackageConfigKeySpec.rangedInteger("viewport.", ".scale_milli_percent",
                    MIN_VIEWPORT_SCALE_MILLI_PERCENT, MAX_VIEWPORT_SCALE_MILLI_PERCENT,
                    DpisConfigStore::keyForViewportScaleMilliPercent),
            PackageConfigKeySpec.string("viewport.", ".mode",
                    DpisConfigStore::keyForViewportMode,
                    DpisConfigStore::isConfiguredViewportModeValue),
            PackageConfigKeySpec.rangedInteger("font.", ".scale_percent",
                    MIN_FONT_SCALE_PERCENT, MAX_FONT_SCALE_PERCENT,
                    DpisConfigStore::keyForFontScale),
            PackageConfigKeySpec.string("font.", ".typeface_id",
                    DpisConfigStore::keyForTypefaceId),
            PackageConfigKeySpec.string("font.", ".mode",
                    DpisConfigStore::keyForFontMode,
                    DpisConfigStore::isConfiguredFontModeValue),
            PackageConfigKeySpec.string("font.", ".hook_domains",
                    DpisConfigStore::keyForFontHookDomains),
            PackageConfigKeySpec.booleanValue("target.", ".dpis_enabled", false,
                    DpisConfigStore::keyForDpisEnabled),
            PackageConfigKeySpec.rangedInteger("wechat.", ".dpi",
                    WechatDpiConfig.MIN_DPI, WechatDpiConfig.MAX_DPI,
                    DpisConfigStore::keyForWechatDpi,
                    WechatDpiConfig::appliesTo)
    };
    private static final PackageConfigKeySpec[] PACKAGE_AGGREGATED_CONFIG_KEYS = {
            PackageConfigKeySpec.positiveInteger("package_config.", ".viewport.width_dp",
                    DpisConfigStore::keyForPackageViewportWidth),
            PackageConfigKeySpec.string("package_config.", ".viewport.target_type",
                    DpisConfigStore::keyForPackageViewportTargetType,
                    DpisConfigStore::isConfiguredViewportTargetTypeValue),
            PackageConfigKeySpec.rangedInteger("package_config.", ".viewport.scale_permille",
                    MIN_VIEWPORT_SCALE_PERMILLE, MAX_VIEWPORT_SCALE_PERMILLE,
                    DpisConfigStore::keyForPackageViewportScalePermille),
            PackageConfigKeySpec.rangedInteger("package_config.", ".viewport.scale_milli_percent",
                    MIN_VIEWPORT_SCALE_MILLI_PERCENT, MAX_VIEWPORT_SCALE_MILLI_PERCENT,
                    DpisConfigStore::keyForPackageViewportScaleMilliPercent),
            PackageConfigKeySpec.string("package_config.", ".viewport.mode",
                    DpisConfigStore::keyForPackageViewportMode,
                    DpisConfigStore::isConfiguredViewportModeValue),
            PackageConfigKeySpec.rangedInteger("package_config.", ".font.scale_percent",
                    MIN_FONT_SCALE_PERCENT, MAX_FONT_SCALE_PERCENT,
                    DpisConfigStore::keyForPackageFontScale),
            PackageConfigKeySpec.string("package_config.", ".font.typeface_id",
                    DpisConfigStore::keyForPackageTypefaceId),
            PackageConfigKeySpec.string("package_config.", ".font.mode",
                    DpisConfigStore::keyForPackageFontMode,
                    DpisConfigStore::isConfiguredFontModeValue),
            PackageConfigKeySpec.string("package_config.", ".font.hook_domains",
                    DpisConfigStore::keyForPackageFontHookDomains),
            PackageConfigKeySpec.booleanValue("package_config.", ".target.dpis_enabled", false,
                    DpisConfigStore::keyForPackageDpisEnabled),
            PackageConfigKeySpec.rangedInteger("package_config.", ".app.wechat_dpi",
                    WechatDpiConfig.MIN_DPI, WechatDpiConfig.MAX_DPI,
                    DpisConfigStore::keyForPackageWechatDpi,
                    WechatDpiConfig::appliesTo)
    };
    private static final PackageConfigKeyFactory[] PACKAGE_TEMPLATE_CONFIG_KEYS = {
            DpisConfigStore::keyForViewportWidth,
            DpisConfigStore::keyForViewportTargetType,
            DpisConfigStore::keyForViewportScalePermille,
            DpisConfigStore::keyForViewportScaleMilliPercent,
            DpisConfigStore::keyForViewportMode,
            DpisConfigStore::keyForFontScale,
            DpisConfigStore::keyForTypefaceId,
            DpisConfigStore::keyForFontMode,
            DpisConfigStore::keyForFontHookDomains
    };
    private static final PackageConfigKeyFactory[] PACKAGE_ALL_TEMPLATE_CONFIG_KEYS = {
            DpisConfigStore::keyForViewportWidth,
            DpisConfigStore::keyForViewportTargetType,
            DpisConfigStore::keyForViewportScalePermille,
            DpisConfigStore::keyForViewportScaleMilliPercent,
            DpisConfigStore::keyForViewportMode,
            DpisConfigStore::keyForFontScale,
            DpisConfigStore::keyForTypefaceId,
            DpisConfigStore::keyForFontMode,
            DpisConfigStore::keyForFontHookDomains,
            DpisConfigStore::keyForPackageViewportWidth,
            DpisConfigStore::keyForPackageViewportTargetType,
            DpisConfigStore::keyForPackageViewportScalePermille,
            DpisConfigStore::keyForPackageViewportScaleMilliPercent,
            DpisConfigStore::keyForPackageViewportMode,
            DpisConfigStore::keyForPackageFontScale,
            DpisConfigStore::keyForPackageTypefaceId,
            DpisConfigStore::keyForPackageFontMode,
            DpisConfigStore::keyForPackageFontHookDomains
    };
    private static final PackageConfigKeyFactory[] PACKAGE_AGGREGATED_TEMPLATE_CONFIG_KEYS = {
            DpisConfigStore::keyForPackageViewportWidth,
            DpisConfigStore::keyForPackageViewportTargetType,
            DpisConfigStore::keyForPackageViewportScalePermille,
            DpisConfigStore::keyForPackageViewportScaleMilliPercent,
            DpisConfigStore::keyForPackageViewportMode,
            DpisConfigStore::keyForPackageFontScale,
            DpisConfigStore::keyForPackageTypefaceId,
            DpisConfigStore::keyForPackageFontMode,
            DpisConfigStore::keyForPackageFontHookDomains
    };
    private static final PackageConfigKeyFactory[] PACKAGE_ALL_VIEWPORT_CONFIG_KEYS = {
            DpisConfigStore::keyForViewportWidth,
            DpisConfigStore::keyForViewportTargetType,
            DpisConfigStore::keyForViewportScalePermille,
            DpisConfigStore::keyForViewportScaleMilliPercent,
            DpisConfigStore::keyForViewportMode,
            DpisConfigStore::keyForPackageViewportWidth,
            DpisConfigStore::keyForPackageViewportTargetType,
            DpisConfigStore::keyForPackageViewportScalePermille,
            DpisConfigStore::keyForPackageViewportScaleMilliPercent,
            DpisConfigStore::keyForPackageViewportMode
    };

    private final SharedPreferences preferences;
    private final SharedPreferences fallbackPreferences;
    private final SharedPreferences localOnlyPreferences;
    private final File legacySharedPrefsMirrorFile;

    public DpisConfigStore(SharedPreferences preferences) {
        this(preferences, null, null);
    }

    DpisConfigStore(SharedPreferences preferences, File legacySharedPrefsMirrorFile) {
        this(preferences, null, legacySharedPrefsMirrorFile);
    }

    DpisConfigStore(SharedPreferences preferences, SharedPreferences fallbackPreferences) {
        this(preferences, fallbackPreferences, null);
    }

    DpisConfigStore(
            SharedPreferences preferences,
            SharedPreferences fallbackPreferences,
            File legacySharedPrefsMirrorFile,
            SharedPreferences localOnlyPreferences) {
        this.preferences = preferences;
        this.fallbackPreferences = fallbackPreferences;
        this.localOnlyPreferences = localOnlyPreferences != null ? localOnlyPreferences : preferences;
        this.legacySharedPrefsMirrorFile = legacySharedPrefsMirrorFile;
    }

    private DpisConfigStore(
            SharedPreferences preferences,
            SharedPreferences fallbackPreferences,
            File legacySharedPrefsMirrorFile) {
        this(preferences, fallbackPreferences, legacySharedPrefsMirrorFile, preferences);
    }

    Set<String> getConfiguredPackages() {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        if (preferences.contains(KEY_TARGET_PACKAGES)) {
            Set<String> primaryPackages = preferences.getStringSet(KEY_TARGET_PACKAGES, Collections.emptySet());
            if (primaryPackages != null) {
                packages.addAll(primaryPackages);
            }
        }
        if (fallbackPreferences != null && fallbackPreferences.contains(KEY_TARGET_PACKAGES)) {
            Set<String> fallbackPackages = fallbackPreferences.getStringSet(KEY_TARGET_PACKAGES, Collections.emptySet());
            if (fallbackPackages != null) {
                packages.addAll(fallbackPackages);
            }
        }
        collectPackageNamesFromSavedState(packages, preferences.getAll());
        if (fallbackPreferences != null) {
            collectPackageNamesFromSavedState(packages, fallbackPreferences.getAll());
        }
        return new LinkedHashSet<>(packages);
    }

    boolean hasAnyUserVisiblePackageConfig() {
        for (String packageName : getConfiguredPackages()) {
            if (hasUserVisiblePackageConfig(packageName)) {
                return true;
            }
        }
        return false;
    }

    Integer getTargetViewportWidthDp(String packageName) {
        String key = keyForViewportWidth(packageName);
        String packageKey = keyForPackageViewportWidth(packageName);
        if (!containsPackageValue(key, packageKey)) {
            return null;
        }
        Integer widthDp = getPackageNullableInt(key, packageKey);
        return normalizeViewportWidth(widthDp);
    }

    Integer getTargetViewportScaleMilliPercent(String packageName) {
        String key = keyForViewportScaleMilliPercent(packageName);
        String packageKey = keyForPackageViewportScaleMilliPercent(packageName);
        if (containsPackageValue(key, packageKey)) {
            return normalizeViewportScaleMilliPercent(getPackageNullableInt(key, packageKey));
        }
        // Legacy fallback: read scale_permille and convert
        String legacyKey = keyForViewportScalePermille(packageName);
        String legacyPackageKey = keyForPackageViewportScalePermille(packageName);
        if (containsPackageValue(legacyKey, legacyPackageKey)) {
            Integer legacyValue = normalizeViewportScalePermille(
                    getPackageNullableInt(legacyKey, legacyPackageKey));
            return legacyValue != null ? AppConfigInputValidation.fromLegacyScalePermille(legacyValue) : null;
        }
        return null;
    }

    String getTargetViewportType(String packageName) {
        String key = keyForViewportTargetType(packageName);
        String packageKey = keyForPackageViewportTargetType(packageName);
        if (!containsPackageValue(key, packageKey)) {
            return ViewportTargetType.OFF;
        }
        return ViewportTargetType.normalize(getPackageString(
                key,
                packageKey,
                ViewportTargetType.OFF));
    }

    ViewportTargetSpec getTargetViewportSpec(String packageName) {
        String typeKey = keyForViewportTargetType(packageName);
        String packageTypeKey = keyForPackageViewportTargetType(packageName);
        String type = containsPackageValue(typeKey, packageTypeKey)
                ? ViewportTargetType.normalize(getPackageString(
                        typeKey,
                        packageTypeKey,
                        ViewportTargetType.OFF))
                : ViewportTargetType.OFF;
        if (ViewportTargetType.RELATIVE_SCALE.equals(type)) {
            Integer scaleMilliPercent = getTargetViewportScaleMilliPercent(packageName);
            return scaleMilliPercent != null
                    ? ViewportTargetSpec.relativeScale(scaleMilliPercent)
                    : ViewportTargetSpec.off();
        }
        if (ViewportTargetType.ABSOLUTE_DP.equals(type)) {
            Integer widthDp = getTargetViewportWidthDp(packageName);
            return widthDp != null ? ViewportTargetSpec.absoluteDp(widthDp) : ViewportTargetSpec.off();
        }
        Integer legacyWidthDp = getTargetViewportWidthDp(packageName);
        return legacyWidthDp != null
                ? ViewportTargetSpec.absoluteDp(legacyWidthDp)
                : ViewportTargetSpec.off();
    }

    String getTargetViewportApplyMode(String packageName) {
        String key = keyForViewportMode(packageName);
        String packageKey = keyForPackageViewportMode(packageName);
        if (containsPackageValue(key, packageKey)) {
            return ViewportApplyMode.normalize(getPackageString(
                    key,
                    packageKey,
                    ViewportApplyMode.OFF));
        }
        if (getTargetViewportWidthDp(packageName) != null
                && !containsPackageValue(
                        keyForViewportTargetType(packageName),
                        keyForPackageViewportTargetType(packageName))) {
            // 历史配置迁移：已有宽度但无模式时，默认视为系统策略。
            return ViewportApplyMode.SYSTEM;
        }
        if (getTargetViewportSpec(packageName).isEnabled()) {
            return ViewportApplyMode.AUTO;
        }
        return ViewportApplyMode.OFF;
    }

    Integer getTargetFontScalePercent(String packageName) {
        String key = keyForFontScale(packageName);
        String packageKey = keyForPackageFontScale(packageName);
        if (!containsPackageValue(key, packageKey)) {
            return null;
        }
        Integer percent = getPackageNullableInt(key, packageKey);
        return normalizeFontScalePercent(percent);
    }

    String getTargetTypefaceId(String packageName) {
        String key = keyForTypefaceId(packageName);
        String packageKey = keyForPackageTypefaceId(packageName);
        if (!containsPackageValue(key, packageKey)) {
            return null;
        }
        return normalizeTypefaceId(getPackageString(key, packageKey, null));
    }

    String getTargetFontHookDomainsRaw(String packageName) {
        String key = keyForFontHookDomains(packageName);
        String packageKey = keyForPackageFontHookDomains(packageName);
        if (!containsPackageValue(key, packageKey)) {
            return null;
        }
        String value = getPackageString(key, packageKey, null);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    Integer getWechatDpi(String packageName) {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return null;
        }
        String key = keyForWechatDpi(packageName);
        String packageKey = keyForPackageWechatDpi(packageName);
        if (!containsPackageValue(key, packageKey)) {
            return null;
        }
        return WechatDpiConfig.normalize(getPackageNullableInt(key, packageKey));
    }

    Integer getLegacyWechatDpiForMigration() {
        if (!contains(LEGACY_WECHAT_DPI_KEY)) {
            return null;
        }
        return WechatDpiConfig.normalize(getNullableInt(LEGACY_WECHAT_DPI_KEY));
    }

    boolean hasTargetAppSpecificConfig(String packageName) {
        return getWechatDpi(packageName) != null;
    }

    String getTargetFontApplyMode(String packageName) {
        String key = keyForFontMode(packageName);
        String packageKey = keyForPackageFontMode(packageName);
        if (containsPackageValue(key, packageKey)) {
            return FontApplyMode.normalize(getPackageString(
                    key,
                    packageKey,
                    FontApplyMode.OFF));
        }
        if (getTargetFontScalePercent(packageName) != null) {
            // 历史配置迁移：已有字体百分比但无模式时，默认视为系统模式。
            return FontApplyMode.SYSTEM_EMULATION;
        }
        return FontApplyMode.OFF;
    }

    boolean isSystemServerHooksEnabled() {
        if (!BuildConfig.DEBUG) {
            return true;
        }
        return getBoolean(KEY_SYSTEM_SERVER_HOOKS_ENABLED, true);
    }

    boolean hasSystemServerHooksEnabled() {
        return containsInPrimary(KEY_SYSTEM_SERVER_HOOKS_ENABLED);
    }

    boolean isSystemServerSafeModeEnabled() {
        return getBoolean(KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED, true);
    }

    boolean hasSystemServerSafeModeEnabled() {
        return containsInPrimary(KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED);
    }

    boolean setSystemServerHooksEnabled(boolean enabled) {
        return commitBoth(editor -> editor.putBoolean(KEY_SYSTEM_SERVER_HOOKS_ENABLED, enabled));
    }

    boolean setSystemServerSafeModeEnabled(boolean enabled) {
        return commitBoth(editor -> editor.putBoolean(KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED, enabled));
    }

    boolean isGlobalLogEnabled() {
        return getBoolean(KEY_GLOBAL_LOG_ENABLED, false);
    }

    boolean hasGlobalLogEnabled() {
        return containsInPrimary(KEY_GLOBAL_LOG_ENABLED);
    }

    boolean setGlobalLogEnabled(boolean enabled) {
        return commitBoth(editor -> editor.putBoolean(KEY_GLOBAL_LOG_ENABLED, enabled));
    }

    int getInterfaceScalePercent() {
        return AppUiScaleManager.normalizeScalePercent(
                getLocalOnlyInt(KEY_INTERFACE_SCALE_PERCENT,
                        AppUiScaleManager.DEFAULT_SCALE_PERCENT));
    }

    boolean setInterfaceScalePercent(int percent) {
        return commitLocalOnly(editor -> editor.putInt(
                KEY_INTERFACE_SCALE_PERCENT, AppUiScaleManager.normalizeScalePercent(percent)));
    }

    boolean isStartupDisclaimerAccepted() {
        return getLocalOnlyBoolean(KEY_STARTUP_DISCLAIMER_ACCEPTED, false);
    }

    boolean setStartupDisclaimerAccepted(boolean accepted) {
        return commitLocalOnly(editor -> editor.putBoolean(KEY_STARTUP_DISCLAIMER_ACCEPTED, accepted));
    }

    boolean isFontDebugOverlayEnabled() {
        return getBoolean(KEY_FONT_DEBUG_OVERLAY_ENABLED, false);
    }

    boolean setFontDebugOverlayEnabled(boolean enabled) {
        return commitBoth(editor -> editor.putBoolean(KEY_FONT_DEBUG_OVERLAY_ENABLED, enabled));
    }

    int getFontDebugSelectedMode() {
        return getInt(KEY_FONT_DEBUG_SELECTED_MODE, FontDebugStatsStore.MODE_CHAIN);
    }

    boolean setFontDebugSelectedMode(int mode) {
        return commitBoth(editor -> editor.putInt(KEY_FONT_DEBUG_SELECTED_MODE, mode));
    }

    int getFontDebugSelectedWindow() {
        return getInt(KEY_FONT_DEBUG_SELECTED_WINDOW, FontDebugStatsStore.WINDOW_ALL);
    }

    boolean setFontDebugSelectedWindow(int window) {
        return commitBoth(editor -> editor.putInt(KEY_FONT_DEBUG_SELECTED_WINDOW, window));
    }

    boolean isHyperOsFlutterFontHookEnabled() {
        return getBoolean(KEY_HYPEROS_FLUTTER_FONT_HOOK_ENABLED, false);
    }

    boolean isFlutterFontHookEnabled() {
        return getBoolean(KEY_FLUTTER_FONT_HOOK_ENABLED, false);
    }

    boolean isFlutterSettingsFontHookEnabled() {
        return getBoolean(KEY_FLUTTER_SETTINGS_FONT_HOOK_ENABLED, false);
    }

    boolean hasFlutterSettingsFontHookEnabled() {
        return containsInPrimary(KEY_FLUTTER_SETTINGS_FONT_HOOK_ENABLED);
    }

    boolean setFlutterSettingsFontHookEnabled(boolean enabled) {
        return commitBoth(editor -> editor.putBoolean(
                KEY_FLUTTER_SETTINGS_FONT_HOOK_ENABLED, enabled));
    }

    boolean hasFlutterFontHookEnabled() {
        return containsInPrimary(KEY_FLUTTER_FONT_HOOK_ENABLED);
    }

    boolean setFlutterFontHookEnabled(boolean enabled) {
        return commitBoth(editor -> editor.putBoolean(KEY_FLUTTER_FONT_HOOK_ENABLED, enabled));
    }

    boolean hasHyperOsFlutterFontHookEnabled() {
        return containsInPrimary(KEY_HYPEROS_FLUTTER_FONT_HOOK_ENABLED);
    }

    boolean setHyperOsFlutterFontHookEnabled(boolean enabled) {
        return commitBoth(editor -> editor.putBoolean(KEY_HYPEROS_FLUTTER_FONT_HOOK_ENABLED, enabled));
    }

    boolean isTtcFontImportEnabled() {
        return getBoolean(KEY_TTC_FONT_IMPORT_ENABLED, false);
    }

    boolean setTtcFontImportEnabled(boolean enabled) {
        return commitBoth(editor -> editor.putBoolean(KEY_TTC_FONT_IMPORT_ENABLED, enabled));
    }

    int getDebugInt(String key, int defaultValue) {
        return getInt(key, defaultValue);
    }

    boolean setDebugInt(String key, int value) {
        return commitBoth(editor -> editor.putInt(key, value));
    }

    String getDebugString(String key, String defaultValue) {
        return getString(key, defaultValue);
    }

    boolean setDebugString(String key, String value) {
        return commitBoth(editor -> editor.putString(key, value));
    }

    boolean setTargetViewportWidthDp(String packageName, int widthDp) {
        Integer normalizedWidthDp = normalizeViewportWidth(widthDp);
        if (normalizedWidthDp == null) {
            return clearTargetViewportWidthDp(packageName);
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> {
            editor.putStringSet(KEY_TARGET_PACKAGES, packages);
            removePackageViewportValueKeys(editor, packageName);
            editor.putString(keyForViewportTargetType(packageName), ViewportTargetType.ABSOLUTE_DP);
            editor.putString(
                    keyForPackageViewportTargetType(packageName),
                    ViewportTargetType.ABSOLUTE_DP);
            editor.putInt(keyForViewportWidth(packageName), normalizedWidthDp);
            editor.putInt(keyForPackageViewportWidth(packageName), normalizedWidthDp);
        });
    }

    boolean setTargetViewportSpec(String packageName, ViewportTargetSpec spec) {
        ViewportTargetSpec normalized = spec != null ? spec : ViewportTargetSpec.off();
        if (!normalized.isEnabled()) {
            return clearTargetViewportWidthDp(packageName);
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> {
            editor.putStringSet(KEY_TARGET_PACKAGES, packages);
            removePackageViewportValueKeys(editor, packageName);
            editor.putString(keyForViewportTargetType(packageName), normalized.type());
            editor.putString(keyForPackageViewportTargetType(packageName), normalized.type());
            if (normalized.isRelativeScale()) {
                int scaleMilliPercent = normalized.scaleMilliPercent();
                editor.putInt(keyForViewportScaleMilliPercent(packageName), scaleMilliPercent);
                editor.putInt(
                        keyForPackageViewportScaleMilliPercent(packageName),
                        scaleMilliPercent);
                // Double-write legacy for downgrade compatibility
                int legacyPermille = AppConfigInputValidation.toLegacyScalePermille(scaleMilliPercent);
                editor.putInt(keyForViewportScalePermille(packageName), legacyPermille);
                editor.putInt(
                        keyForPackageViewportScalePermille(packageName),
                        legacyPermille);
                return;
            }
            editor.putInt(keyForViewportWidth(packageName), normalized.absoluteWidthDp());
            editor.putInt(keyForPackageViewportWidth(packageName), normalized.absoluteWidthDp());
        });
    }

    boolean setTargetViewportTypeDraft(String packageName, String viewportTargetType) {
        String normalized = ViewportTargetType.normalize(viewportTargetType);
        if (ViewportTargetType.OFF.equals(normalized)) {
            return clearTargetViewportTypeDraft(packageName);
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putString(keyForViewportTargetType(packageName), normalized)
                .putString(keyForPackageViewportTargetType(packageName), normalized));
    }

    boolean clearTargetViewportTypeDraft(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName,
                keyForViewportTargetType(packageName),
                keyForPackageViewportTargetType(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForViewportTargetType(packageName))
                .remove(keyForPackageViewportTargetType(packageName)));
    }

    boolean setTargetViewportWidthDraft(String packageName, Integer widthDp) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        if (widthDp != null && widthDp <= 0) {
            return true;
        }
        String widthKey = keyForViewportWidth(packageName);
        String packageWidthKey = keyForPackageViewportWidth(packageName);
        if (widthDp == null) {
            LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
            if (!hasAnyPackageConfigAfterRemoving(packageName, widthKey, packageWidthKey)) {
                packages.remove(packageName);
            }
            return commitBoth(editor -> editor
                    .putStringSet(KEY_TARGET_PACKAGES, packages)
                    .remove(widthKey)
                    .remove(packageWidthKey));
        }
        Integer normalizedWidthDp = normalizeViewportWidth(widthDp);
        if (normalizedWidthDp == null) {
            return true;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(widthKey, normalizedWidthDp)
                .putInt(packageWidthKey, normalizedWidthDp));
    }

    boolean setTargetViewportScaleMilliPercentDraft(String packageName, Integer scaleMilliPercent) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        if (scaleMilliPercent != null && scaleMilliPercent <= 0) {
            return true;
        }
        String scaleKey = keyForViewportScaleMilliPercent(packageName);
        String packageScaleKey = keyForPackageViewportScaleMilliPercent(packageName);
        String legacyScaleKey = keyForViewportScalePermille(packageName);
        String legacyPackageScaleKey = keyForPackageViewportScalePermille(packageName);
        if (scaleMilliPercent == null) {
            LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
            if (!hasAnyPackageConfigAfterRemoving(
                    packageName,
                    scaleKey,
                    packageScaleKey,
                    legacyScaleKey,
                    legacyPackageScaleKey)) {
                packages.remove(packageName);
            }
            return commitBoth(editor -> editor
                    .putStringSet(KEY_TARGET_PACKAGES, packages)
                    .remove(scaleKey)
                    .remove(packageScaleKey)
                    .remove(legacyScaleKey)
                    .remove(legacyPackageScaleKey));
        }
        Integer normalizedScaleMilliPercent = normalizeViewportScaleMilliPercent(scaleMilliPercent);
        if (normalizedScaleMilliPercent == null) {
            return true;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        int legacyPermille = AppConfigInputValidation.toLegacyScalePermille(normalizedScaleMilliPercent);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(scaleKey, normalizedScaleMilliPercent)
                .putInt(packageScaleKey, normalizedScaleMilliPercent)
                .putInt(legacyScaleKey, legacyPermille)
                .putInt(legacyPackageScaleKey, legacyPermille));
    }

    boolean clearTargetViewportWidthDp(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName,
                allViewportConfigKeysForPackage(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForViewportWidth(packageName))
                .remove(keyForViewportTargetType(packageName))
                .remove(keyForViewportScalePermille(packageName))
                .remove(keyForViewportScaleMilliPercent(packageName))
                .remove(keyForViewportMode(packageName))
                .remove(keyForPackageViewportWidth(packageName))
                .remove(keyForPackageViewportTargetType(packageName))
                .remove(keyForPackageViewportScalePermille(packageName))
                .remove(keyForPackageViewportScaleMilliPercent(packageName))
                .remove(keyForPackageViewportMode(packageName)));
    }

    boolean clearTargetViewportValue(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName,
                keyForViewportWidth(packageName),
                keyForViewportScalePermille(packageName),
                keyForViewportScaleMilliPercent(packageName),
                keyForViewportMode(packageName),
                keyForPackageViewportWidth(packageName),
                keyForPackageViewportScalePermille(packageName),
                keyForPackageViewportScaleMilliPercent(packageName),
                keyForPackageViewportMode(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForViewportWidth(packageName))
                .remove(keyForViewportScalePermille(packageName))
                .remove(keyForViewportScaleMilliPercent(packageName))
                .remove(keyForViewportMode(packageName))
                .remove(keyForPackageViewportWidth(packageName))
                .remove(keyForPackageViewportScalePermille(packageName))
                .remove(keyForPackageViewportScaleMilliPercent(packageName))
                .remove(keyForPackageViewportMode(packageName)));
    }

    boolean setTargetViewportApplyMode(String packageName, String mode) {
        String normalized = ViewportApplyMode.normalize(mode);
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!isConfiguredViewportModeValue(normalized)) {
            if (!hasAnyPackageConfigAfterRemoving(packageName,
                    keyForViewportMode(packageName),
                    keyForPackageViewportMode(packageName))) {
                packages.remove(packageName);
            }
            return commitBoth(editor -> editor
                    .putStringSet(KEY_TARGET_PACKAGES, packages)
                    .remove(keyForViewportMode(packageName))
                    .remove(keyForPackageViewportMode(packageName)));
        }
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putString(keyForViewportMode(packageName), normalized)
                .putString(keyForPackageViewportMode(packageName), normalized));
    }

    boolean setTargetFontScalePercent(String packageName, int percent) {
        Integer normalizedPercent = normalizeFontScalePercent(percent);
        if (normalizedPercent == null) {
            return clearTargetFontScalePercent(packageName);
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(keyForFontScale(packageName), normalizedPercent)
                .putInt(keyForPackageFontScale(packageName), normalizedPercent));
    }

    boolean setTargetTypefaceId(String packageName, String typefaceId) {
        String normalizedTypefaceId = normalizeTypefaceId(typefaceId);
        if (normalizedTypefaceId == null) {
            return clearTargetTypefaceId(packageName);
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putString(keyForTypefaceId(packageName), normalizedTypefaceId)
                .putString(keyForPackageTypefaceId(packageName), normalizedTypefaceId));
    }

    boolean setWechatDpi(String packageName, Integer dpi) {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return true;
        }
        Integer normalized = WechatDpiConfig.normalize(dpi);
        if (normalized == null) {
            return clearWechatDpi(packageName);
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(keyForWechatDpi(packageName), normalized)
                .putInt(keyForPackageWechatDpi(packageName), normalized));
    }

    boolean setTargetFontApplyMode(String packageName, String mode) {
        String normalized = FontApplyMode.normalize(mode);
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!isConfiguredFontModeValue(normalized)) {
            if (!hasAnyPackageConfigAfterRemoving(packageName,
                    keyForFontMode(packageName),
                    keyForPackageFontMode(packageName))) {
                packages.remove(packageName);
            }
            return commitBoth(editor -> editor
                    .putStringSet(KEY_TARGET_PACKAGES, packages)
                    .remove(keyForFontMode(packageName))
                    .remove(keyForPackageFontMode(packageName)));
        }
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putString(keyForFontMode(packageName), normalized)
                .putString(keyForPackageFontMode(packageName), normalized));
    }

    boolean clearTargetFontScalePercent(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName,
                keyForFontScale(packageName),
                keyForPackageFontScale(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForFontScale(packageName))
                .remove(keyForPackageFontScale(packageName)));
    }

    boolean clearTargetTypefaceId(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName,
                keyForTypefaceId(packageName),
                keyForPackageTypefaceId(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForTypefaceId(packageName))
                .remove(keyForPackageTypefaceId(packageName)));
    }

    boolean clearWechatDpi(String packageName) {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return true;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName,
                keyForWechatDpi(packageName),
                keyForPackageWechatDpi(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForWechatDpi(packageName))
                .remove(keyForPackageWechatDpi(packageName)));
    }

    boolean migrateLegacyWechatDpi() {
        Integer legacyDpi = getLegacyWechatDpiForMigration();
        String officialKey = keyForWechatDpi(WechatDpiConfig.PACKAGE_NAME);
        if (legacyDpi == null) {
            if (!contains(LEGACY_WECHAT_DPI_KEY)) {
                return true;
            }
            return commitBoth(editor -> editor.remove(LEGACY_WECHAT_DPI_KEY));
        }
        if (contains(officialKey)) {
            return commitBoth(editor -> editor.remove(LEGACY_WECHAT_DPI_KEY));
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(WechatDpiConfig.PACKAGE_NAME);
        return commitBoth(editor -> {
            editor.putStringSet(KEY_TARGET_PACKAGES, packages)
                    .remove(LEGACY_WECHAT_DPI_KEY);
            editor.putInt(officialKey, legacyDpi);
            editor.putInt(keyForPackageWechatDpi(WechatDpiConfig.PACKAGE_NAME), legacyDpi);
        });
    }

    boolean migrateLegacyPackageConfigToAggregated() {
        LinkedHashSet<String> packages = collectLegacyPackageConfigNames(preferences.getAll());
        if (packages.isEmpty()) {
            return true;
        }
        return commitBoth(editor -> {
            for (String packageName : packages) {
                for (int index = 0; index < PACKAGE_CONFIG_KEYS.length; index++) {
                    PackageConfigKeySpec legacySpec = PACKAGE_CONFIG_KEYS[index];
                    PackageConfigKeySpec packageSpec = PACKAGE_AGGREGATED_CONFIG_KEYS[index];
                    String legacyKey = legacySpec.keyForPackage(packageName);
                    String packageKey = packageSpec.keyForPackage(packageName);
                    Object legacyValue = readPrimaryPackageConfigValue(legacySpec, legacyKey);
                    Object normalizedValue = normalizeLegacyPackageConfigValue(
                            legacyKey,
                            legacyValue);
                    if (legacySpec.appliesTo(packageName)
                            && normalizedValue != null
                            && !preferences.contains(packageKey)) {
                        putTypedValue(editor, packageKey, normalizedValue);
                    }
                }
                removePackageConfigKeys(editor, packageName, PACKAGE_CONFIG_KEYS);
            }
        });
    }

    boolean hasPrimaryTargetViewportWidthDp(String packageName) {
        return containsInPrimary(keyForViewportWidth(packageName));
    }

    boolean hasPrimaryTargetViewportApplyMode(String packageName) {
        return containsInPrimary(keyForViewportMode(packageName));
    }

    boolean hasPrimaryTargetFontScalePercent(String packageName) {
        return containsInPrimary(keyForFontScale(packageName));
    }

    boolean hasPrimaryTargetTypefaceId(String packageName) {
        return containsInPrimary(keyForTypefaceId(packageName));
    }

    boolean hasPrimaryTargetFontApplyMode(String packageName) {
        return containsInPrimary(keyForFontMode(packageName));
    }

    boolean isTargetDpisEnabled(String packageName) {
        return getPackageBoolean(
                keyForDpisEnabled(packageName),
                keyForPackageDpisEnabled(packageName),
                true);
    }

    boolean setTargetDpisEnabled(String packageName, boolean enabled) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (enabled) {
            if (!hasAnyPackageConfigAfterRemoving(packageName,
                    keyForDpisEnabled(packageName),
                    keyForPackageDpisEnabled(packageName))) {
                packages.remove(packageName);
            }
            return commitBoth(editor -> editor
                    .putStringSet(KEY_TARGET_PACKAGES, packages)
                    .remove(keyForDpisEnabled(packageName))
                    .remove(keyForPackageDpisEnabled(packageName)));
        }
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putBoolean(keyForDpisEnabled(packageName), false)
                .putBoolean(keyForPackageDpisEnabled(packageName), false));
    }

    boolean clearTargetPackageConfig(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.remove(packageName);
        return commitBoth(editor -> {
            editor.putStringSet(KEY_TARGET_PACKAGES, packages);
            removePackageConfigKeys(editor, packageName, PACKAGE_CONFIG_KEYS);
            removePackageConfigKeys(editor, packageName, PACKAGE_AGGREGATED_CONFIG_KEYS);
        });
    }

    boolean prunePackageIfOnlyDefaultConfigRemains(String packageName) {
        if (packageName == null || packageName.isBlank()
                || hasAnyPackageConfigAfterRemoving(packageName)) {
            return true;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.remove(packageName);
        return commitBoth(editor -> editor.putStringSet(KEY_TARGET_PACKAGES, packages));
    }

    String getPackageFontHookDomainsRaw(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return null;
        }
        String key = keyForFontHookDomains(packageName);
        String packageKey = keyForPackageFontHookDomains(packageName);
        if (!containsPackageValue(key, packageKey)) {
            return null;
        }
        return getPackageString(key, packageKey, null);
    }

    boolean setPackageFontHookDomainsRaw(String packageName, String rawValue) {
        if (packageName == null || packageName.isBlank() || rawValue == null) {
            return false;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putString(keyForFontHookDomains(packageName), rawValue)
                .putString(keyForPackageFontHookDomains(packageName), rawValue));
    }

    boolean clearPackageFontHookDomainsRaw(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName,
                keyForFontHookDomains(packageName),
                keyForPackageFontHookDomains(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForFontHookDomains(packageName))
                .remove(keyForPackageFontHookDomains(packageName)));
    }

    boolean hasRealPackageConfig(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        return hasAnyPackageConfigAfterRemoving(packageName);
    }

    boolean hasUserVisiblePackageConfig(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        return hasAnyPackageConfigAfterRemoving(packageName);
    }

    PackageConfigValue readPackageConfig(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return PackageConfigValue.EMPTY;
        }
        return new PackageConfigValue(
                getPackageViewportSpec(packageName),
                containsPackageValue(
                        keyForViewportTargetType(packageName),
                        keyForPackageViewportTargetType(packageName))
                        ? ViewportTargetType.normalize(getPackageString(
                                keyForViewportTargetType(packageName),
                                keyForPackageViewportTargetType(packageName),
                                ViewportTargetType.OFF))
                        : ViewportTargetType.OFF,
                containsPackageValue(
                        keyForViewportMode(packageName),
                        keyForPackageViewportMode(packageName))
                        ? ViewportApplyMode.normalize(getPackageString(
                                keyForViewportMode(packageName),
                                keyForPackageViewportMode(packageName),
                                ViewportApplyMode.OFF))
                        : ViewportApplyMode.OFF,
                containsPackageValue(
                        keyForFontScale(packageName),
                        keyForPackageFontScale(packageName))
                        ? normalizeFontScalePercent(getPackageNullableInt(
                                keyForFontScale(packageName),
                                keyForPackageFontScale(packageName)))
                        : null,
                containsPackageValue(
                        keyForFontMode(packageName),
                        keyForPackageFontMode(packageName))
                        ? FontApplyMode.normalize(getPackageString(
                                keyForFontMode(packageName),
                                keyForPackageFontMode(packageName),
                                FontApplyMode.OFF))
                        : FontApplyMode.OFF,
                normalizeTypefaceId(getPackageString(
                        keyForTypefaceId(packageName),
                        keyForPackageTypefaceId(packageName),
                        null)),
                getPackageString(
                        keyForFontHookDomains(packageName),
                        keyForPackageFontHookDomains(packageName),
                        null),
                containsPackageValue(
                        keyForDpisEnabled(packageName),
                        keyForPackageDpisEnabled(packageName))
                        ? Boolean.valueOf(getPackageBoolean(
                                keyForDpisEnabled(packageName),
                                keyForPackageDpisEnabled(packageName),
                                true))
                        : null,
                WechatDpiConfig.appliesTo(packageName)
                        ? WechatDpiConfig.normalize(getPackageNullableInt(
                                keyForWechatDpi(packageName),
                                keyForPackageWechatDpi(packageName)))
                        : null);
    }

    private ViewportTargetSpec getPackageViewportSpec(String packageName) {
        String type = containsPackageValue(
                keyForViewportTargetType(packageName),
                keyForPackageViewportTargetType(packageName))
                ? ViewportTargetType.normalize(getPackageString(
                        keyForViewportTargetType(packageName),
                        keyForPackageViewportTargetType(packageName),
                        ViewportTargetType.OFF))
                : ViewportTargetType.OFF;
        if (ViewportTargetType.RELATIVE_SCALE.equals(type)) {
            Integer scaleMilliPercent = normalizeViewportScaleMilliPercent(getPackageNullableInt(
                    keyForViewportScaleMilliPercent(packageName),
                    keyForPackageViewportScaleMilliPercent(packageName)));
            if (scaleMilliPercent == null) {
                // Legacy fallback
                Integer legacyPermille = normalizeViewportScalePermille(getPackageNullableInt(
                        keyForViewportScalePermille(packageName),
                        keyForPackageViewportScalePermille(packageName)));
                scaleMilliPercent = legacyPermille != null
                        ? AppConfigInputValidation.fromLegacyScalePermille(legacyPermille) : null;
            }
            return scaleMilliPercent != null
                    ? ViewportTargetSpec.relativeScale(scaleMilliPercent)
                    : ViewportTargetSpec.off();
        }
        if (ViewportTargetType.ABSOLUTE_DP.equals(type)) {
            Integer widthDp = normalizeViewportWidth(getPackageNullableInt(
                    keyForViewportWidth(packageName),
                    keyForPackageViewportWidth(packageName)));
            return widthDp != null ? ViewportTargetSpec.absoluteDp(widthDp) : ViewportTargetSpec.off();
        }
        Integer legacyWidthDp = normalizeViewportWidth(getPackageNullableInt(
                keyForViewportWidth(packageName),
                keyForPackageViewportWidth(packageName)));
        return legacyWidthDp != null
                ? ViewportTargetSpec.absoluteDp(legacyWidthDp)
                : ViewportTargetSpec.off();
    }

    boolean writePackageConfig(String packageName, PackageConfigValue value) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        PackageConfigValue normalized = value != null ? value : PackageConfigValue.EMPTY;
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (normalized.hasAnyValue()) {
            packages.add(packageName);
        } else if (!hasAnyPackageConfigAfterRemoving(
                packageName,
                packageConfigKeysForPackage(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> {
            editor.putStringSet(KEY_TARGET_PACKAGES, packages);
            removePackageConfigKeys(editor, packageName, PACKAGE_CONFIG_KEYS);
            removePackageConfigKeys(editor, packageName, PACKAGE_AGGREGATED_CONFIG_KEYS);
            if (normalized.viewportTargetSpec.isEnabled()) {
                editor.putString(
                        keyForViewportTargetType(packageName),
                        normalized.viewportTargetSpec.type());
                editor.putString(
                        keyForPackageViewportTargetType(packageName),
                        normalized.viewportTargetSpec.type());
                if (normalized.viewportTargetSpec.isRelativeScale()) {
                    int scaleMilliPercent = normalized.viewportTargetSpec.scaleMilliPercent();
                    editor.putInt(
                            keyForViewportScaleMilliPercent(packageName),
                            scaleMilliPercent);
                    editor.putInt(
                            keyForPackageViewportScaleMilliPercent(packageName),
                            scaleMilliPercent);
                    int legacyPermille = AppConfigInputValidation.toLegacyScalePermille(scaleMilliPercent);
                    editor.putInt(
                            keyForViewportScalePermille(packageName),
                            legacyPermille);
                    editor.putInt(
                            keyForPackageViewportScalePermille(packageName),
                            legacyPermille);
                } else {
                    editor.putInt(
                            keyForViewportWidth(packageName),
                            normalized.viewportTargetSpec.absoluteWidthDp());
                    editor.putInt(
                            keyForPackageViewportWidth(packageName),
                            normalized.viewportTargetSpec.absoluteWidthDp());
                }
            } else if (!ViewportTargetType.OFF.equals(normalized.viewportTargetType)) {
                editor.putString(
                        keyForViewportTargetType(packageName),
                        normalized.viewportTargetType);
                editor.putString(
                        keyForPackageViewportTargetType(packageName),
                        normalized.viewportTargetType);
            }
            if (isConfiguredViewportModeValue(normalized.viewportApplyMode)) {
                editor.putString(keyForViewportMode(packageName), normalized.viewportApplyMode);
                editor.putString(
                        keyForPackageViewportMode(packageName),
                        normalized.viewportApplyMode);
            }
            if (normalized.fontScalePercent != null) {
                editor.putInt(keyForFontScale(packageName), normalized.fontScalePercent);
                editor.putInt(keyForPackageFontScale(packageName), normalized.fontScalePercent);
            }
            if (isConfiguredFontModeValue(normalized.fontApplyMode)) {
                editor.putString(keyForFontMode(packageName), normalized.fontApplyMode);
                editor.putString(keyForPackageFontMode(packageName), normalized.fontApplyMode);
            }
            if (normalized.typefaceId != null) {
                editor.putString(keyForTypefaceId(packageName), normalized.typefaceId);
                editor.putString(keyForPackageTypefaceId(packageName), normalized.typefaceId);
            }
            if (normalized.fontHookDomainsRaw != null) {
                editor.putString(keyForFontHookDomains(packageName), normalized.fontHookDomainsRaw);
                editor.putString(
                        keyForPackageFontHookDomains(packageName),
                        normalized.fontHookDomainsRaw);
            }
            if (Boolean.FALSE.equals(normalized.dpisEnabled)) {
                editor.putBoolean(keyForDpisEnabled(packageName), false);
                editor.putBoolean(keyForPackageDpisEnabled(packageName), false);
            }
            if (normalized.wechatDpi != null && WechatDpiConfig.appliesTo(packageName)) {
                editor.putInt(keyForWechatDpi(packageName), normalized.wechatDpi);
                editor.putInt(keyForPackageWechatDpi(packageName), normalized.wechatDpi);
            }
        });
    }

    TemplateConfigValue readPackageTemplateConfigValue(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return TemplateConfigValue.EMPTY;
        }
        return templateConfigValueFromPackageConfig(
                readPackageConfig(packageName));
    }

    boolean writePackageTemplateConfigValue(String packageName, TemplateConfigValue value) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        PackageConfigValue copyableConfig = packageConfigValueFromTemplateConfigValue(normalized);
        if (!normalized.hasAnyValue()) {
            LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
            if (hasAnyPackageConfigAfterRemoving(
                    packageName,
                    allTemplateConfigKeysForPackage(packageName))) {
                packages.add(packageName);
            } else {
                packages.remove(packageName);
            }
            return commitBoth(editor -> {
                editor.putStringSet(KEY_TARGET_PACKAGES, packages);
                removePackageTemplateConfigKeys(editor, packageName);
                removePackageAggregatedTemplateConfigKeys(editor, packageName);
            });
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> {
            editor.putStringSet(KEY_TARGET_PACKAGES, packages);
            removePackageTemplateConfigKeys(editor, packageName);
            removePackageAggregatedTemplateConfigKeys(editor, packageName);
            if (copyableConfig.viewportTargetSpec.isEnabled()) {
                editor.putString(
                        keyForViewportTargetType(packageName),
                        copyableConfig.viewportTargetSpec.type());
                editor.putString(
                        keyForPackageViewportTargetType(packageName),
                        copyableConfig.viewportTargetSpec.type());
                if (copyableConfig.viewportTargetSpec.isRelativeScale()) {
                    int scaleMilliPercent = copyableConfig.viewportTargetSpec.scaleMilliPercent();
                    editor.putInt(
                            keyForViewportScaleMilliPercent(packageName),
                            scaleMilliPercent);
                    editor.putInt(
                            keyForPackageViewportScaleMilliPercent(packageName),
                            scaleMilliPercent);
                    int legacyPermille = AppConfigInputValidation.toLegacyScalePermille(scaleMilliPercent);
                    editor.putInt(
                            keyForViewportScalePermille(packageName),
                            legacyPermille);
                    editor.putInt(
                            keyForPackageViewportScalePermille(packageName),
                            legacyPermille);
                } else {
                    editor.putInt(
                            keyForViewportWidth(packageName),
                            copyableConfig.viewportTargetSpec.absoluteWidthDp());
                    editor.putInt(
                            keyForPackageViewportWidth(packageName),
                            copyableConfig.viewportTargetSpec.absoluteWidthDp());
                }
            }
            if (ViewportApplyMode.isEnabled(copyableConfig.viewportApplyMode)) {
                editor.putString(
                        keyForViewportMode(packageName),
                        copyableConfig.viewportApplyMode);
                editor.putString(
                        keyForPackageViewportMode(packageName),
                        copyableConfig.viewportApplyMode);
            }
            if (copyableConfig.fontScalePercent != null) {
                editor.putInt(keyForFontScale(packageName), copyableConfig.fontScalePercent);
                editor.putInt(
                        keyForPackageFontScale(packageName),
                        copyableConfig.fontScalePercent);
            }
            if (FontApplyMode.isEnabled(copyableConfig.fontApplyMode)) {
                editor.putString(keyForFontMode(packageName), copyableConfig.fontApplyMode);
                editor.putString(keyForPackageFontMode(packageName), copyableConfig.fontApplyMode);
            }
            if (copyableConfig.typefaceId != null) {
                editor.putString(keyForTypefaceId(packageName), copyableConfig.typefaceId);
                editor.putString(keyForPackageTypefaceId(packageName), copyableConfig.typefaceId);
            }
            if (copyableConfig.fontHookDomainsRaw != null) {
                editor.putString(
                        keyForFontHookDomains(packageName),
                        copyableConfig.fontHookDomainsRaw);
                editor.putString(
                        keyForPackageFontHookDomains(packageName),
                        copyableConfig.fontHookDomainsRaw);
            }
        });
    }

    private static TemplateConfigValue templateConfigValueFromPackageConfig(
            PackageConfigValue value) {
        PackageConfigValue normalized = value != null ? value : PackageConfigValue.EMPTY;
        return TemplateConfigValueAdapters.fromViewportTargetSpec(
                normalized.viewportTargetSpec,
                normalized.viewportTargetType,
                normalized.viewportApplyMode,
                normalized.fontScalePercent,
                normalized.fontApplyMode,
                normalized.typefaceId,
                normalized.fontHookDomainsRaw);
    }

    private static PackageConfigValue packageConfigValueFromTemplateConfigValue(
            TemplateConfigValue value) {
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        return TemplateConfigValueAdapters.toPackageConfigValue(normalized);
    }

    private static void removePackageTemplateConfigKeys(
            SharedPreferences.Editor editor,
            String packageName) {
        for (String key : templateConfigKeysForPackage(packageName)) {
            editor.remove(key);
        }
    }

    private static void removePackageAggregatedTemplateConfigKeys(
            SharedPreferences.Editor editor,
            String packageName) {
        for (String key : aggregatedTemplateConfigKeysForPackage(packageName)) {
            editor.remove(key);
        }
    }

    private static void removePackageViewportConfigKeys(
            SharedPreferences.Editor editor,
            String packageName) {
        for (String key : allViewportConfigKeysForPackage(packageName)) {
            editor.remove(key);
        }
    }

    private static void removePackageViewportValueKeys(
            SharedPreferences.Editor editor,
            String packageName) {
        editor.remove(keyForViewportWidth(packageName))
                .remove(keyForViewportScalePermille(packageName))
                .remove(keyForViewportScaleMilliPercent(packageName))
                .remove(keyForPackageViewportWidth(packageName))
                .remove(keyForPackageViewportScalePermille(packageName))
                .remove(keyForPackageViewportScaleMilliPercent(packageName));
    }

    private static String[] templateConfigKeysForPackage(String packageName) {
        return keysForPackage(packageName, PACKAGE_TEMPLATE_CONFIG_KEYS);
    }

    private static String[] aggregatedTemplateConfigKeysForPackage(String packageName) {
        return keysForPackage(packageName, PACKAGE_AGGREGATED_TEMPLATE_CONFIG_KEYS);
    }

    private static String[] allTemplateConfigKeysForPackage(String packageName) {
        return keysForPackage(packageName, PACKAGE_ALL_TEMPLATE_CONFIG_KEYS);
    }

    private static String[] allViewportConfigKeysForPackage(String packageName) {
        return keysForPackage(packageName, PACKAGE_ALL_VIEWPORT_CONFIG_KEYS);
    }

    private static String[] packageConfigKeysForPackage(String packageName) {
        String[] keys = new String[PACKAGE_CONFIG_KEYS.length + PACKAGE_AGGREGATED_CONFIG_KEYS.length];
        int index = 0;
        for (PackageConfigKeySpec spec : PACKAGE_CONFIG_KEYS) {
            keys[index++] = spec.keyForPackage(packageName);
        }
        for (PackageConfigKeySpec spec : PACKAGE_AGGREGATED_CONFIG_KEYS) {
            keys[index++] = spec.keyForPackage(packageName);
        }
        return keys;
    }

    private boolean hasAnyPackageConfigAfterRemoving(String packageName, String... removedKeys) {
        return hasAnyPackageConfigAfterRemoving(PACKAGE_CONFIG_KEYS, packageName, removedKeys)
                || hasAnyPackageConfigAfterRemoving(
                        PACKAGE_AGGREGATED_CONFIG_KEYS,
                        packageName,
                        removedKeys);
    }

    private boolean hasAnyPackageConfigAfterRemoving(
            PackageConfigKeySpec[] specs,
            String packageName,
            String... removedKeys) {
        for (PackageConfigKeySpec spec : specs) {
            String key = spec.keyForPackage(packageName);
            if (!isRemovedKey(key, removedKeys) && hasConfiguredValue(spec, packageName, key)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasConfiguredValue(
            PackageConfigKeySpec spec,
            String packageName,
            String key) {
        if (!spec.appliesTo(packageName) || !contains(key)) {
            return false;
        }
        return spec.isConfiguredValue(readPackageConfigValue(spec, key));
    }

    private Object readPackageConfigValue(PackageConfigKeySpec spec, String key) {
        if (spec.expectsInteger()) {
            return getNullableInt(key);
        }
        if (spec.expectsBoolean()) {
            return readPreferenceValue(key, prefs -> prefs.getBoolean(key, false));
        }
        return getString(key, null);
    }

    private Object readPrimaryPackageConfigValue(PackageConfigKeySpec spec, String key) {
        if (!preferences.contains(key)) {
            return null;
        }
        if (spec.expectsInteger()) {
            return readPreferenceValue(preferences, prefs -> prefs.getInt(key, 0));
        }
        if (spec.expectsBoolean()) {
            return readPreferenceValue(preferences, prefs -> prefs.getBoolean(key, false));
        }
        return readPreferenceValue(preferences, prefs -> prefs.getString(key, null));
    }

    private static boolean isRemovedKey(String key, String... removedKeys) {
        if (removedKeys == null) {
            return false;
        }
        for (String removedKey : removedKeys) {
            if (key.equals(removedKey)) {
                return true;
            }
        }
        return false;
    }

    private static void collectPackageNamesFromSavedState(
            LinkedHashSet<String> packages,
            Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        for (String key : values.keySet()) {
            String packageName = packageNameFromSavedPackageKey(key, values.get(key));
            if (packageName != null) {
                packages.add(packageName);
            }
        }
    }

    private static LinkedHashSet<String> collectLegacyPackageConfigNames(Map<String, ?> values) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        if (values == null || values.isEmpty()) {
            return packages;
        }
        for (String key : values.keySet()) {
            for (PackageConfigKeySpec spec : PACKAGE_CONFIG_KEYS) {
                String packageName = spec.packageNameFromStorageKey(key, false);
                if (packageName != null) {
                    packages.add(packageName);
                }
            }
        }
        return packages;
    }

    private static Object normalizeLegacyPackageConfigValue(String key, Object value) {
        if (key == null || value == null) {
            return null;
        }
        if (key.startsWith("viewport.") && key.endsWith(".width_dp")) {
            return value instanceof Integer intValue ? normalizeViewportWidth(intValue) : null;
        }
        if (key.startsWith("viewport.") && key.endsWith(".target_type")) {
            String normalized = value instanceof String stringValue
                    ? ViewportTargetType.normalize(stringValue)
                    : ViewportTargetType.OFF;
            return ViewportTargetType.OFF.equals(normalized) ? null : normalized;
        }
        if (key.startsWith("viewport.") && key.endsWith(".scale_permille")) {
            return value instanceof Integer intValue
                    ? normalizeViewportScalePermille(intValue)
                    : null;
        }
        if (key.startsWith("viewport.") && key.endsWith(".scale_milli_percent")) {
            return value instanceof Integer intValue
                    ? normalizeViewportScaleMilliPercent(intValue)
                    : null;
        }
        if (key.startsWith("viewport.") && key.endsWith(".mode")) {
            String normalized = value instanceof String stringValue
                    ? ViewportApplyMode.normalize(stringValue)
                    : ViewportApplyMode.OFF;
            return isConfiguredViewportModeValue(normalized) ? normalized : null;
        }
        if (key.startsWith("font.") && key.endsWith(".scale_percent")) {
            return value instanceof Integer intValue ? normalizeFontScalePercent(intValue) : null;
        }
        if (key.startsWith("font.") && key.endsWith(".typeface_id")) {
            return value instanceof String stringValue ? normalizeTypefaceId(stringValue) : null;
        }
        if (key.startsWith("font.") && key.endsWith(".mode")) {
            String normalized = value instanceof String stringValue
                    ? FontApplyMode.normalize(stringValue)
                    : FontApplyMode.OFF;
            return isConfiguredFontModeValue(normalized) ? normalized : null;
        }
        if (key.startsWith("font.") && key.endsWith(".hook_domains")) {
            return value instanceof String stringValue ? normalizeNonEmptyString(stringValue) : null;
        }
        if (key.startsWith("target.") && key.endsWith(".dpis_enabled")) {
            return Boolean.FALSE.equals(value) ? Boolean.FALSE : null;
        }
        if (key.startsWith("wechat.") && key.endsWith(".dpi")) {
            return value instanceof Integer intValue ? WechatDpiConfig.normalize(intValue) : null;
        }
        return null;
    }

    private static String packageNameFromSavedPackageKey(String key, Object value) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        for (PackageConfigKeySpec spec : PACKAGE_CONFIG_KEYS) {
            String packageName = spec.packageNameFromKey(key, value);
            if (packageName != null) {
                return packageName;
            }
        }
        for (PackageConfigKeySpec spec : PACKAGE_AGGREGATED_CONFIG_KEYS) {
            String packageName = spec.packageNameFromKey(key, value);
            if (packageName != null) {
                return packageName;
            }
        }
        return null;
    }

    private static String packageNameBetween(String key, String prefix, String suffix) {
        if (!key.startsWith(prefix) || !key.endsWith(suffix)) {
            return null;
        }
        String packageName = key.substring(prefix.length(), key.length() - suffix.length());
        return packageName.isBlank() ? null : packageName;
    }

    private static String[] keysForPackage(
            String packageName,
            PackageConfigKeyFactory[] keyFactories) {
        String[] keys = new String[keyFactories.length];
        for (int index = 0; index < keyFactories.length; index++) {
            keys[index] = keyFactories[index].keyForPackage(packageName);
        }
        return keys;
    }

    private static boolean isConfiguredViewportTargetTypeValue(Object value) {
        String normalized = value instanceof String stringValue
                ? ViewportTargetType.normalize(stringValue)
                : ViewportTargetType.OFF;
        return ViewportTargetType.ABSOLUTE_DP.equals(normalized);
    }

    private static boolean isConfiguredViewportModeValue(Object value) {
        String normalized = value instanceof String stringValue
                ? ViewportApplyMode.normalize(stringValue)
                : ViewportApplyMode.OFF;
        return ViewportApplyMode.isEnabled(normalized)
                && !ViewportApplyMode.AUTO.equals(normalized);
    }

    private static boolean isConfiguredFontModeValue(Object value) {
        String normalized = value instanceof String stringValue
                ? FontApplyMode.normalize(stringValue)
                : FontApplyMode.OFF;
        return FontApplyMode.FIELD_REWRITE.equals(normalized);
    }

    private static void removePackageConfigKeys(
            SharedPreferences.Editor editor,
            String packageName,
            PackageConfigKeySpec[] specs) {
        for (PackageConfigKeySpec spec : specs) {
            editor.remove(spec.keyForPackage(packageName));
        }
    }

    boolean ensureSeedConfig(Map<String, Integer> seedTargetViewportWidthDps) {
        LinkedHashSet<String> mergedPackages = new LinkedHashSet<>(getConfiguredPackages());
        mergedPackages.addAll(seedTargetViewportWidthDps.keySet());
        return commitBoth(editor -> {
            editor.putStringSet(KEY_TARGET_PACKAGES, mergedPackages);
            for (Map.Entry<String, Integer> entry : seedTargetViewportWidthDps.entrySet()) {
                String key = keyForViewportWidth(entry.getKey());
                if (!containsInPrimary(key)) {
                    editor.putInt(key, entry.getValue());
                }
            }
        });
    }

    Map<String, Object> snapshotAll() {
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        copyEntries(snapshot, preferences.getAll(), false);
        return snapshot;
    }

    Map<String, Object> snapshotRuntimeDelivery() {
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>(snapshotAll());
        snapshot.entrySet().removeIf(entry -> isLocalOnlyRuntimeDeliveryKey(entry.getKey()));
        return snapshot;
    }

    Map<String, Object> snapshotBackup() {
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        copyEntries(snapshot, preferences.getAll(), true);
        return snapshot;
    }

    boolean replaceAll(Map<String, Object> entries) {
        return replaceEntries(entries);
    }

    boolean importSharedPreferencesXml(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) {
            return false;
        }
        try {
            Map<String, Object> entries = readSharedPreferencesXml(sourceFile);
            return !entries.isEmpty() && replaceAll(entries);
        } catch (Throwable throwable) {
            DpisLog.e("legacy shared prefs import failed", throwable);
            return false;
        }
    }

    boolean replaceBackup(Map<String, Object> entries) {
        return replaceBackupEntries(entries);
    }

    private boolean replaceEntries(Map<String, Object> entries) {
        if (entries == null) {
            return false;
        }
        return commitBoth(editor -> {
            editor.clear();
            for (Map.Entry<String, Object> entry : entries.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isEmpty()) {
                    continue;
                }
                putTypedValue(editor, key, entry.getValue());
            }
        });
    }

    private boolean replaceBackupEntries(Map<String, Object> entries) {
        if (entries == null) {
            return false;
        }
        boolean replaced = replaceBackupEntries(preferences, entries);
        if (!replaced) {
            return false;
        }
        boolean migrated = migrateLegacyPackageConfigToAggregated();
        if (migrated) {
            mirrorLegacySharedPrefsFile();
        }
        return migrated;
    }

    private static boolean replaceBackupEntries(
            SharedPreferences targetPreferences,
            Map<String, Object> entries) {
        LinkedHashMap<String, Object> preservedEntries = new LinkedHashMap<>();
        copyExcludedBackupEntries(preservedEntries, targetPreferences.getAll());

        SharedPreferences.Editor editor = targetPreferences.edit();
        editor.clear();
        for (Map.Entry<String, Object> entry : preservedEntries.entrySet()) {
            putTypedValue(editor, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty() || isBackupExcludedKey(key)) {
                continue;
            }
            putTypedValue(editor, key, entry.getValue());
        }
        return editor.commit();
    }

    private static void copyEntries(Map<String, Object> target, Map<String, ?> source, boolean backupOnly) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty() || (backupOnly && !isBackupConfigKey(key))) {
                continue;
            }
            Object normalized = normalizeValue(entry.getValue());
            if (normalized != null) {
                target.put(key, normalized);
            }
        }
    }

    private static boolean isBackupConfigKey(String key) {
        return key != null
                && !isBackupExcludedKey(key)
                && !KEY_TARGET_PACKAGES.equals(key)
                && !isLegacyPackageConfigKey(key);
    }

    private static boolean isLegacyPackageConfigKey(String key) {
        if (key == null) {
            return false;
        }
        for (PackageConfigKeySpec spec : PACKAGE_CONFIG_KEYS) {
            if (spec.packageNameFromStorageKey(key, false) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBackupExcludedKey(String key) {
        if (key == null) {
            return false;
        }
        for (String prefix : BACKUP_EXCLUDED_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void copyExcludedBackupEntries(
            Map<String, Object> target,
            Map<String, ?> source) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty() || !isBackupExcludedKey(key)) {
                continue;
            }
            Object normalized = normalizeValue(entry.getValue());
            if (normalized != null) {
                target.put(key, normalized);
            }
        }
    }

    private boolean contains(String key) {
        return preferences.contains(key)
                || (fallbackPreferences != null && fallbackPreferences.contains(key));
    }

    private boolean containsPackageValue(String legacyKey, String packageKey) {
        return contains(legacyKey) || contains(packageKey);
    }

    private boolean containsInPrimary(String key) {
        return preferences.contains(key);
    }

    private boolean containsLocalOnly(String key) {
        return preferences.contains(key);
    }

    private int getInt(String key, int defaultValue) {
        Integer value = readPreferenceValue(key, prefs -> prefs.getInt(key, defaultValue));
        return value != null ? value : defaultValue;
    }

    private int getLocalOnlyInt(String key, int defaultValue) {
        if (!localOnlyPreferences.contains(key)) {
            return defaultValue;
        }
        Integer value = readPreferenceValue(
                localOnlyPreferences, prefs -> prefs.getInt(key, defaultValue));
        return value != null ? value : defaultValue;
    }

    private String getString(String key, String defaultValue) {
        String value = readPreferenceValue(key, prefs -> prefs.getString(key, defaultValue));
        return value != null ? value : defaultValue;
    }

    private String getPackageString(String legacyKey, String packageKey, String defaultValue) {
        if (contains(legacyKey)) {
            return getString(legacyKey, defaultValue);
        }
        return getString(packageKey, defaultValue);
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = readPreferenceValue(key, prefs -> prefs.getBoolean(key, defaultValue));
        return value != null ? value : defaultValue;
    }

    private boolean getPackageBoolean(String legacyKey, String packageKey, boolean defaultValue) {
        if (contains(legacyKey)) {
            return getBoolean(legacyKey, defaultValue);
        }
        return getBoolean(packageKey, defaultValue);
    }

    private boolean getLocalOnlyBoolean(String key, boolean defaultValue) {
        if (!localOnlyPreferences.contains(key)) {
            return defaultValue;
        }
        Boolean value = readPreferenceValue(
                localOnlyPreferences, prefs -> prefs.getBoolean(key, defaultValue));
        return value != null ? value : defaultValue;
    }

    private Integer getNullableInt(String key) {
        return readPreferenceValue(key, prefs -> prefs.getInt(key, 0));
    }

    private Integer getPackageNullableInt(String legacyKey, String packageKey) {
        if (contains(legacyKey)) {
            return getNullableInt(legacyKey);
        }
        return getNullableInt(packageKey);
    }

    private <T> T readPreferenceValue(String key, PreferenceReader<T> reader) {
        if (preferences.contains(key)) {
            return readPreferenceValue(preferences, reader);
        }
        if (fallbackPreferences != null && fallbackPreferences.contains(key)) {
            return readPreferenceValue(fallbackPreferences, reader);
        }
        return null;
    }

    private static <T> T readPreferenceValue(SharedPreferences source, PreferenceReader<T> reader) {
        try {
            return reader.read(source);
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    private boolean commitBoth(EditorAction action) {
        SharedPreferences.Editor primaryEditor = preferences.edit();
        action.apply(primaryEditor);
        boolean committed = primaryEditor.commit();
        if (committed) {
            mirrorLegacySharedPrefsFile();
        }
        return committed;
    }

    private boolean commitLocalOnly(EditorAction action) {
        SharedPreferences.Editor editor = localOnlyPreferences.edit();
        action.apply(editor);
        boolean committed = editor.commit();
        if (committed) {
            mirrorLegacySharedPrefsFile();
        }
        return committed;
    }


    private static boolean isLocalOnlyRuntimeDeliveryKey(String key) {
        for (String localOnlyKey : LOCAL_ONLY_RUNTIME_DELIVERY_KEYS) {
            if (localOnlyKey.equals(key)) {
                return true;
            }
        }
        for (String localOnlyPrefix : LOCAL_ONLY_RUNTIME_DELIVERY_PREFIXES) {
            if (key.startsWith(localOnlyPrefix)) {
                return true;
            }
        }
        return false;
    }

    private interface EditorAction {
        void apply(SharedPreferences.Editor editor);
    }

    private void mirrorLegacySharedPrefsFile() {
        if (legacySharedPrefsMirrorFile == null) {
            return;
        }
        try {
            // Some Android builds back SharedPreferences with an APEX-managed prefs
            // directory. Legacy XSharedPreferences still reads the conventional
            // /data/user/0/<pkg>/shared_prefs/<name>.xml path, so serialize the
            // committed logical preference snapshot there after each local commit.
            File parent = legacySharedPrefsMirrorFile.getParentFile();
            if (parent == null || (!parent.exists() && !parent.mkdirs() && !parent.exists())) {
                return;
            }
            File tempFile = new File(parent, legacySharedPrefsMirrorFile.getName() + ".tmp");
            writeSharedPreferencesXml(preferences.getAll(), tempFile);
            try {
                Files.move(
                        tempFile.toPath(),
                        legacySharedPrefsMirrorFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                        tempFile.toPath(),
                        legacySharedPrefsMirrorFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException throwable) {
            DpisLog.e("legacy shared prefs mirror failed", throwable);
        } catch (Throwable throwable) {
            DpisLog.e("legacy shared prefs mirror failed", throwable);
        }
    }

    static void writeSharedPreferencesXmlForTest(Map<String, ?> entries, File targetFile)
            throws IOException {
        writeSharedPreferencesXml(entries, targetFile);
    }

    static Map<String, Object> readSharedPreferencesXmlForTest(File sourceFile)
            throws Exception {
        return readSharedPreferencesXml(sourceFile);
    }

    private static void writeSharedPreferencesXml(Map<String, ?> entries, File targetFile)
            throws IOException {
        Files.write(
                targetFile.toPath(),
                sharedPreferencesXml(entries).getBytes(StandardCharsets.UTF_8));
    }

    private static String sharedPreferencesXml(Map<String, ?> entries) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n");
        builder.append("<map>\n");
        if (entries != null) {
            for (Map.Entry<String, ?> entry : entries.entrySet()) {
                appendPreferenceXmlEntry(builder, entry.getKey(), normalizeValue(entry.getValue()));
            }
        }
        builder.append("</map>\n");
        return builder.toString();
    }

    private static Map<String, Object> readSharedPreferencesXml(File sourceFile)
            throws Exception {
        LinkedHashMap<String, Object> entries = new LinkedHashMap<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        setXmlFeatureIfSupported(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setXmlFeatureIfSupported(factory, "http://xml.org/sax/features/external-general-entities", false);
        setXmlFeatureIfSupported(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        Element root = factory.newDocumentBuilder().parse(sourceFile).getDocumentElement();
        if (root == null || !"map".equals(root.getTagName())) {
            return entries;
        }
        NodeList children = root.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (!(node instanceof Element element)) {
                continue;
            }
            String key = element.getAttribute("name");
            if (key == null || key.isEmpty()) {
                continue;
            }
            Object value = readSharedPreferencesXmlValue(element);
            if (value != null) {
                entries.put(key, value);
            }
        }
        return entries;
    }

    private static void setXmlFeatureIfSupported(
            DocumentBuilderFactory factory,
            String feature,
            boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException ignored) {
            // Android XML implementations vary; SharedPreferences XML is app-owned.
        }
    }

    private static Object readSharedPreferencesXmlValue(Element element) {
        String tag = element.getTagName();
        try {
            return switch (tag) {
                case "string" -> element.getTextContent();
                case "int" -> Integer.parseInt(element.getAttribute("value"));
                case "long" -> Long.parseLong(element.getAttribute("value"));
                case "float" -> Float.parseFloat(element.getAttribute("value"));
                case "boolean" -> Boolean.parseBoolean(element.getAttribute("value"));
                case "set" -> readSharedPreferencesXmlStringSet(element);
                default -> null;
            };
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static LinkedHashSet<String> readSharedPreferencesXmlStringSet(Element element) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element child && "string".equals(child.getTagName())) {
                values.add(child.getTextContent());
            }
        }
        return values;
    }

    private static void appendPreferenceXmlEntry(StringBuilder builder, String key, Object value) {
        if (key == null || key.isEmpty() || value == null) {
            return;
        }
        String escapedKey = escapeXml(key);
        if (value instanceof String stringValue) {
            builder.append("    <string name=\"")
                    .append(escapedKey)
                    .append("\">")
                    .append(escapeXml(stringValue))
                    .append("</string>\n");
        } else if (value instanceof Integer intValue) {
            appendPrimitiveXmlEntry(builder, "int", escapedKey, Integer.toString(intValue));
        } else if (value instanceof Long longValue) {
            appendPrimitiveXmlEntry(builder, "long", escapedKey, Long.toString(longValue));
        } else if (value instanceof Float floatValue) {
            appendPrimitiveXmlEntry(builder, "float", escapedKey, Float.toString(floatValue));
        } else if (value instanceof Boolean booleanValue) {
            appendPrimitiveXmlEntry(builder, "boolean", escapedKey, Boolean.toString(booleanValue));
        } else if (value instanceof Set<?> setValue) {
            if (setValue.isEmpty()) {
                builder.append("    <set name=\"").append(escapedKey).append("\" />\n");
                return;
            }
            builder.append("    <set name=\"").append(escapedKey).append("\">\n");
            for (Object item : setValue) {
                if (item instanceof String stringItem) {
                    builder.append("        <string>")
                            .append(escapeXml(stringItem))
                            .append("</string>\n");
                }
            }
            builder.append("    </set>\n");
        }
    }

    private static void appendPrimitiveXmlEntry(
            StringBuilder builder,
            String tag,
            String escapedKey,
            String value) {
        builder.append("    <")
                .append(tag)
                .append(" name=\"")
                .append(escapedKey)
                .append("\" value=\"")
                .append(escapeXml(value))
                .append("\" />\n");
    }

    private static String escapeXml(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '&' -> builder.append("&amp;");
                case '<' -> builder.append("&lt;");
                case '>' -> builder.append("&gt;");
                case '"' -> builder.append("&quot;");
                case '\'' -> builder.append("&apos;");
                default -> builder.append(character);
            }
        }
        return builder.toString();
    }

    private interface PreferenceReader<T> {
        T read(SharedPreferences preferences);
    }

    private interface PackageConfigKeyFactory {
        String keyForPackage(String packageName);
    }

    private static final class PackageConfigKeySpec {
        private final String prefix;
        private final String suffix;
        private final Integer minIntValue;
        private final Integer maxIntValue;
        private final Boolean requiredBooleanValue;
        private final Function<String, String> keyFactory;
        private final Predicate<String> packagePredicate;
        private final Predicate<Object> configuredValuePredicate;

        private PackageConfigKeySpec(
                String prefix,
                String suffix,
                Integer minIntValue,
                Integer maxIntValue,
                Boolean requiredBooleanValue,
                Function<String, String> keyFactory,
                Predicate<String> packagePredicate,
                Predicate<Object> configuredValuePredicate) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.minIntValue = minIntValue;
            this.maxIntValue = maxIntValue;
            this.requiredBooleanValue = requiredBooleanValue;
            this.keyFactory = keyFactory;
            this.packagePredicate = packagePredicate;
            this.configuredValuePredicate = configuredValuePredicate;
        }

        static PackageConfigKeySpec any(
                String prefix,
                String suffix,
                Function<String, String> keyFactory) {
            return new PackageConfigKeySpec(
                    prefix, suffix, null, null, null, keyFactory, packageName -> true,
                    value -> value != null);
        }

        static PackageConfigKeySpec string(
                String prefix,
                String suffix,
                Function<String, String> keyFactory) {
            return any(prefix, suffix, keyFactory);
        }

        static PackageConfigKeySpec string(
                String prefix,
                String suffix,
                Function<String, String> keyFactory,
                Predicate<Object> configuredValuePredicate) {
            return new PackageConfigKeySpec(
                    prefix, suffix, null, null, null, keyFactory, packageName -> true,
                    configuredValuePredicate);
        }

        static PackageConfigKeySpec positiveInteger(
                String prefix,
                String suffix,
                Function<String, String> keyFactory) {
            return rangedInteger(prefix, suffix, 1, Integer.MAX_VALUE, keyFactory);
        }

        static PackageConfigKeySpec rangedInteger(
                String prefix,
                String suffix,
                int minIntValue,
                int maxIntValue,
                Function<String, String> keyFactory) {
            return rangedInteger(prefix, suffix, minIntValue, maxIntValue,
                    keyFactory, packageName -> true);
        }

        static PackageConfigKeySpec rangedInteger(
                String prefix,
                String suffix,
                int minIntValue,
                int maxIntValue,
                Function<String, String> keyFactory,
                Predicate<String> packagePredicate) {
            return new PackageConfigKeySpec(
                    prefix,
                    suffix,
                    minIntValue,
                    maxIntValue,
                    null,
                    keyFactory,
                    packagePredicate,
                    value -> true);
        }

        static PackageConfigKeySpec booleanValue(
                String prefix,
                String suffix,
                boolean configuredValue,
                Function<String, String> keyFactory) {
            return new PackageConfigKeySpec(
                    prefix,
                    suffix,
                    null,
                    null,
                    configuredValue,
                    keyFactory,
                    packageName -> true,
                    value -> true);
        }

        String keyForPackage(String packageName) {
            return keyFactory.apply(packageName);
        }

        String packageNameFromKey(String key, Object value) {
            String packageName = packageNameBetween(key, prefix, suffix);
            return packageName != null && appliesTo(packageName) && isConfiguredValue(value)
                    ? packageName
                    : null;
        }

        boolean appliesTo(String packageName) {
            return packageName != null && packagePredicate.test(packageName);
        }

        String packageNameFromStorageKey(String key, boolean requireApplicablePackage) {
            String packageName = packageNameBetween(key, prefix, suffix);
            if (packageName == null) {
                return null;
            }
            return !requireApplicablePackage || appliesTo(packageName) ? packageName : null;
        }

        boolean expectsInteger() {
            return minIntValue != null && maxIntValue != null;
        }

        boolean expectsBoolean() {
            return requiredBooleanValue != null;
        }

        boolean isConfiguredValue(Object value) {
            if (requiredBooleanValue != null) {
                return value instanceof Boolean boolValue
                        && requiredBooleanValue.equals(boolValue);
            }
            if (configuredValuePredicate != null && !configuredValuePredicate.test(value)) {
                return false;
            }
            if (minIntValue == null || maxIntValue == null) {
                return value != null;
            }
            if (!(value instanceof Integer intValue)) {
                return false;
            }
            return intValue >= minIntValue && intValue <= maxIntValue;
        }
    }

    @SuppressWarnings("unchecked")
    private static void putTypedValue(SharedPreferences.Editor editor, String key, Object value) {
        if (value == null) {
            editor.remove(key);
            return;
        }
        if (value instanceof String typed) {
            editor.putString(key, typed);
            return;
        }
        if (value instanceof Integer typed) {
            editor.putInt(key, typed);
            return;
        }
        if (value instanceof Long typed) {
            editor.putLong(key, typed);
            return;
        }
        if (value instanceof Float typed) {
            editor.putFloat(key, typed);
            return;
        }
        if (value instanceof Boolean typed) {
            editor.putBoolean(key, typed);
            return;
        }
        if (value instanceof Set<?> typed) {
            LinkedHashSet<String> stringSet = new LinkedHashSet<>();
            for (Object item : typed) {
                if (!(item instanceof String text)) {
                    continue;
                }
                stringSet.add(text);
            }
            editor.putStringSet(key, stringSet);
            return;
        }
        throw new IllegalArgumentException("Unsupported preference value type: " + value.getClass());
    }

    @SuppressWarnings("unchecked")
    private static Object normalizeValue(Object value) {
        if (value == null
                || value instanceof String
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Set<?> typed) {
            LinkedHashSet<String> stringSet = new LinkedHashSet<>();
            for (Object item : typed) {
                if (!(item instanceof String text)) {
                    return null;
                }
                stringSet.add(text);
            }
            return stringSet;
        }
        return null;
    }

    private static Integer normalizeViewportWidth(Integer widthDp) {
        if (widthDp == null || widthDp < MIN_VIEWPORT_WIDTH_DP) {
            return null;
        }
        return widthDp;
    }

    private static Integer normalizeViewportScaleMilliPercent(Integer scaleMilliPercent) {
        if (scaleMilliPercent == null
                || scaleMilliPercent < MIN_VIEWPORT_SCALE_MILLI_PERCENT
                || scaleMilliPercent > MAX_VIEWPORT_SCALE_MILLI_PERCENT) {
            return null;
        }
        return scaleMilliPercent;
    }

    private static Integer normalizeViewportScalePermille(Integer scalePermille) {
        if (scalePermille == null
                || scalePermille < MIN_VIEWPORT_SCALE_PERMILLE
                || scalePermille > MAX_VIEWPORT_SCALE_PERMILLE) {
            return null;
        }
        return scalePermille;
    }

    private static Integer normalizeFontScalePercent(Integer percent) {
        if (percent == null
                || percent < MIN_FONT_SCALE_PERCENT
                || percent > MAX_FONT_SCALE_PERCENT) {
            return null;
        }
        return percent;
    }

    private static String normalizeTypefaceId(String typefaceId) {
        return normalizeNonEmptyString(typefaceId);
    }

    private static String normalizeNonEmptyString(String value) {
        if (value == null) {
            return null;
        }
        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            return null;
        }
        return normalizedValue;
    }

    private static String keyForViewportWidth(String packageName) {
        return "viewport." + packageName + ".width_dp";
    }

    private static String keyForPackageViewportWidth(String packageName) {
        return "package_config." + packageName + ".viewport.width_dp";
    }

    private static String keyForViewportTargetType(String packageName) {
        return "viewport." + packageName + ".target_type";
    }

    private static String keyForPackageViewportTargetType(String packageName) {
        return "package_config." + packageName + ".viewport.target_type";
    }

    private static String keyForViewportScalePermille(String packageName) {
        return "viewport." + packageName + ".scale_permille";
    }

    private static String keyForViewportScaleMilliPercent(String packageName) {
        return "viewport." + packageName + ".scale_milli_percent";
    }

    private static String keyForPackageViewportScalePermille(String packageName) {
        return "package_config." + packageName + ".viewport.scale_permille";
    }

    private static String keyForPackageViewportScaleMilliPercent(String packageName) {
        return "package_config." + packageName + ".viewport.scale_milli_percent";
    }

    private static String keyForViewportMode(String packageName) {
        return "viewport." + packageName + ".mode";
    }

    private static String keyForPackageViewportMode(String packageName) {
        return "package_config." + packageName + ".viewport.mode";
    }

    private static String keyForFontScale(String packageName) {
        return "font." + packageName + ".scale_percent";
    }

    private static String keyForPackageFontScale(String packageName) {
        return "package_config." + packageName + ".font.scale_percent";
    }

    private static String keyForTypefaceId(String packageName) {
        return "font." + packageName + ".typeface_id";
    }

    private static String keyForPackageTypefaceId(String packageName) {
        return "package_config." + packageName + ".font.typeface_id";
    }

    private static String keyForFontMode(String packageName) {
        return "font." + packageName + ".mode";
    }

    private static String keyForPackageFontMode(String packageName) {
        return "package_config." + packageName + ".font.mode";
    }

    private static String keyForDpisEnabled(String packageName) {
        return "target." + packageName + ".dpis_enabled";
    }

    private static String keyForPackageDpisEnabled(String packageName) {
        return "package_config." + packageName + ".target.dpis_enabled";
    }

    private static String keyForFontHookDomains(String packageName) {
        return "font." + packageName + ".hook_domains";
    }

    private static String keyForPackageFontHookDomains(String packageName) {
        return "package_config." + packageName + ".font.hook_domains";
    }

    private static String keyForWechatDpi(String packageName) {
        return "wechat." + packageName + ".dpi";
    }

    private static String keyForPackageWechatDpi(String packageName) {
        return "package_config." + packageName + ".app.wechat_dpi";
    }
}
