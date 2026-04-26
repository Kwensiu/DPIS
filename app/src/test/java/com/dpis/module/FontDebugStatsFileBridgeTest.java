package com.dpis.module;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

public class FontDebugStatsFileBridgeTest {
    @Test
    public void importIfNewerIgnoresOlderFileData() {
        FakePrefs preferences = new FakePrefs();
        preferences.edit()
                .putLong(FontDebugStatsStore.KEY_UPDATED_AT, 200L)
                .putString(FontDebugStatsStore.KEY_CHAIN_5S, "new")
                .apply();
        Properties properties = new Properties();
        properties.setProperty(FontDebugStatsStore.EXTRA_UPDATED_AT, "100");
        properties.setProperty(FontDebugStatsStore.EXTRA_CHAIN_5S, "old");

        FontDebugStatsFileBridge.importIfNewer(preferences, properties);

        assertEquals(200L, preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L));
        assertEquals("new", preferences.getString(FontDebugStatsStore.KEY_CHAIN_5S, null));
    }

    @Test
    public void importIfNewerAppliesNewerFileData() {
        FakePrefs preferences = new FakePrefs();
        preferences.edit()
                .putLong(FontDebugStatsStore.KEY_UPDATED_AT, 100L)
                .putString(FontDebugStatsStore.KEY_CHAIN_5S, "old")
                .apply();
        Properties properties = new Properties();
        properties.setProperty(FontDebugStatsStore.EXTRA_UPDATED_AT, "200");
        properties.setProperty(FontDebugStatsStore.EXTRA_CHAIN_5S, "new");
        properties.setProperty(FontDebugStatsStore.EXTRA_EVENT_TOTAL, "3");

        FontDebugStatsFileBridge.importIfNewer(preferences, properties);

        assertEquals(200L, preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L));
        assertEquals("new", preferences.getString(FontDebugStatsStore.KEY_CHAIN_5S, null));
        assertEquals(3, preferences.getInt(FontDebugStatsStore.KEY_EVENT_TOTAL, 0));
    }

    @Test
    public void importIfNewerIgnoresMissingTimestamp() {
        FakePrefs preferences = new FakePrefs();
        preferences.edit()
                .putLong(FontDebugStatsStore.KEY_UPDATED_AT, 100L)
                .putString(FontDebugStatsStore.KEY_CHAIN_5S, "current")
                .apply();
        Properties properties = new Properties();
        properties.setProperty(FontDebugStatsStore.EXTRA_CHAIN_5S, "missing timestamp");

        FontDebugStatsFileBridge.importIfNewer(preferences, properties);

        assertEquals(100L, preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L));
        assertEquals("current", preferences.getString(FontDebugStatsStore.KEY_CHAIN_5S, null));
    }
}
