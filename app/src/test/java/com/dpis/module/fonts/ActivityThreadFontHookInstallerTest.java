package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;

import com.dpis.module.config.DpisConfigStore;
import com.dpis.module.diagnostics.DpisLog;
import com.dpis.module.fonts.FontDebugStatsReporter;
import com.dpis.module.runtime.font.ActivityThreadFontHookInstaller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ActivityThreadFontHookInstallerTest {
    private boolean savedLoggingEnabled;

    @Before
    public void setUp() {
        savedLoggingEnabled = DpisLog.isLoggingEnabled();
        DpisLog.setLoggingEnabled(true);
        FontDebugStatsReporter.resetForTest();
    }

    @After
    public void tearDown() {
        DpisLog.setLoggingEnabled(savedLoggingEnabled);
        FontDebugStatsReporter.resetForTest();
    }

    @Test
    public void applyFontScaleToBindData_usesPerAppFontPercent() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetFontScalePercent("com.max.xiaoheihe", 150);

        FakeBindData bindData = new FakeBindData();
        bindData.config.fontScale = 1.0f;

        boolean changed = ActivityThreadFontHookInstaller.applyFontScaleToBindData(
                bindData, "com.max.xiaoheihe", store);

        assertTrue(changed);
        assertEquals(1.5f, bindData.config.fontScale, 0.0001f);
        assertEquals("Applying bind configuration must not report before Application exists",
                0, FontDebugStatsReporter.debugTotalEvents());
    }

    @Test
    public void applyFontScaleToBindData_returnsFalseWhenNoFontConfig() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        FakeBindData bindData = new FakeBindData();
        bindData.config.fontScale = 1.0f;

        boolean changed = ActivityThreadFontHookInstaller.applyFontScaleToBindData(
                bindData, "com.max.xiaoheihe", store);

        assertFalse(changed);
        assertEquals(1.0f, bindData.config.fontScale, 0.0001f);
    }

    @Test
    public void bindOverrideIsReportedOnlyAfterApplicationBindCompletes() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/dpis/module/runtime/font/ActivityThreadFontHookInstaller.java"));
        int proceed = source.indexOf("Object result = chain.proceed();");
        int report = source.indexOf("FontDebugStatsReporter.record(", proceed);

        assertTrue("the original bind must finish before reporting", proceed >= 0 && report > proceed);
    }

    private static final class FakeBindData {
        @SuppressWarnings("unused")
        Configuration config = new Configuration();
    }
}
