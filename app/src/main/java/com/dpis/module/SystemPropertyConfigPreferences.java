package com.dpis.module;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class SystemPropertyConfigPreferences implements SharedPreferences {
    private final String packageName;
    private volatile Map<String, Object> cachedSnapshot;

    SystemPropertyConfigPreferences(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public Map<String, ?> getAll() {
        Map<String, Object> snapshot = cachedSnapshot;
        if (snapshot != null) {
            return snapshot;
        }
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        Integer widthDp = ViewportPropertyBridge.readTargetWidthDp(packageName);
        Integer fontScalePercent = HyperOsFlutterFontBridge.readCompatFontScalePercent(packageName);
        if (widthDp != null && widthDp > 0) {
            values.put(viewportWidthKey(), widthDp);
            values.put(viewportModeKey(), ViewportApplyMode.SYSTEM_EMULATION);
        }
        if (fontScalePercent != null && fontScalePercent > 0) {
            values.put(fontScaleKey(), fontScalePercent);
            values.put(fontModeKey(), FontApplyMode.SYSTEM_EMULATION);
        }
        if (!values.isEmpty()) {
            values.put(DpiConfigStore.KEY_TARGET_PACKAGES,
                    new LinkedHashSet<>(Collections.singleton(packageName)));
        }
        cachedSnapshot = Collections.unmodifiableMap(values);
        return cachedSnapshot;
    }

    @Override
    public String getString(String key, String defValue) {
        Object value = getAll().get(key);
        return value instanceof String typed ? typed : defValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Object value = getAll().get(key);
        return value instanceof Set<?> ? new LinkedHashSet<>((Set<String>) value) : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object value = getAll().get(key);
        return value instanceof Integer typed ? typed : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object value = getAll().get(key);
        return value instanceof Long typed ? typed : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object value = getAll().get(key);
        return value instanceof Float typed ? typed : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = getAll().get(key);
        return value instanceof Boolean typed ? typed : defValue;
    }

    @Override
    public boolean contains(String key) {
        return getAll().containsKey(key);
    }

    @Override
    public Editor edit() {
        throw new UnsupportedOperationException("SystemPropertyConfigPreferences is read-only");
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }

    private String viewportWidthKey() {
        return "viewport." + packageName + ".width_dp";
    }

    private String viewportModeKey() {
        return "viewport." + packageName + ".mode";
    }

    private String fontScaleKey() {
        return "font." + packageName + ".scale_percent";
    }

    private String fontModeKey() {
        return "font." + packageName + ".mode";
    }
}
