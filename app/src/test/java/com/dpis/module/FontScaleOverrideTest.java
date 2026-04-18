package com.dpis.module;

import android.util.TypedValue;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FontScaleOverrideTest {
    @Test
    public void resolveFallsBackToOriginalWhenTargetMissing() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        FontScaleOverride.Result result = FontScaleOverride.resolve(
                store, "com.max.xiaoheihe", 1.25f);

        assertEquals(1.25f, result.original, 0.0001f);
        assertEquals(1.25f, result.effective, 0.0001f);
        assertFalse(result.changed);
    }

    @Test
    public void resolveAppliesTargetPercentWhenConfigured() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("font.com.max.xiaoheihe.scale_percent", 150).commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        FontScaleOverride.Result result = FontScaleOverride.resolve(
                store, "com.max.xiaoheihe", 1.0f);

        assertEquals(Integer.valueOf(150), result.targetPercent);
        assertEquals(1.5f, result.effective, 0.0001f);
        assertTrue(result.changed);
    }

    @Test
    public void identifiesUnitsThatNeedForceScaling() {
        assertTrue(FontScaleOverride.shouldForceTextUnit(TypedValue.COMPLEX_UNIT_PX));
        assertTrue(FontScaleOverride.shouldForceTextUnit(TypedValue.COMPLEX_UNIT_DIP));
        assertFalse(FontScaleOverride.shouldForceTextUnit(TypedValue.COMPLEX_UNIT_SP));
    }

}
