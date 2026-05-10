package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StartupUpdateCheckOnceTest {
    @Test
    public void consumeAllowsOnlyOneCheckPerProcess() {
        StartupUpdateCheckOnce.resetForTest();

        assertTrue(StartupUpdateCheckOnce.consume());
        assertFalse(StartupUpdateCheckOnce.consume());
    }
}
