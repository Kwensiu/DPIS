package com.dpis.module.config

import com.dpis.module.FakePrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DpisConfigStoreCompatibilityTest {
    @Test
    fun systemServerFacadeKeysStayAlignedWithConfigPreferenceKeys() {
        assertEquals(
            ConfigPreferenceKeys.SYSTEM_SERVER_HOOKS_ENABLED,
            DpisConfigStore.KEY_SYSTEM_SERVER_HOOKS_ENABLED,
        )
        assertEquals(
            ConfigPreferenceKeys.SYSTEM_SERVER_SAFE_MODE_ENABLED,
            DpisConfigStore.KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED,
        )
    }

    @Test
    fun fallbackConstructorReadsSystemServerFlagsFromSecondaryPreferences() {
        val primary = FakePrefs()
        val fallback = FakePrefs()
        fallback.edit()
            .putBoolean(DpisConfigStore.KEY_SYSTEM_SERVER_HOOKS_ENABLED, false)
            .putBoolean(DpisConfigStore.KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED, false)
            .commit()

        val store = DpisConfigStore(primary, fallback)

        assertFalse(store.isSystemServerHooksEnabled())
        assertFalse(store.isSystemServerSafeModeEnabled())
        assertFalse(store.hasSystemServerHooksEnabled())
        assertFalse(store.hasSystemServerSafeModeEnabled())
    }
}
