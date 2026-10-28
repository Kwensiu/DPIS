package com.dpis.module.runtime.resources

import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.runtime.resources.ResourcesReadHookTestSupport.PACKAGE_NAME
import com.dpis.module.runtime.resources.ResourcesReadHookTestSupport.request
import com.dpis.module.runtime.resources.ResourcesReadHookTestSupport.stableConfig
import com.dpis.module.viewport.ResourcesConfigurationReadReuse
import com.dpis.module.viewport.ResourcesMetricsReadReuse
import com.dpis.module.viewport.ViewportApplyMode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResourcesConfigurationReadReuseTest {
    private lateinit var store: DpisConfigStore

    @Before
    fun setUp() {
        store = DpisConfigStore(FakePrefs().also { prefs ->
            prefs.edit()
                .putInt("viewport.$PACKAGE_NAME.width_dp", 500)
                .putString("viewport.$PACKAGE_NAME.mode", ViewportApplyMode.COMPAT)
                .commit()
        })
        ResourcesConfigurationReadReuse.invalidate()
        RuntimeEvents.start(PACKAGE_NAME, request())
    }

    @After
    fun tearDown() {
        RuntimeEvents.cancel()
        RuntimeHotPathEvents.resetForTest()
        ResourcesConfigurationReadReuse.invalidate()
    }

    @Test
    fun repeatedReadOnSameResourcesScopeReusesStableCompatConfiguration() {
        val resources = Any()
        val config = stableConfig()

        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
            resources,
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
            false,
        )
        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
            resources,
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
            false,
        )

        val events = RuntimeEvents.stopSnapshot()
        assertEquals(500, config.screenWidthDp)
        assertTrue(
            events.toString(),
            events.any { it.contains("reason=stable_configuration_reused") },
        )
    }

    @Test
    fun viewportGenerationChangeInvalidatesStableConfigurationReuse() {
        val resources = Any()
        val config = stableConfig()

        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
            resources, config, PACKAGE_NAME, store, "ResourcesRead(getConfiguration)", false,
        )
        ResourcesMetricsReadReuse.bump()
        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
            resources, config, PACKAGE_NAME, store, "ResourcesRead(getConfiguration)", false,
        )

        val events = RuntimeEvents.stopSnapshot()
        assertTrue(
            events.toString(),
            events.none { it.contains("reason=stable_configuration_reused") },
        )
    }

    @Test
    fun differentResourcesScopeDoesNotReusePreviousConfiguration() {
        val first = stableConfig()
        val second = stableConfig()

        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
            Any(), first, PACKAGE_NAME, store, "ResourcesRead(getConfiguration)", false,
        )
        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
            Any(), second, PACKAGE_NAME, store, "ResourcesRead(getConfiguration)", false,
        )

        val events = RuntimeEvents.stopSnapshot()
        assertTrue(
            events.toString(),
            events.none { it.contains("reason=stable_configuration_reused") },
        )
        assertEquals(500, first.screenWidthDp)
        assertEquals(500, second.screenWidthDp)
    }
}
