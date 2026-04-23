package com.dpis.module;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class DpiConfigStore {
    static final String GROUP = "dpi_config";
    static final String KEY_TARGET_PACKAGES = "target_packages";
    static final String KEY_SYSTEM_SERVER_HOOKS_ENABLED = "system_server.hooks_enabled";
    static final String KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED = "system_server.safe_mode_enabled";
    static final String KEY_GLOBAL_LOG_ENABLED = "global.log_enabled";
    static final String KEY_FONT_DEBUG_OVERLAY_ENABLED = "font.debug.overlay_enabled";
    static final String KEY_FONT_DEBUG_SELECTED_MODE = "font.debug.selected_mode";
    static final String KEY_FONT_DEBUG_SELECTED_WINDOW = "font.debug.selected_window";
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
        Set<String> packages;
        if (preferences.contains(KEY_TARGET_PACKAGES)) {
            packages = preferences.getStringSet(KEY_TARGET_PACKAGES, Collections.emptySet());
        } else if (mirrorPreferences != null) {
            packages = mirrorPreferences.getStringSet(KEY_TARGET_PACKAGES, Collections.emptySet());
        } else {
            packages = Collections.emptySet();
        }
        if (packages == null) {
            packages = Collections.emptySet();
        }
        return new LinkedHashSet<>(packages);
    }

    Integer getTargetViewportWidthDp(String packageName) {
        String key = keyForViewportWidth(packageName);
        if (!contains(key)) {
            return null;
        }
        return getInt(key, 0);
    }

    String getTargetViewportApplyMode(String packageName) {
        String key = keyForViewportMode(packageName);
        if (contains(key)) {
            return ViewportApplyMode.normalize(getString(key, ViewportApplyMode.OFF));
        }
        if (contains(keyForViewportWidth(packageName))) {
            // 历史配置迁移：已有宽度但无模式时，默认视为系统伪装。
            return ViewportApplyMode.SYSTEM_EMULATION;
        }
        return ViewportApplyMode.OFF;
    }

    Integer getTargetFontScalePercent(String packageName) {
        String key = keyForFontScale(packageName);
        if (!contains(key)) {
            return null;
        }
        return getInt(key, 0);
    }

    String getTargetFontApplyMode(String packageName) {
        String key = keyForFontMode(packageName);
        if (contains(key)) {
            return FontApplyMode.normalize(getString(key, FontApplyMode.OFF));
        }
        if (contains(keyForFontScale(packageName))) {
            // 历史配置迁移：已有字体百分比但无模式时，默认视为系统伪装。
            return FontApplyMode.SYSTEM_EMULATION;
        }
        return FontApplyMode.OFF;
    }

    boolean isSystemServerHooksEnabled() {
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
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(keyForViewportWidth(packageName), widthDp));
    }

    boolean clearTargetViewportWidthDp(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!contains(keyForFontScale(packageName))
                && !contains(keyForFontMode(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForViewportWidth(packageName))
                .remove(keyForViewportMode(packageName)));
    }

    boolean setTargetViewportApplyMode(String packageName, String mode) {
        String normalized = ViewportApplyMode.normalize(mode);
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!ViewportApplyMode.isEnabled(normalized)) {
            if (!contains(keyForViewportWidth(packageName))
                    && !contains(keyForFontScale(packageName))
                    && !contains(keyForFontMode(packageName))) {
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
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(keyForFontScale(packageName), percent));
    }

    boolean setTargetFontApplyMode(String packageName, String mode) {
        String normalized = FontApplyMode.normalize(mode);
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (FontApplyMode.OFF.equals(normalized)) {
            if (!contains(keyForViewportWidth(packageName))
                    && !contains(keyForFontScale(packageName))) {
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
        if (!contains(keyForViewportWidth(packageName))
                && !contains(keyForFontMode(packageName))) {
            packages.remove(packageName);
        }
        return commitBoth(editor -> editor
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForFontScale(packageName)));
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

    boolean hasPrimaryTargetFontApplyMode(String packageName) {
        return containsInPrimary(keyForFontMode(packageName));
    }

    boolean isTargetDpisEnabled(String packageName) {
        return getBoolean(keyForDpisEnabled(packageName), true);
    }

    boolean setTargetDpisEnabled(String packageName, boolean enabled) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (enabled) {
            if (!contains(keyForViewportWidth(packageName))
                    && !contains(keyForFontScale(packageName))
                    && !contains(keyForViewportMode(packageName))
                    && !contains(keyForFontMode(packageName))) {
                packages.remove(packageName);
                return commitBoth(editor -> editor
                        .putStringSet(KEY_TARGET_PACKAGES, packages)
                        .remove(keyForDpisEnabled(packageName)));
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
            copyAllEntries(snapshot, mirrorPreferences.getAll());
        }
        copyAllEntries(snapshot, preferences.getAll());
        return snapshot;
    }

    boolean replaceAll(Map<String, Object> entries) {
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

    private boolean contains(String key) {
        return preferences.contains(key)
                || (mirrorPreferences != null && mirrorPreferences.contains(key));
    }

    private boolean containsInPrimary(String key) {
        return preferences.contains(key);
    }

    private int getInt(String key, int defaultValue) {
        if (preferences.contains(key)) {
            return preferences.getInt(key, defaultValue);
        }
        if (mirrorPreferences != null && mirrorPreferences.contains(key)) {
            return mirrorPreferences.getInt(key, defaultValue);
        }
        return defaultValue;
    }

    private String getString(String key, String defaultValue) {
        if (preferences.contains(key)) {
            return preferences.getString(key, defaultValue);
        }
        if (mirrorPreferences != null && mirrorPreferences.contains(key)) {
            return mirrorPreferences.getString(key, defaultValue);
        }
        return defaultValue;
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        if (preferences.contains(key)) {
            return preferences.getBoolean(key, defaultValue);
        }
        if (mirrorPreferences != null && mirrorPreferences.contains(key)) {
            return mirrorPreferences.getBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    private boolean commitBoth(EditorAction action) {
        SharedPreferences.Editor primaryEditor = preferences.edit();
        action.apply(primaryEditor);
        boolean primaryCommitted = primaryEditor.commit();
        if (mirrorPreferences == null) {
            return primaryCommitted;
        }
        SharedPreferences.Editor mirrorEditor = mirrorPreferences.edit();
        action.apply(mirrorEditor);
        return primaryCommitted && mirrorEditor.commit();
    }

    private interface EditorAction {
        void apply(SharedPreferences.Editor editor);
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

    private static void copyAllEntries(Map<String, Object> target, Map<String, ?> source) {
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            Object normalized = normalizeValue(entry.getValue());
            if (normalized != null) {
                target.put(key, normalized);
            }
        }
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

    private static String keyForViewportWidth(String packageName) {
        return "viewport." + packageName + ".width_dp";
    }

    private static String keyForViewportMode(String packageName) {
        return "viewport." + packageName + ".mode";
    }

    private static String keyForFontScale(String packageName) {
        return "font." + packageName + ".scale_percent";
    }

    private static String keyForFontMode(String packageName) {
        return "font." + packageName + ".mode";
    }

    private static String keyForDpisEnabled(String packageName) {
        return "target." + packageName + ".dpis_enabled";
    }
}
