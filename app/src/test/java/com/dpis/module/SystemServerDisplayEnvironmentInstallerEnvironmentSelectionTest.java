package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}
