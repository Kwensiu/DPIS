package com.dpis.module.fonts;

import static org.junit.Assert.assertFalse;

import android.content.Context;

import com.dpis.module.FakePrefs;

import org.junit.Test;

public class FontDebugStatsUpdateWriterTest {
    @Test
    public void applyExtrasIgnoresNullInput() {
        FakePrefs preferences = new FakePrefs();
        FontDebugStatsUpdateWriter.applyExtras(preferences, null);
        FontDebugStatsUpdateWriter.applyExtras((Context) null, null);
        assertFalse(preferences.contains(FontDebugStatsStore.KEY_CHAIN_5S));
    }
}
