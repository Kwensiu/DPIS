package com.dpis.module.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Captures the cold-start path users take after installing DPIS, including the
 * first Apps workspace composition and the first list scroll. The app keeps a
 * complete list in memory; this only makes that existing path cheaper to load.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DpisBaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun coldStartOpenAppsAndScroll() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true
    ) {
        startActivityAndWait()

        // A post-install restart notice can cover the first tap. Mirroring the
        // real-device reproduction keeps the Apps destination in the profile.
        device.click(APPS_NAV_X, APPS_NAV_Y)
        device.waitForIdle()
        device.click(APPS_NAV_X, APPS_NAV_Y)
        device.waitForIdle()

        // Do not depend on translated labels. The profile should capture the
        // first list composition on every supported locale.
        device.swipe(SWIPE_X, SWIPE_START_Y, SWIPE_X, SWIPE_END_Y, 18)
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE_NAME = "io.github.kwensiu.dpis"
        const val APPS_NAV_X = 95
        const val APPS_NAV_Y = 2215
        const val SWIPE_X = 520
        const val SWIPE_START_Y = 1800
        const val SWIPE_END_Y = 700
    }
}
