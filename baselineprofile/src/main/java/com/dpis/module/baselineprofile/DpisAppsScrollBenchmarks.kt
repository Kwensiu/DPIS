package com.dpis.module.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.dpis.module.baselineprofile.DpisProfileJourneys.TAG_NAV_APP
import com.dpis.module.baselineprofile.DpisProfileJourneys.openWorkspace
import com.dpis.module.baselineprofile.DpisProfileJourneys.startManagerAndDismissOverlays
import com.dpis.module.baselineprofile.DpisProfileJourneys.swipeContentUp
import com.dpis.module.baselineprofile.DpisProfileJourneys.targetPackageName
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class DpisAppsScrollBenchmarks {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun appsScrollWithBaselineProfiles() {
        rule.measureRepeated(
            packageName = targetPackageName(),
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = { pressHome() },
            measureBlock = {
                startManagerAndDismissOverlays()
                openWorkspace(TAG_NAV_APP)
                swipeContentUp()
            },
        )
    }
}
