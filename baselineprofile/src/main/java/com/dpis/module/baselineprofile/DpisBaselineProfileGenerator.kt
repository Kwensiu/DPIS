package com.dpis.module.baselineprofile

import android.util.Log
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.dpis.module.baselineprofile.DpisProfileJourneys.startManagerForBaselineProfile
import com.dpis.module.baselineprofile.DpisProfileJourneys.targetPackageName
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline Profile capture for the DPIS manager APK.
 *
 * `BaselineProfileRule.collect` is the official capture API. The startup journey
 * launches with `am start` without `-W` and does not depend on UiAutomator's
 * accessibility view of the Compose UI.
 *
 * Run from Android Studio: Run > Edit Configurations > Generate Baseline Profile
 * for app (modernRelease / legacyRelease), targeting a dedicated API 33+ AOSP or
 * Google APIs emulator. Gradle:
 * `./gradlew :app:generateModernReleaseBaselineProfile`
 *
 * @see <a href="https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile">Create Baseline Profiles</a>
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DpisBaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() {
        val packageName = targetPackageName()
        Log.i(TAG, "collect starting for $packageName")
        baselineProfileRule.collect(
            packageName = packageName,
            includeInStartupProfile = true,
        ) {
            Log.i(TAG, "iteration launch for $packageName")
            startManagerForBaselineProfile()
        }
        Log.i(TAG, "collect finished for $packageName")
    }

    private companion object {
        const val TAG = "DpisBaselineProfile"
    }
}
