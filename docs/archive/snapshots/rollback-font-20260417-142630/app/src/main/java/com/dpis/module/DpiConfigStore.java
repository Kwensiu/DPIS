package com.dpis.module;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class DpiConfigStore {
    static final String GROUP = "dpi_config";
    static final String KEY_TARGET_PACKAGES = "target_packages";
    static final String KEY_SYSTEM_SERVER_HOOKS_ENABLED = "system_server.hooks_enabled";
    static final String KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED = "system_server.safe_mode_enabled";
    static final String KEY_GLOBAL_LOG_ENABLED = "global.log_enabled";

    private final SharedPreferences preferences;

    DpiConfigStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    Set<String> getConfiguredPackages() {
        Set<String> packages = preferences.getStringSet(KEY_TARGET_PACKAGES, Collections.emptySet());
        return new LinkedHashSet<>(packages);
    }

    Integer getTargetViewportWidthDp(String packageName) {
        String key = keyForViewportWidth(packageName);
        if (!preferences.contains(key)) {
            return null;
        }
        return preferences.getInt(key, 0);
    }

    Integer getTargetFontScalePercent(String packageName) {
        String key = keyForFontScale(packageName);
        if (!preferences.contains(key)) {
            return null;
        }
        return preferences.getInt(key, 0);
    }

    boolean isSystemServerHooksEnabled() {
        return preferences.getBoolean(KEY_SYSTEM_SERVER_HOOKS_ENABLED, true);
    }

    boolean hasSystemServerHooksEnabled() {
        return preferences.contains(KEY_SYSTEM_SERVER_HOOKS_ENABLED);
    }

    boolean isSystemServerSafeModeEnabled() {
        return preferences.getBoolean(KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED, true);
    }

    boolean hasSystemServerSafeModeEnabled() {
        return preferences.contains(KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED);
    }

    boolean setSystemServerHooksEnabled(boolean enabled) {
        return preferences.edit().putBoolean(KEY_SYSTEM_SERVER_HOOKS_ENABLED, enabled).commit();
    }

    boolean setSystemServerSafeModeEnabled(boolean enabled) {
        return preferences.edit().putBoolean(KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED, enabled).commit();
    }

    boolean isGlobalLogEnabled() {
        return preferences.getBoolean(KEY_GLOBAL_LOG_ENABLED, false);
    }

    boolean hasGlobalLogEnabled() {
        return preferences.contains(KEY_GLOBAL_LOG_ENABLED);
    }

    boolean setGlobalLogEnabled(boolean enabled) {
        return preferences.edit().putBoolean(KEY_GLOBAL_LOG_ENABLED, enabled).commit();
    }

    boolean setTargetViewportWidthDp(String packageName, int widthDp) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return preferences.edit()
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(keyForViewportWidth(packageName), widthDp)
                .commit();
    }

    boolean clearTargetViewportWidthDp(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!preferences.contains(keyForFontScale(packageName))) {
            packages.remove(packageName);
        }
        return preferences.edit()
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForViewportWidth(packageName))
                .commit();
    }

    boolean setTargetFontScalePercent(String packageName, int percent) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        return preferences.edit()
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(keyForFontScale(packageName), percent)
                .commit();
    }

    boolean clearTargetFontScalePercent(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        if (!preferences.contains(keyForViewportWidth(packageName))) {
            packages.remove(packageName);
        }
        return preferences.edit()
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForFontScale(packageName))
                .commit();
    }

    boolean ensureSeedConfig(Map<String, Integer> seedTargetViewportWidthDps) {
        LinkedHashSet<String> mergedPackages = new LinkedHashSet<>(getConfiguredPackages());
        mergedPackages.addAll(seedTargetViewportWidthDps.keySet());

        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(KEY_TARGET_PACKAGES, mergedPackages);
        for (Map.Entry<String, Integer> entry : seedTargetViewportWidthDps.entrySet()) {
            String key = keyForViewportWidth(entry.getKey());
            if (!preferences.contains(key)) {
                editor.putInt(key, entry.getValue());
            }
        }
        return editor.commit();
    }

    private static String keyForViewportWidth(String packageName) {
        return "viewport." + packageName + ".width_dp";
    }

    private static String keyForFontScale(String packageName) {
        return "font." + packageName + ".scale_percent";
    }
}
