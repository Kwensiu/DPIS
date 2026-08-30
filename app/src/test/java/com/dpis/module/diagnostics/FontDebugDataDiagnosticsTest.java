package com.dpis.module;

import com.dpis.module.fonts.FontDebugDataDiagnostics;
import com.dpis.module.fonts.FontDebugStatsStore;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class FontDebugDataDiagnosticsTest {
    @Test
    public void returnsScopeMissingWhenNoConfiguredTargets() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        FontDebugDataDiagnostics.NoDataReason reason =
                FontDebugDataDiagnostics.resolveNoDataReason(store, prefs);

        assertEquals(FontDebugDataDiagnostics.NoDataReason.SCOPE_MISSING, reason);
    }

    @Test
    public void returnsNotInjectedWhenTargetsConfiguredButNoSignals() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        prefs.edit().putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                Collections.singleton("com.example.app")).commit();

        FontDebugDataDiagnostics.NoDataReason reason =
                FontDebugDataDiagnostics.resolveNoDataReason(store, prefs);

        assertEquals(FontDebugDataDiagnostics.NoDataReason.NOT_INJECTED, reason);
    }

    @Test
    public void returnsNoEventsWhenViewportSignalExistsWithoutFontEvents() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, Collections.singleton("com.example.app"))
                .putString(FontDebugStatsStore.KEY_VIEWPORT_DEBUG_SUMMARY, "视口: 360dp -> 320dp")
                .commit();

        FontDebugDataDiagnostics.NoDataReason reason =
                FontDebugDataDiagnostics.resolveNoDataReason(store, prefs);

        assertEquals(FontDebugDataDiagnostics.NoDataReason.NO_EVENTS, reason);
    }

    @Test
    public void returnsNoneWhenFontEventsExist() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, Collections.singleton("com.example.app"))
                .putInt(FontDebugStatsStore.KEY_EVENT_TOTAL, 3)
                .commit();

        FontDebugDataDiagnostics.NoDataReason reason =
                FontDebugDataDiagnostics.resolveNoDataReason(store, prefs);

        assertEquals(FontDebugDataDiagnostics.NoDataReason.NONE, reason);
    }
}
