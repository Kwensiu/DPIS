package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DpiConfigStoreCommitTest {
    @Test
    public void commitFailureLeavesCurrentStoreUnchanged() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        prefs.setCommitResult(false);

        assertFalse(store.setHyperOsFlutterFontHookEnabled(true));

        prefs.setCommitResult(true);
        assertFalse(store.isHyperOsFlutterFontHookEnabled());
        assertTrue(prefs.getAll().isEmpty());
    }
}