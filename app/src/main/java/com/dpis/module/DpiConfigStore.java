package com.dpis.module;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class DpiConfigStore {
    private static final int MIN_VIEWPORT_WIDTH_DP = 1;
    private static final int MIN_VIEWPORT_SCALE_PERMILLE = ViewportTargetSpec.MIN_SCALE_PERMILLE;
    private static final int MAX_VIEWPORT_SCALE_PERMILLE = ViewportTargetSpec.MAX_SCALE_PERMILLE;
    private static final int MIN_FONT_SCALE_PERCENT = 50;
    private static final int MAX_FONT_SCALE_PERCENT = 300;

    static final String GROUP = "dpi_config";
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
    static final String KEY_STARTUP_DISCLAIMER_ACCEPTED = "ui.startup_disclaimer_accepted";

    private final SharedPreferences preferences;
    private final SharedPreferences mirrorPreferences;

    DpiConfigStore(SharedPreferences preferences) {
        this(preferences, null);
    }

    DpiConfigStore(SharedPreferences preferences, SharedPreferences mirrorPreferences) {
        this.preferences = preferences;
        this.mirrorPreferences = mirrorPreferences;
    }

    Set<String> getConfiguredPackages() {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        if (preferences.contains(KEY_TARGET_PACKAGES)) {
            Set<String> primaryPackages = preferences.getStringSet(KEY_TARGET_PACKAGES, Collections.emptySet());
            if (primaryPackages != null) {
                packages.addAll(primaryPackages);
            }
            return new LinkedHashSet<>(packages);
        }
        if (mirrorPreferences != null && mirrorPreferences.contains(KEY_TARGET_PACKAGES)) {
            Set<String> backupPackages = mirrorPreferences.getStringSet(KEY_TARGET_PACKAGES, Collections.emptySet());
            if (backupPackages != null) {
                packages.addAll(backupPackages);
            }
        }
        return new LinkedHashSet<>(packages);
    }

    Integer getTargetViewportWidthDp(String packageName) {
        String key = keyForViewportWidth(packageName);
        if (!contains(key)) {
            return null;
        }
        Integer widthDp = getNullableInt(key);
        return normalizeViewportWidth(widthDp);
    }

    Integer getTargetViewportScalePermille(String packageName) {
        String key = keyForViewportScalePermille(packageName);
        if (!contains(key)) {
            return null;
        }
        return normalizeViewportScalePermille(getNullableInt(key));
    }

    String getTargetViewportType(String packageName) {
        String key = keyForViewportTargetType(packageName);
        if (!contains(key)) {
            return ViewportTargetType.OFF;
        }
        return ViewportTargetType.normalize(getString(key, ViewportTargetType.OFF));
    }

    ViewportTargetSpec getTargetViewportSpec(String packageName) {
        String typeKey = keyForViewportTargetType(packageName);
        String type = contains(typeKey)
                ? ViewportTargetType.normalize(getString(typeKey, ViewportTargetType.OFF))
                : ViewportTargetType.OFF;
        if (ViewportTargetType.RELATIVE_SCALE.equals(type)) {
            Integer scalePermille = getTargetViewportScalePermille(packageName);
            return scalePermille != null
                    ? ViewportTargetSpec.relativeScale(scalePermille)
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
        if (contains(key)) {
            return ViewportApplyMode.normalize(getString(key, ViewportApplyMode.OFF));
        }
        if (getTargetViewportWidthDp(packageName) != null
                && !contains(keyForViewportTargetType(packageName))) {
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
        if (!contains(key)) {
            return null;
        }
        Integer percent = getNullableInt(key);
        return normalizeFontScalePercent(percent);
    }

    String getTargetTypefaceId(String packageName) {
        String key = keyForTypefaceId(packageName);
        if (!contains(key)) {
            return null;
        }
        return normalizeTypefaceId(getString(key, null));
    }

    Integer getWechatTargetField(String packageName) {
        if (!WechatTargetFieldConfig.appliesTo(packageName)) {
            return null;
        }
        String key = keyForWechatTargetField(packageName);
        if (!contains(key)) {
            return null;
        }
        return WechatTargetFieldConfig.normalize(getNullableInt(key));
    }

    boolean hasTargetAppSpecificConfig(String packageName) {
        return getWechatTargetField(packageName) != null;
    }

    String getTargetFontApplyMode(String packageName) {
        String key = keyForFontMode(packageName);
        if (contains(key)) {
            return FontApplyMode.normalize(getString(key, FontApplyMode.OFF));
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

    boolean isLauncherIconHidden() {
        return getBoolean(KEY_HIDE_LAUNCHER_ICON, false);
    }

    boolean hasLauncherIconHidden() {
        return containsInPrimary(KEY_HIDE_LAUNCHER_ICON);
    }

    boolean setLauncherIconHidden(boolean hidden) {
        return commitBoth(editor -> editor.putBoolean(KEY_HIDE_LAUNCHER_ICON, hidden));
    }

    boolean isStartupDisclaimerAccepted() {
        if (preferences.contains(KEY_STARTUP_DISCLAIMER_ACCEPTED)) {
            return preferences.getBoolean(KEY_STARTUP_DISCLAIMER_ACCEPTED, false)
                    || (mirrorPreferences != null
                    && mirrorPreferences.getBoolean(KEY_STARTUP_DISCLAIMER_ACCEPTED, false));
        }
        return getBoolean(KEY_STARTUP_DISCLAIMER_ACCEPTED, false);
    }

    boolean setStartupDisclaimerAccepted(boolean accepted) {
        return commitBoth(editor -> editor.putBoolean(KEY_STARTUP_DISCLAIMER_ACCEPTED, accepted));
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
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putString(keyForViewportTargetType(packageName), ViewportTargetType.ABSOLUTE_DP)
                .putInt(keyForViewportWidth(packageName), normalizedWidthDp));
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
            editor.putString(keyForViewportTargetType(packageName), normalized.type());
            if (normalized.isRelativeScale()) {
                editor.putInt(keyForViewportScalePermille(packageName), normalized.scalePermille());
                return;
            }
            editor.putInt(keyForViewportWidth(packageName), normalized.absoluteWidthDp());
        });
    }

    boolean setTargetViewportWidthDraft(String packageName, Integer widthDp) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        if (widthDp != null && widthDp <= 0) {
            return true;
        }
        String widthKey = keyForViewportWidth(packageName);
        if (widthDp == null) {
            LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
            if (!hasAnyPackageConfigAfterRemoving(packageName, widthKey)) {
                packages.remove(packageName);
            }
            return commitBoth(editor -> editor
                    .putStringSet(KEY_TARGET_PACKAGES, packages)
                    .remove(widthKey));
        }
        Integer normalizedWidthDp = normalizeViewportWidth(widthDp);
        if (normalizedWidthDp == null) {
            return true;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(widthKey, normalizedWidthDp));
    }

    boolean setTargetViewportScalePermilleDraft(String packageName, Integer scalePermille) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        if (scalePermille != null && scalePermille <= 0) {
            return true;
        }
        String scaleKey = keyForViewportScalePermille(packageName);
        if (scalePermille == null) {
            LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
            if (!hasAnyPackageConfigAfterRemoving(packageName, scaleKey)) {
                packages.remove(packageName);
            }
            return commitBoth(editor -> editor
                    .putStringSet(KEY_TARGET_PACKAGES, packages)
                    .remove(scaleKey));
        }
        Integer normalizedScalePermille = normalizeViewportScalePermille(scalePermille);
        if (normalizedScalePermille == null) {
            return true;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(scaleKey, normalizedScalePermille));
    }

    boolean clearTargetViewportWidthDp(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName,
                keyForViewportWidth(packageName),
                keyForViewportTargetType(packageName),
                keyForViewportScalePermille(packageName),
                keyForViewportMode(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForViewportWidth(packageName))
                .remove(keyForViewportTargetType(packageName))
                .remove(keyForViewportScalePermille(packageName))
                .remove(keyForViewportMode(packageName)));
    }

    boolean setTargetViewportApplyMode(String packageName, String mode) {
        String normalized = ViewportApplyMode.normalize(mode);
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!ViewportApplyMode.isEnabled(normalized)) {
            if (!hasAnyPackageConfigAfterRemoving(packageName, keyForViewportMode(packageName))) {
                packages.remove(packageName);
            }
            return commitBoth(editor -> editor
                    .putStringSet(KEY_TARGET_PACKAGES, packages)
                    .remove(keyForViewportMode(packageName)));
        }
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putString(keyForViewportMode(packageName), normalized));
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
                .putInt(keyForFontScale(packageName), normalizedPercent));
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
                .putString(keyForTypefaceId(packageName), normalizedTypefaceId));
    }

    boolean setWechatTargetField(String packageName, Integer targetField) {
        if (!WechatTargetFieldConfig.appliesTo(packageName)) {
            return true;
        }
        Integer normalized = WechatTargetFieldConfig.normalize(targetField);
        if (normalized == null) {
            return clearWechatTargetField(packageName);
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(keyForWechatTargetField(packageName), normalized));
    }

    boolean migrateWechatViewportToTargetFieldIfNeeded() {
        String packageName = WechatTargetFieldConfig.PACKAGE_NAME;
        if (getWechatTargetField(packageName) != null) {
            return true;
        }
        Integer legacyViewportWidth = getTargetViewportWidthDp(packageName);
        Integer targetField = WechatTargetFieldConfig.normalize(legacyViewportWidth);
        if (targetField == null) {
            return true;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(keyForWechatTargetField(packageName), targetField)
                .remove(keyForViewportWidth(packageName))
                .remove(keyForViewportTargetType(packageName))
                .remove(keyForViewportScalePermille(packageName))
                .remove(keyForViewportMode(packageName)));
    }

    boolean setTargetFontApplyMode(String packageName, String mode) {
        String normalized = FontApplyMode.normalize(mode);
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (FontApplyMode.OFF.equals(normalized)) {
            if (!hasAnyPackageConfigAfterRemoving(packageName, keyForFontMode(packageName))) {
                packages.remove(packageName);
            }
            return commitBoth(editor -> editor
                    .putStringSet(KEY_TARGET_PACKAGES, packages)
                    .remove(keyForFontMode(packageName)));
        }
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putString(keyForFontMode(packageName), normalized));
    }

    boolean clearTargetFontScalePercent(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName, keyForFontScale(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForFontScale(packageName)));
    }

    boolean clearTargetTypefaceId(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName, keyForTypefaceId(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForTypefaceId(packageName)));
    }

    boolean clearWechatTargetField(String packageName) {
        if (!WechatTargetFieldConfig.appliesTo(packageName)) {
            return true;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName, keyForWechatTargetField(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForWechatTargetField(packageName)));
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
        return getBoolean(keyForDpisEnabled(packageName), true);
    }

    boolean setTargetDpisEnabled(String packageName, boolean enabled) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (enabled) {
            if (!hasAnyPackageConfigAfterRemoving(packageName, keyForDpisEnabled(packageName))) {
                packages.remove(packageName);
            }
            return commitBoth(editor -> editor
                    .putStringSet(KEY_TARGET_PACKAGES, packages)
                    .remove(keyForDpisEnabled(packageName)));
        }
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putBoolean(keyForDpisEnabled(packageName), false));
    }

    boolean clearTargetPackageConfig(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.remove(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForViewportWidth(packageName))
                .remove(keyForViewportTargetType(packageName))
                .remove(keyForViewportScalePermille(packageName))
                .remove(keyForViewportMode(packageName))
                .remove(keyForFontScale(packageName))
                .remove(keyForTypefaceId(packageName))
                .remove(keyForFontMode(packageName))
                .remove(keyForDpisEnabled(packageName))
                .remove(keyForFontHookDomains(packageName))
                .remove(keyForWechatTargetField(packageName)));
    }

    String getPackageFontHookDomainsRaw(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return null;
        }
        String key = keyForFontHookDomains(packageName);
        if (!contains(key)) {
            return null;
        }
        return getString(key, null);
    }

    boolean setPackageFontHookDomainsRaw(String packageName, String rawValue) {
        if (packageName == null || packageName.isBlank() || rawValue == null) {
            return false;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putString(keyForFontHookDomains(packageName), rawValue));
    }

    boolean clearPackageFontHookDomainsRaw(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!hasAnyPackageConfigAfterRemoving(packageName, keyForFontHookDomains(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForFontHookDomains(packageName)));
    }

    boolean hasRealPackageConfig(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        return getConfiguredPackages().contains(packageName)
                || hasAnyPackageConfigAfterRemoving(packageName);
    }

    TemplateConfigValue readPackageTemplateConfigValue(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return TemplateConfigValue.EMPTY;
        }
        return new TemplateConfigValue(
                getTargetViewportSpec(packageName),
                getTargetViewportApplyMode(packageName),
                getTargetFontScalePercent(packageName),
                getTargetFontApplyMode(packageName),
                getTargetTypefaceId(packageName),
                getPackageFontHookDomainsRaw(packageName));
    }

    boolean writePackageTemplateConfigValue(String packageName, TemplateConfigValue value) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        TemplateConfigValue normalized = value != null ? value : TemplateConfigValue.EMPTY;
        if (!normalized.hasAnyValue()) {
            LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
            if (hasAnyPackageConfigAfterRemoving(packageName, templateConfigKeysForPackage(packageName))) {
                packages.add(packageName);
            } else {
                packages.remove(packageName);
            }
            return commitBoth(editor -> {
                editor.putStringSet(KEY_TARGET_PACKAGES, packages);
                removePackageTemplateConfigKeys(editor, packageName);
            });
        }
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> {
            editor.putStringSet(KEY_TARGET_PACKAGES, packages);
            removePackageTemplateConfigKeys(editor, packageName);
            if (normalized.viewportTargetSpec.isEnabled()) {
                editor.putString(
                        keyForViewportTargetType(packageName),
                        normalized.viewportTargetSpec.type());
                if (normalized.viewportTargetSpec.isRelativeScale()) {
                    editor.putInt(
                            keyForViewportScalePermille(packageName),
                            normalized.viewportTargetSpec.scalePermille());
                } else {
                    editor.putInt(
                            keyForViewportWidth(packageName),
                            normalized.viewportTargetSpec.absoluteWidthDp());
                }
            }
            if (ViewportApplyMode.isEnabled(normalized.viewportApplyMode)) {
                editor.putString(keyForViewportMode(packageName), normalized.viewportApplyMode);
            }
            if (normalized.fontScalePercent != null) {
                editor.putInt(keyForFontScale(packageName), normalized.fontScalePercent);
            }
            if (FontApplyMode.isEnabled(normalized.fontApplyMode)) {
                editor.putString(keyForFontMode(packageName), normalized.fontApplyMode);
            }
            if (normalized.typefaceId != null) {
                editor.putString(keyForTypefaceId(packageName), normalized.typefaceId);
            }
            if (normalized.fontHookDomainsRaw != null) {
                editor.putString(keyForFontHookDomains(packageName), normalized.fontHookDomainsRaw);
            }
        });
    }

    private static void removePackageTemplateConfigKeys(
            SharedPreferences.Editor editor,
            String packageName) {
        for (String key : templateConfigKeysForPackage(packageName)) {
            editor.remove(key);
        }
    }

    private static String[] templateConfigKeysForPackage(String packageName) {
        return new String[] {
                keyForViewportWidth(packageName),
                keyForViewportTargetType(packageName),
                keyForViewportScalePermille(packageName),
                keyForViewportMode(packageName),
                keyForFontScale(packageName),
                keyForTypefaceId(packageName),
                keyForFontMode(packageName),
                keyForFontHookDomains(packageName)
        };
    }

    private boolean hasAnyPackageConfigAfterRemoving(String packageName, String... removedKeys) {
        String viewportWidthKey = keyForViewportWidth(packageName);
        if (!isRemovedKey(viewportWidthKey, removedKeys)
                && getTargetViewportWidthDp(packageName) != null) {
            return true;
        }
        String viewportTargetTypeKey = keyForViewportTargetType(packageName);
        String viewportScaleKey = keyForViewportScalePermille(packageName);
        if ((!isRemovedKey(viewportTargetTypeKey, removedKeys)
                || !isRemovedKey(viewportScaleKey, removedKeys))
                && getTargetViewportSpec(packageName).isEnabled()) {
            return true;
        }
        String viewportModeKey = keyForViewportMode(packageName);
        if (!isRemovedKey(viewportModeKey, removedKeys)
                && contains(viewportModeKey)) {
            return true;
        }
        String fontScaleKey = keyForFontScale(packageName);
        if (!isRemovedKey(fontScaleKey, removedKeys)
                && getTargetFontScalePercent(packageName) != null) {
            return true;
        }
        String typefaceIdKey = keyForTypefaceId(packageName);
        if (!isRemovedKey(typefaceIdKey, removedKeys)
                && getTargetTypefaceId(packageName) != null) {
            return true;
        }
        String fontModeKey = keyForFontMode(packageName);
        if (!isRemovedKey(fontModeKey, removedKeys)
                && contains(fontModeKey)) {
            return true;
        }
        String dpisEnabledKey = keyForDpisEnabled(packageName);
        if (!isRemovedKey(dpisEnabledKey, removedKeys)
                && contains(dpisEnabledKey)) {
            return true;
        }
        String wechatTargetFieldKey = keyForWechatTargetField(packageName);
        if (!isRemovedKey(wechatTargetFieldKey, removedKeys)
                && getWechatTargetField(packageName) != null) {
            return true;
        }
        String hookDomainsKey = keyForFontHookDomains(packageName);
        if (!isRemovedKey(hookDomainsKey, removedKeys)
                && contains(hookDomainsKey)) {
            return true;
        }
        return false;
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
        if (mirrorPreferences != null) {
            copyEntries(snapshot, mirrorPreferences.getAll(), false);
        }
        copyEntries(snapshot, preferences.getAll(), false);
        return snapshot;
    }

    Map<String, Object> snapshotBackup() {
        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        if (mirrorPreferences != null) {
            copyEntries(snapshot, mirrorPreferences.getAll(), true);
        }
        copyEntries(snapshot, preferences.getAll(), true);
        return snapshot;
    }

    boolean replaceAll(Map<String, Object> entries) {
        return replaceEntries(entries, false);
    }

    boolean replaceBackup(Map<String, Object> entries) {
        return replaceEntries(entries, true);
    }

    private boolean replaceEntries(Map<String, Object> entries, boolean backupOnly) {
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
                if (backupOnly && !isBackupConfigKey(key)) {
                    continue;
                }
                putTypedValue(editor, key, entry.getValue());
            }
        });
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
                && !key.startsWith("default_config.")
                && !key.startsWith("template.")
                && !key.startsWith("font.library.")
                && !key.startsWith("font.debug.")
                && !key.startsWith("runtime.");
    }

    private boolean contains(String key) {
        return preferences.contains(key)
                || (mirrorPreferences != null && mirrorPreferences.contains(key));
    }

    private boolean containsInPrimary(String key) {
        return preferences.contains(key);
    }

    private int getInt(String key, int defaultValue) {
        Integer value = readPreferenceValue(key, prefs -> prefs.getInt(key, defaultValue));
        return value != null ? value : defaultValue;
    }

    private String getString(String key, String defaultValue) {
        String value = readPreferenceValue(key, prefs -> prefs.getString(key, defaultValue));
        return value != null ? value : defaultValue;
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = readPreferenceValue(key, prefs -> prefs.getBoolean(key, defaultValue));
        return value != null ? value : defaultValue;
    }

    private Integer getNullableInt(String key) {
        return readPreferenceValue(key, prefs -> prefs.getInt(key, 0));
    }

    private <T> T readPreferenceValue(String key, PreferenceReader<T> reader) {
        if (preferences.contains(key)) {
            return readPreferenceValue(preferences, reader);
        }
        if (mirrorPreferences != null && mirrorPreferences.contains(key)) {
            return readPreferenceValue(mirrorPreferences, reader);
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
        boolean primaryCommitted = primaryEditor.commit();
        if (mirrorPreferences == null) {
            return primaryCommitted;
        }
        try {
            SharedPreferences.Editor mirrorEditor = mirrorPreferences.edit();
            action.apply(mirrorEditor);
            return primaryCommitted && mirrorEditor.commit();
        } catch (UnsupportedOperationException ignored) {
            return primaryCommitted;
        }
    }

    private interface EditorAction {
        void apply(SharedPreferences.Editor editor);
    }

    private interface PreferenceReader<T> {
        T read(SharedPreferences preferences);
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
        if (typefaceId == null) {
            return null;
        }
        String normalizedTypefaceId = typefaceId.trim();
        if (normalizedTypefaceId.isEmpty()) {
            return null;
        }
        return normalizedTypefaceId;
    }

    private static String keyForViewportWidth(String packageName) {
        return "viewport." + packageName + ".width_dp";
    }

    private static String keyForViewportTargetType(String packageName) {
        return "viewport." + packageName + ".target_type";
    }

    private static String keyForViewportScalePermille(String packageName) {
        return "viewport." + packageName + ".scale_permille";
    }

    private static String keyForViewportMode(String packageName) {
        return "viewport." + packageName + ".mode";
    }

    private static String keyForFontScale(String packageName) {
        return "font." + packageName + ".scale_percent";
    }

    private static String keyForTypefaceId(String packageName) {
        return "font." + packageName + ".typeface_id";
    }

    private static String keyForFontMode(String packageName) {
        return "font." + packageName + ".mode";
    }

    private static String keyForDpisEnabled(String packageName) {
        return "target." + packageName + ".dpis_enabled";
    }

    private static String keyForFontHookDomains(String packageName) {
        return "font." + packageName + ".hook_domains";
    }

    private static String keyForWechatTargetField(String packageName) {
        return "wechat." + packageName + ".target_field";
    }
}
