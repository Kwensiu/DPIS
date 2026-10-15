package com.dpis.module.viewport

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewportOverrideTest {
    @Test
    fun fullDisplayWindowKeepsTheRelativeWidthWhenDensityIsStillPhysical() {
        val config = Configuration()
        config.screenWidthDp = 540
        config.screenHeightDp = 1188
        config.smallestScreenWidthDp = 540
        config.densityDpi = 456

        val result = ViewportOverride.deriveWindowScoped(config, 540, null, 1080, 2376)

        assertEquals(540, result!!.widthDp)
        assertEquals(1188, result.heightDp)
        assertEquals(320, result.densityDpi)
    }

    @Test
    fun sourceWidthWindowStillScalesTowardTheTarget() {
        val config = Configuration()
        config.screenWidthDp = 379
        config.screenHeightDp = 834
        config.smallestScreenWidthDp = 379
        config.densityDpi = 456

        val result = ViewportOverride.deriveWindowScoped(config, 540, null, 1080, 2376)

        assertEquals(540, result!!.widthDp)
        assertEquals(1188, result.heightDp)
        assertEquals(320, result.densityDpi)
    }

    @Test
    fun shorterWindowKeepsTheDensityItAlreadyHas() {
        val config = Configuration()
        config.screenWidthDp = 540
        config.screenHeightDp = 1188
        config.smallestScreenWidthDp = 540
        config.densityDpi = 320

        val result = ViewportOverride.deriveWindowScoped(config, 540, null, 1080, 1600)

        assertEquals(540, result!!.widthDp)
        assertEquals(800, result.heightDp)
        assertEquals(320, result.densityDpi)
    }
}
