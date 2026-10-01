package com.dpis.module.viewport

import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelativeViewportOwnershipTest {
    @Test
    fun autoRelativeTargetDefersWhenSystemHooksAreEnabled() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.example.target", ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.AUTO)
        store.setSystemServerHooksEnabled(true)

        assertTrue(RelativeViewportOwnership.shouldDefer(store, "com.example.target"))
    }

    @Test
    fun compatRelativeTargetRemainsAppOwned() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.example.target", ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.COMPAT)
        store.setSystemServerHooksEnabled(true)

        assertFalse(RelativeViewportOwnership.shouldDefer(store, "com.example.target"))
    }

    @Test
    fun disabledSystemHooksKeepRelativeTargetAppOwned() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.example.target", ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.AUTO)
        store.setSystemServerHooksEnabled(false)

        assertFalse(RelativeViewportOwnership.shouldDefer(store, "com.example.target"))
    }
}
