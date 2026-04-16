package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LegacySystemServerGateTest {
    @Test
    public void acceptsSystemMainProcess() {
        assertEquals(true, LegacySystemServerGate.shouldInstall("android", "system"));
    }

    @Test
    public void acceptsAndroidMainProcess() {
        assertEquals(true, LegacySystemServerGate.shouldInstall("android", "android"));
    }

    @Test
    public void rejectsSystemUiSideProcess() {
        assertEquals(false, LegacySystemServerGate.shouldInstall("android", "system:ui"));
    }

    @Test
    public void rejectsAndroidUiSideProcess() {
        assertEquals(false, LegacySystemServerGate.shouldInstall("android", "android:ui"));
    }

    @Test
    public void rejectsRegularAppProcess() {
        assertEquals(false, LegacySystemServerGate.shouldInstall(
                "com.max.xiaoheihe", "com.max.xiaoheihe"));
    }
}
