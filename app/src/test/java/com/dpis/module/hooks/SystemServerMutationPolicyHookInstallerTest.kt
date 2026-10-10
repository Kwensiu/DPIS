package com.dpis.module

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.systemserver.SystemServerMutationPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemServerMutationPolicyHookInstallerTest {
    @Test
    fun skipsSystemServerInstallerEntryWhenHooksDisabled() {
        val policy = createPolicy(true, false)

        assertFalse(
            SystemServerMutationPolicy.shouldInstallSystemServerHooks(
                "android",
                "android",
                policy
            )
        )
    }

    @Test
    fun keepsLowRiskSystemServerEntryWhenSafetyModeEnabled() {
        val policy = createPolicy(true, true)

        assertTrue(
            SystemServerMutationPolicy.shouldInstallSystemServerHooks(
                "android",
                "android",
                policy
            )
        )
    }

    @Test
    fun skipsRegularAppProcessesForSystemServerInstallerEntry() {
        val policy = createPolicy(false, true)

        assertFalse(
            SystemServerMutationPolicy.shouldInstallSystemServerHooks(
                "com.max.xiaoheihe",
                "com.max.xiaoheihe",
                policy,
            ),
        )
    }

    private fun createPolicy(safeMode: Boolean, systemHooksEnabled: Boolean): HookRuntimePolicy {
        val store = DpisConfigStore(FakePrefs())
        store.setSystemServerSafeModeEnabled(safeMode)
        store.setSystemServerHooksEnabled(systemHooksEnabled)
        return HookRuntimePolicy.fromStore(store)
    }
}
