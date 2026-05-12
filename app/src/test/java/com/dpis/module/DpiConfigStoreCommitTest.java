package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class DpiConfigStoreCommitTest {
    @Test
    public void commitBothIgnoresReadOnlyMirror() {
        FakePrefs primaryPrefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(primaryPrefs, new ReadOnlyPrefs());

        assertTrue(store.setHyperOsFlutterFontHookEnabled(true));
        assertTrue(store.isHyperOsFlutterFontHookEnabled());
        assertFalse(primaryPrefs.getAll().isEmpty());
    }

    private static final class ReadOnlyPrefs implements SharedPreferences {
        @Override
        public Map<String, ?> getAll() {
            return Collections.emptyMap();
        }

        @Override
        public String getString(String key, String defValue) {
            return defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            return defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            return defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            return defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            return defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            return defValue;
        }

        @Override
        public boolean contains(String key) {
            return false;
        }

        @Override
        public Editor edit() {
            throw new UnsupportedOperationException("read only");
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        }
    }
}
