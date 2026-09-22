package com.dpis.module.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.UiObject2

internal object DpisProfileJourneys {
    const val TAG_NAV_APP = "workspace-nav-app"
    const val TAG_NAV_TEMPLATE = "workspace-nav-template"
    const val TAG_NAV_HOME = "workspace-nav-home"
    const val TAG_NAV_TOOLS = "workspace-nav-tools"
    const val TAG_NAV_SETTINGS = "workspace-nav-settings"
    const val TAG_DISCLAIMER_AGREEMENT = "startup-disclaimer-agreement"
    const val TAG_DISCLAIMER_ACCEPT = "startup-disclaimer-accept"
    const val TAG_RELOAD_ACK = "runtime-reload-notice-ack"

    private const val GET_INSTALLED_APPS = "com.android.permission.GET_INSTALLED_APPS"
    private const val LAUNCHER_ACTIVITY = "com.dpis.module.MainActivityLauncher"
    private const val BASELINE_PROFILE_STARTUP_WAIT_MS = 3_000L
    private const val UI_TIMEOUT_MS = 8_000L
    private const val POLL_MS = 250L
    private val NAV_TAGS = listOf(
        TAG_NAV_APP,
        TAG_NAV_TEMPLATE,
        TAG_NAV_HOME,
        TAG_NAV_TOOLS,
        TAG_NAV_SETTINGS,
    )

    fun targetPackageName(): String {
        return InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: throw IllegalStateException("targetAppId not passed as instrumentation runner arg")
    }

    fun shortenUiAutomatorIdleTimeouts() {
        Configurator.getInstance().apply {
            waitForIdleTimeout = 1_000
            waitForSelectorTimeout = 1_000
            actionAcknowledgmentTimeout = 1_000
        }
    }

    fun MacrobenchmarkScope.startManagerAndDismissOverlays() {
        grantInstalledAppsPermission()
        device.executeShellCommand("input keyevent KEYCODE_HOME")
        SystemClock.sleep(400)
        device.executeShellCommand(
            "am start -a android.intent.action.MAIN " +
                    "-c android.intent.category.LAUNCHER " +
                    "-n $packageName/$LAUNCHER_ACTIVITY",
        )
        firstExisting(listOf(TAG_DISCLAIMER_AGREEMENT) + NAV_TAGS, UI_TIMEOUT_MS)
            ?: error("manager UI not found after launching $packageName")
        val disclaimer = firstExisting(listOf(TAG_DISCLAIMER_AGREEMENT), POLL_MS)
        if (disclaimer != null) {
            disclaimer.click()
            clickIfPresent(TAG_DISCLAIMER_ACCEPT, UI_TIMEOUT_MS)
        }
        clickIfPresent(TAG_RELOAD_ACK)
        waitForWorkspaceNavigation()
    }

    /**
     * Launches the manager without querying its UI tree.
     *
     * Baseline profile startup collection must also work on emulators whose
     * accessibility bridge does not expose Compose nodes reliably. The fixed
     * wait keeps this path focused on startup execution rather than UI
     * automation availability.
     */
    fun MacrobenchmarkScope.startManagerForBaselineProfile() {
        grantInstalledAppsPermission()
        device.executeShellCommand("input keyevent KEYCODE_HOME")
        SystemClock.sleep(400)
        device.executeShellCommand(
            "am start -a android.intent.action.MAIN " +
                    "-c android.intent.category.LAUNCHER " +
                    "-n $packageName/$LAUNCHER_ACTIVITY",
        )
        SystemClock.sleep(BASELINE_PROFILE_STARTUP_WAIT_MS)
    }

    fun MacrobenchmarkScope.grantInstalledAppsPermission() {
        device.executeShellCommand("pm grant $packageName $GET_INSTALLED_APPS")
    }

    fun MacrobenchmarkScope.waitForWorkspaceNavigation() {
        firstExisting(NAV_TAGS, UI_TIMEOUT_MS)
            ?: error("workspace navigation not found after launching $packageName")
    }

    fun MacrobenchmarkScope.openWorkspace(tag: String) {
        val node = firstExisting(listOf(tag), UI_TIMEOUT_MS)
            ?: error("workspace destination not found: $tag")
        node.click()
        SystemClock.sleep(300)
    }

    fun MacrobenchmarkScope.swipeContentUp() {
        val width = device.displayWidth
        val height = device.displayHeight
        device.swipe(width / 2, (height * 0.72).toInt(), width / 2, (height * 0.28).toInt(), 18)
        SystemClock.sleep(300)
    }

    private fun MacrobenchmarkScope.clickIfPresent(
        tag: String,
        timeoutMs: Long = POLL_MS,
    ) {
        firstExisting(listOf(tag), timeoutMs)?.click()
    }

    private fun MacrobenchmarkScope.firstExisting(
        tags: List<String>,
        timeoutMs: Long,
    ): UiObject2? {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            tags.forEach { tag ->
                val node = runCatching { device.findObject(By.res(tag)) }.getOrNull()
                if (node != null) {
                    return node
                }
            }
            SystemClock.sleep(POLL_MS)
        }
        return null
    }
}
