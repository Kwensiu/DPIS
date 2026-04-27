package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemServerDisplayEnvironmentInstallerEnvironmentSelectionTest {
    @Test
    public void keepsPreEnvironmentWhenPostEnvironmentMissing() {
        String selected = SystemServerDisplayEnvironmentInstaller.selectEnvironmentSourceForTest(
                true,
                false
        );
        assertEquals("pre", selected);
    }

    @Test
    public void prefersPostEnvironmentWhenAvailable() {
        String selected = SystemServerDisplayEnvironmentInstaller.selectEnvironmentSourceForTest(
                true,
                true
        );
        assertEquals("post", selected);
    }

    @Test
    public void systemServerUsesViewportOrFontEmulationConfig() {
        PerAppDisplayConfig fontOnlyEmulation = new PerAppDisplayConfig(
                "com.example.target",
                null,
                120,
                FontApplyMode.SYSTEM_EMULATION
        );
        PerAppDisplayConfig fontOnlyRewrite = new PerAppDisplayConfig(
                "com.example.target",
                null,
                120,
                FontApplyMode.FIELD_REWRITE
        );
        PerAppDisplayConfig viewport = new PerAppDisplayConfig(
                "com.example.target",
                360
        );

        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(fontOnlyEmulation));
        assertFalse(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(fontOnlyRewrite));
        assertTrue(SystemServerDisplayEnvironmentInstaller
                .shouldUseConfigInSystemServerForTest(viewport));
    }
}
