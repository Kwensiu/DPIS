package com.dpis.module.runtime;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;

public final class XSharedPreferencesAdapter implements SharedPreferences {
    private final XSharedPreferences preferences;
    private final long reloadIntervalMs;
    private volatile Map<String, Object> snapshot;
    private volatile long lastReloadAtMs;

    public XSharedPreferencesAdapter(String packageName, String preferenceName) {
        this(packageName, preferenceName, 0L);
    }

    public XSharedPreferencesAdapter(String packageName, String preferenceName, long reloadIntervalMs) {
        preferences = new XSharedPreferences(packageName, preferenceName);
        this.reloadIntervalMs = Math.max(0L, reloadIntervalMs);
        // XSharedPreferences.reload() can touch disk. Legacy resource hooks call into
        // DpisConfigStore from Resources hot paths, so app-process fallback stays as a
        // process-start snapshot. Long-lived system_server uses an explicit low-frequency
        // refresh interval instead of per-read reloads.
        reloadNow();
    }

    @Override
    public Map<String, ?> getAll() {
        maybeReload();
        return snapshot;
    }

    @Override
    public String getString(String key, String defValue) {
        maybeReload();
        Object value = snapshot.get(key);
        return value instanceof String typed ? typed : defValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, Set<String> defValues) {
        maybeReload();
        Object value = snapshot.get(key);
        return value instanceof Set<?> ? new LinkedHashSet<>((Set<String>) value) : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        maybeReload();
        Object value = snapshot.get(key);
        return value instanceof Integer typed ? typed : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        maybeReload();
        Object value = snapshot.get(key);
        return value instanceof Long typed ? typed : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        maybeReload();
        Object value = snapshot.get(key);
        return value instanceof Float typed ? typed : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        maybeReload();
        Object value = snapshot.get(key);
        return value instanceof Boolean typed ? typed : defValue;
    }

    @Override
    public boolean contains(String key) {
        maybeReload();
        return snapshot.containsKey(key);
    }

    @Override
    public Editor edit() {
        throw new UnsupportedOperationException("XSharedPreferencesAdapter is read-only");
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    }

    private void maybeReload() {
        if (reloadIntervalMs <= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastReloadAtMs < reloadIntervalMs) {
            return;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (now - lastReloadAtMs >= reloadIntervalMs) {
                reloadNow();
            }
        }
    }

    private void reloadNow() {
        preferences.reload();
        snapshot = snapshot(preferences.getAll());
        lastReloadAtMs = System.currentTimeMillis();
    }

    private static Map<String, Object> snapshot(Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Set<?> typed) {
                LinkedHashSet<String> stringSet = new LinkedHashSet<>();
                boolean valid = true;
                for (Object item : typed) {
                    if (!(item instanceof String text)) {
                        valid = false;
                        break;
                    }
                    stringSet.add(text);
                }
                if (valid) {
                    values.put(key, Collections.unmodifiableSet(stringSet));
                }
                continue;
            }
            if (value instanceof String
                    || value instanceof Integer
                    || value instanceof Long
                    || value instanceof Float
                    || value instanceof Boolean) {
                values.put(key, value);
            }
        }
        return Collections.unmodifiableMap(values);
    }
}
