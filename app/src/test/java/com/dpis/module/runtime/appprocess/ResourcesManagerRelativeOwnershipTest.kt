package com.dpis.module.runtime.appprocess

import android.content.res.Configuration
import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourcesManagerRelativeOwnershipTest {
    @Test
    fun systemOwnedRelativeViewportSkipsConfigurationMutation() {
        val packageName = "com.example.target"
        val config = Configuration().apply {
            screenWidthDp = 360
            screenHeightDp = 792
            smallestScreenWidthDp = 360
            densityDpi = 480
            fontScale = 1.0f
        }
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs).also {
            it.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        }
        prefs.edit()
            .putString("viewport.$packageName.mode", ViewportApplyMode.AUTO)
            .putBoolean("system_server.hooks_enabled", true)
            .commit()

        ResourcesManagerHookInstaller.applyResourceOverrides(
            config,
            store,
            packageName,
            "ResourcesManager",
        )

        assertEquals(360, config.screenWidthDp)
        assertEquals(792, config.screenHeightDp)
        assertEquals(360, config.smallestScreenWidthDp)
        assertEquals(480, config.densityDpi)
    }
}
