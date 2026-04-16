package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SystemServerProcessTest {
    @Test
    public void matchesAndroidProcess() {
        assertEquals(true, SystemServerProcess.isSystemServer("android", "com.any.app"));
    }

    @Test
    public void matchesSystemProcess() {
        assertEquals(true, SystemServerProcess.isSystemServer("system", "com.any.app"));
    }

    @Test
    public void matchesAndroidPackage() {
        assertEquals(true, SystemServerProcess.isSystemServer("system", "android"));
    }

    @Test
    public void rejectsRegularAppProcess() {
        assertEquals(false, SystemServerProcess.isSystemServer(
                "com.max.xiaoheihe", "com.max.xiaoheihe"));
    }
}
