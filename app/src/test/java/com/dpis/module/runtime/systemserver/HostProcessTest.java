package com.dpis.module.runtime.systemserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HostProcessTest {
    @Test
    public void matchesAndroidProcess() {
        assertTrue(HostProcess.isSystemServer("android", "com.any.app"));
    }

    @Test
    public void matchesSystemProcess() {
        assertTrue(HostProcess.isSystemServer("system", "com.any.app"));
    }

    @Test
    public void matchesAndroidPackage() {
        assertTrue(HostProcess.isSystemServer("system", "android"));
    }

    @Test
    public void rejectsRegularAppProcess() {
        assertFalse(HostProcess.isSystemServer(
                "com.max.xiaoheihe", "com.max.xiaoheihe"));
    }
}
