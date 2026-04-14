package com.dpis.module;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class DpiConfigStore {
    static final String GROUP = "dpi_config";
    static final String KEY_TARGET_PACKAGES = "target_packages";

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

    Integer getEffectiveViewportWidthDp(String packageName) {
        return getTargetViewportWidthDp(packageName);
    }

    void setTargetViewportWidthDp(String packageName, int widthDp) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.add(packageName);
        preferences.edit()
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .putInt(keyForViewportWidth(packageName), widthDp)
                .commit();
    }

    void clearTargetViewportWidthDp(String packageName) {
        LinkedHashSet<String> packages = new LinkedHashSet<>(getConfiguredPackages());
        packages.remove(packageName);
        preferences.edit()
                .putStringSet(KEY_TARGET_PACKAGES, packages)
                .remove(keyForViewportWidth(packageName))
                .commit();
    }

    void ensureSeedConfig(Map<String, Integer> seedTargetDensityDpis) {
        LinkedHashSet<String> mergedPackages = new LinkedHashSet<>(getConfiguredPackages());
        mergedPackages.addAll(seedTargetDensityDpis.keySet());

        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(KEY_TARGET_PACKAGES, mergedPackages);
        for (Map.Entry<String, Integer> entry : seedTargetDensityDpis.entrySet()) {
            String key = keyForViewportWidth(entry.getKey());
            if (!preferences.contains(key)) {
                editor.putInt(key, entry.getValue());
            }
        }
        editor.commit();
    }

    private static String keyForViewportWidth(String packageName) {
        return "viewport." + packageName + ".width_dp";
    }
}
