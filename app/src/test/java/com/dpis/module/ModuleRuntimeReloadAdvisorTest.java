package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ModuleRuntimeReloadAdvisorTest {
    @Test
    public void detectsSystemServerRuntimeOlderThanCurrentInstall() {
        assertTrue(ModuleRuntimeReloadAdvisor.isSystemServerRuntimeOlderThanInstall(
                10_000L, 7_000L));
    }

    @Test
    public void ignoresMissingOrCurrentSystemServerRuntime() {
        assertFalse(ModuleRuntimeReloadAdvisor.isSystemServerRuntimeOlderThanInstall(0L, 7_000L));
        assertFalse(ModuleRuntimeReloadAdvisor.isSystemServerRuntimeOlderThanInstall(10_000L, 0L));
        assertFalse(ModuleRuntimeReloadAdvisor.isSystemServerRuntimeOlderThanInstall(10_000L, 9_000L));
        assertFalse(ModuleRuntimeReloadAdvisor.isSystemServerRuntimeOlderThanInstall(10_000L, 10_000L));
        assertFalse(ModuleRuntimeReloadAdvisor.isSystemServerRuntimeOlderThanInstall(10_000L, 12_000L));
    }
}
