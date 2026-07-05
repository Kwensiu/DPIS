package com.dpis.module;

import com.dpis.module.fonts.FontDebugStatsSchema;
import com.dpis.module.fonts.FontDebugStatsStore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Properties;

public class FontDebugStatsSchemaTest {
    @Test
    public void statsKeyForResolvesModeAndWindow() {
        assertEquals(FontDebugStatsStore.KEY_CHAIN_5S,
                FontDebugStatsSchema.statsKeyFor(FontDebugStatsStore.MODE_CHAIN,
                        FontDebugStatsStore.WINDOW_5S));
        assertEquals(FontDebugStatsStore.KEY_CHAIN_30S,
                FontDebugStatsSchema.statsKeyFor(FontDebugStatsStore.MODE_CHAIN,
                        FontDebugStatsStore.WINDOW_30S));
        assertEquals(FontDebugStatsStore.KEY_CHAIN_ALL,
                FontDebugStatsSchema.statsKeyFor(FontDebugStatsStore.MODE_CHAIN,
                        FontDebugStatsStore.WINDOW_ALL));
        assertEquals(FontDebugStatsStore.KEY_CHAIN_VIEW_5S,
                FontDebugStatsSchema.statsKeyFor(FontDebugStatsStore.MODE_CHAIN_VIEW,
                        FontDebugStatsStore.WINDOW_5S));
        assertEquals(FontDebugStatsStore.KEY_CHAIN_VIEW_30S,
                FontDebugStatsSchema.statsKeyFor(FontDebugStatsStore.MODE_CHAIN_VIEW,
                        FontDebugStatsStore.WINDOW_30S));
        assertEquals(FontDebugStatsStore.KEY_CHAIN_VIEW_ALL,
                FontDebugStatsSchema.statsKeyFor(FontDebugStatsStore.MODE_CHAIN_VIEW,
                        FontDebugStatsStore.WINDOW_ALL));
    }

    @Test
    public void copiesPropertiesToPreferenceSchema() {
        Properties properties = new Properties();
        properties.setProperty(FontDebugStatsStore.EXTRA_CHAIN_VIEW_30S, "view");
        properties.setProperty(FontDebugStatsStore.EXTRA_EVENT_TOTAL, "5");
        properties.setProperty(FontDebugStatsStore.EXTRA_UPDATED_AT, "123");

        FakePrefs preferences = new FakePrefs();
        android.content.SharedPreferences.Editor editor = preferences.edit();
        FontDebugStatsSchema.copyPropertiesToPreferences(properties, editor);
        editor.apply();

        assertEquals("view", preferences.getString(FontDebugStatsStore.KEY_CHAIN_VIEW_30S, null));
        assertEquals(5, preferences.getInt(FontDebugStatsStore.KEY_EVENT_TOTAL, 0));
        assertEquals(123L, preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L));
    }

    @Test
    public void removesAllPersistedStatsFields() {
        FakePrefs preferences = new FakePrefs();
        preferences.edit()
                .putString(FontDebugStatsStore.KEY_CHAIN_5S, "chain")
                .putString(FontDebugStatsStore.KEY_UNIT_BREAKDOWN_5S, "unit")
                .putString(FontDebugStatsStore.KEY_VIEWPORT_DEBUG_SUMMARY, "viewport")
                .putInt(FontDebugStatsStore.KEY_EVENT_TOTAL, 1)
                .putLong(FontDebugStatsStore.KEY_UPDATED_AT, 2L)
                .commit();

        android.content.SharedPreferences.Editor editor = preferences.edit();
        FontDebugStatsSchema.removeStats(editor);
        editor.apply();

        assertFalse(preferences.contains(FontDebugStatsStore.KEY_CHAIN_5S));
        assertFalse(preferences.contains(FontDebugStatsStore.KEY_UNIT_BREAKDOWN_5S));
        assertFalse(preferences.contains(FontDebugStatsStore.KEY_VIEWPORT_DEBUG_SUMMARY));
        assertFalse(preferences.contains(FontDebugStatsStore.KEY_EVENT_TOTAL));
        assertFalse(preferences.contains(FontDebugStatsStore.KEY_UPDATED_AT));
    }

    @Test
    public void signalChecksUseSharedSentinels() {
        assertFalse(FontDebugStatsSchema.isNonEmptyStatsText(""));
        assertFalse(FontDebugStatsSchema.isNonEmptyStatsText(" 暂无数据 "));
        assertTrue(FontDebugStatsSchema.isNonEmptyStatsText("1 text-size-unit-0"));

        assertFalse(FontDebugStatsSchema.isViewportSignal(""));
        assertFalse(FontDebugStatsSchema.isViewportSignal(" 视口: 暂无 "));
        assertTrue(FontDebugStatsSchema.isViewportSignal("视口 com.example | system"));
    }
}
