package com.dpis.module

import com.dpis.module.runtime.systemserver.LegacySystemServerGate
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacySystemServerGateTest {
    @Test
    fun acceptsSystemMainProcess() =
        assertEquals(true, LegacySystemServerGate.shouldInstall("android", "system"))

    @Test
    fun acceptsAndroidMainProcess() =
        assertEquals(true, LegacySystemServerGate.shouldInstall("android", "android"))

    @Test
    fun rejectsSystemUiSideProcess() =
        assertEquals(false, LegacySystemServerGate.shouldInstall("android", "system:ui"))

    @Test
    fun rejectsAndroidUiSideProcess() =
        assertEquals(false, LegacySystemServerGate.shouldInstall("android", "android:ui"))

    @Test
    fun rejectsRegularAppProcess() = assertEquals(
        false,
        LegacySystemServerGate.shouldInstall("com.max.xiaoheihe", "com.max.xiaoheihe"),
    )
}
