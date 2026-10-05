package com.dpis.module.viewport

import android.content.res.Configuration
import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import org.junit.After
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

    @Test
    fun activeWindowKeepsItsOwnConfigurationInTheAppProcess() {
        val packageName = "com.example.target"
        val store = systemOwnedStore(packageName)
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record(packageName, 0, 0, 1080, 1920)

        assertFalse(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(360, 640),
            ),
        )
        assertTrue(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(360, 792),
            ),
        )
    }

    @After
    fun tearDown() {
        VirtualDisplayState.set(null)
        WindowBoundsState.clearForTest()
    }

    private fun systemOwnedStore(packageName: String): DpisConfigStore {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.AUTO)
        store.setSystemServerHooksEnabled(true)
        return store
    }

    private fun configuration(widthDp: Int, heightDp: Int): Configuration {
        val config = Configuration()
        config.screenWidthDp = widthDp
        config.screenHeightDp = heightDp
        config.smallestScreenWidthDp = minOf(widthDp, heightDp)
        config.densityDpi = 480
        config.fontScale = 1.0f
        return config
    }
}
