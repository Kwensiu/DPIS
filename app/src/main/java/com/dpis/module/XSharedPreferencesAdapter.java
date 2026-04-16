package com.dpis.module;

import android.content.SharedPreferences;

import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XSharedPreferences;

final class XSharedPreferencesAdapter implements SharedPreferences {
    private final XSharedPreferences preferences;

    XSharedPreferencesAdapter(String packageName, String preferenceName) {
        preferences = new XSharedPreferences(packageName, preferenceName);
        preferences.reload();
    }

    private void reload() {
        preferences.reload();
    }

    @Override
    public Map<String, ?> getAll() {
        reload();
        return preferences.getAll();
    }

    @Override
    public String getString(String key, String defValue) {
        reload();
        return preferences.getString(key, defValue);
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        reload();
        Set<String> values = preferences.getStringSet(key, defValues);
        return values != null ? values : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        reload();
        return preferences.getInt(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        reload();
        return preferences.getLong(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        reload();
        return preferences.getFloat(key, defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        reload();
        return preferences.getBoolean(key, defValue);
    }

    @Override
    public boolean contains(String key) {
        reload();
        return preferences.contains(key);
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
}
