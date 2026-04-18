package com.dpis.module;

import android.content.res.Configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActivityThreadFontHookInstallerTest {

    @Test
    public void applyFontScaleToBindData_usesPerAppFontPercent() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setTargetFontScalePercent("com.max.xiaoheihe", 150);

        FakeBindData bindData = new FakeBindData();
        bindData.config.fontScale = 1.0f;

        boolean changed = ActivityThreadFontHookInstaller.applyFontScaleToBindData(
                bindData, "com.max.xiaoheihe", store);

        assertTrue(changed);
        assertEquals(1.5f, bindData.config.fontScale, 0.0001f);
    }

    @Test
    public void applyFontScaleToBindData_returnsFalseWhenNoFontConfig() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        FakeBindData bindData = new FakeBindData();
        bindData.config.fontScale = 1.0f;

        boolean changed = ActivityThreadFontHookInstaller.applyFontScaleToBindData(
                bindData, "com.max.xiaoheihe", store);

        assertFalse(changed);
        assertEquals(1.0f, bindData.config.fontScale, 0.0001f);
    }

    private static final class FakeBindData {
        @SuppressWarnings("unused")
        Configuration config = new Configuration();
    }
}
