package com.dpis.module.templates

import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.PackageConfigValue
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickTemplateApplyCoordinatorTest {
    @Test
    fun appliesTemplateToMultiplePackagesAndPublishesSuccessfulWrites() {
        val writer = FakeWriter()
        val publisher = RecordingPublisher()
        val coordinator = QuickTemplateApplyCoordinator<TemplateConfigValue>(writer, publisher)

        val result = coordinator.apply(
            template(
                orderedSet("com.example.one", "com.example.two"),
                templateValue(),
            )
        )

        assertFalse(result.emptySelection)
        assertEquals(2, result.successCount())
        assertEquals(0, result.failureCount())
        assertEquals(listOf("com.example.one", "com.example.two"), writer.writes)
        assertEquals(writer.writes, publisher.publishedPackages)
    }

    @Test
    fun unreadyTemplateApplyDoesNotWriteOrPublishSelectedPackages() {
        val writer = FakeWriter()
        val publisher = RecordingPublisher()
        val coordinator =
            QuickTemplateApplyCoordinator<TemplateConfigValue>(writer, publisher) { false }

        val result = coordinator.apply(
            template(
                orderedSet("com.example.one", "com.example.two"),
                templateValue(),
            )
        )

        assertEquals(0, result.successCount())
        assertEquals(2, result.failureCount())
        assertTrue(writer.writes.isEmpty())
        assertTrue(publisher.publishedPackages.isEmpty())
    }

    @Test
    fun overwritesConfiguredPackagesAndCountsThemInPlan() {
        val writer = FakeWriter().apply { configuredPackages.add("com.example.configured") }
        val coordinator =
            QuickTemplateApplyCoordinator<TemplateConfigValue>(writer, RecordingPublisher())

        val plan = coordinator.plan(
            template(
                orderedSet("com.example.configured", "com.example.new"),
                templateValue(),
            )
        )

        assertEquals(2, plan.targetCount)
        assertEquals(1, plan.overwriteCount)
    }

    @Test
    fun partialWriteFailureIsSummarizedWithoutRollback() {
        val writer = FakeWriter().apply { failedPackages.add("com.example.fail") }
        val publisher = RecordingPublisher()
        val coordinator = QuickTemplateApplyCoordinator<TemplateConfigValue>(writer, publisher)

        val result = coordinator.apply(
            template(
                orderedSet("com.example.ok", "com.example.fail"),
                templateValue(),
            )
        )

        assertEquals(listOf("com.example.ok"), result.successfulPackages)
        assertEquals(listOf("com.example.fail"), result.failedPackages)
        assertEquals(listOf("com.example.ok", "com.example.fail"), writer.writes)
        assertEquals(listOf("com.example.ok"), publisher.publishedPackages)
    }

    @Test
    fun emptySelectionBlocksApplyBeforeWriting() {
        val writer = FakeWriter()
        val coordinator =
            QuickTemplateApplyCoordinator<TemplateConfigValue>(writer, RecordingPublisher())

        val result = coordinator.apply(template(emptySet(), templateValue()))

        assertTrue(result.emptySelection)
        assertTrue(writer.writes.isEmpty())
    }

    @Test
    fun applyNormalizesPackageNamesBeforeWritingAndPublishing() {
        val writer = FakeWriter()
        val publisher = RecordingPublisher()
        val coordinator = QuickTemplateApplyCoordinator<TemplateConfigValue>(writer, publisher)

        val result = coordinator.apply(
            template(
                orderedSet("  com.example.target  ", "", "   "),
                templateValue(),
            )
        )

        assertEquals(listOf("com.example.target"), writer.writes)
        assertEquals(writer.writes, publisher.publishedPackages)
        assertEquals(writer.writes, result.successfulPackages)
    }

    @Test
    fun missingWriterReportsFailureWithoutPublishing() {
        val publisher = RecordingPublisher()
        val coordinator = QuickTemplateApplyCoordinator<TemplateConfigValue>(null, publisher)

        val result = coordinator.apply(
            template(
                orderedSet("com.example.target"),
                templateValue(),
            )
        )

        assertFalse(result.emptySelection)
        assertEquals(0, result.successCount())
        assertEquals(0, result.failureCount())
        assertTrue(publisher.publishedPackages.isEmpty())
    }

    @Test
    fun targetFilterSkipsStalePackagesBeforePlanningAndWriting() {
        val writer = FakeWriter().apply {
            configuredPackages.addAll(listOf("com.example.installed", "com.example.removed"))
        }
        val publisher = RecordingPublisher()
        val coordinator = QuickTemplateApplyCoordinator<TemplateConfigValue>(writer, publisher)
        val installedOnly = QuickTemplateApplyCoordinator.TargetPackageFilter {
            it != "com.example.removed"
        }
        val template = template(
            orderedSet("com.example.installed", "com.example.removed"),
            templateValue(),
        )

        val plan = coordinator.plan(template, installedOnly)
        val result = coordinator.apply(template, installedOnly)

        assertEquals(1, plan.targetCount)
        assertEquals(1, plan.overwriteCount)
        assertEquals(listOf("com.example.installed"), writer.writes)
        assertEquals(listOf("com.example.installed"), publisher.publishedPackages)
        assertEquals(listOf("com.example.installed"), result.successfulPackages)
        assertTrue(result.failedPackages.isEmpty())
    }

    @Test
    fun storeBackedApplyUsesPackageTemplateAdapterOnly() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(
            store.writePackageConfig(
                "com.tencent.mm",
                PackageConfigValue(
                    ViewportTargetSpec.relativeScale(120000),
                    ViewportTargetType.RELATIVE_SCALE,
                    ViewportApplyMode.SYSTEM,
                    130,
                    FontApplyMode.FIELD_REWRITE,
                    "source_font",
                    "resources_font",
                    false,
                    600,
                ),
            )
        )
        val coordinator = QuickTemplateApplyAdapters.from(store) { true }

        val result = coordinator.apply(
            template(
                orderedSet("com.example.target"),
                store.readPackageTemplateConfigValue("com.tencent.mm"),
            )
        )

        assertEquals(1, result.successCount())
        assertEquals(130, store.getTargetFontScalePercent("com.example.target"))
        assertEquals("source_font", store.getTargetTypefaceId("com.example.target"))
        assertTrue(store.isTargetDpisEnabled("com.example.target"))
        assertNull(store.getWechatDpi("com.example.target"))
        assertFalse(prefs.contains("target.com.example.target.dpis_enabled"))
        assertFalse(prefs.contains("package_config.com.example.target.target.dpis_enabled"))
        assertFalse(prefs.contains("wechat.com.example.target.dpi"))
        assertFalse(prefs.contains("package_config.com.example.target.app.wechat_dpi"))
    }

    @Test
    fun runtimePublishPlanMirrorsSingleAppSaveForEnabledTemplateValues() {
        val plan = QuickTemplateApplyAdapters.RuntimePublishPlan.from(
            templateValue(),
            HookDomainOverride(
                true,
                orderedSet("resources_font", "typeface"),
                setOf("unknown_domain"),
            ),
            true,
        )

        assertTrue(plan.publishViewport)
        assertEquals(ViewportTargetSpec.absoluteDp(411), plan.viewportTargetSpec)
        assertEquals(ViewportApplyMode.AUTO, plan.viewportApplyMode)
        assertTrue(plan.publishFontScale)
        assertEquals(115, plan.fontScalePercent)
        assertEquals(FontApplyMode.SYSTEM_EMULATION, plan.fontApplyMode)
        assertTrue(plan.hyperOsNativeFontHookEnabled)
        assertEquals("font_a", plan.typefaceId)
        assertTrue(plan.publishFontHookDomains)
        assertEquals(orderedSet("resources_font", "typeface"), plan.fontHookDomains)
    }

    @Test
    fun runtimePublishPlanClearsRuntimeStateForEmptyTemplateValues() {
        val plan = QuickTemplateApplyAdapters.RuntimePublishPlan.from(
            TemplateConfigValue.EMPTY,
            HookDomainOverride.automatic(),
            false,
        )

        assertFalse(plan.publishViewport)
        assertEquals(ViewportTargetSpec.off(), plan.viewportTargetSpec)
        assertEquals(ViewportApplyMode.OFF, plan.viewportApplyMode)
        assertFalse(plan.publishFontScale)
        assertNull(plan.fontScalePercent)
        assertEquals(FontApplyMode.OFF, plan.fontApplyMode)
        assertNull(plan.typefaceId)
        assertFalse(plan.publishFontHookDomains)
        assertTrue(plan.fontHookDomains.isEmpty())
    }

    @Test
    fun runtimePublishPlanIgnoresModeIntentWithoutRuntimeValues() {
        val plan = QuickTemplateApplyAdapters.RuntimePublishPlan.from(
            TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportTargetType.ABSOLUTE_DP,
                ViewportApplyMode.COMPAT,
                null,
                FontApplyMode.FIELD_REWRITE,
                null,
                null,
            ),
            HookDomainOverride.automatic(),
            false,
        )

        assertFalse(plan.publishViewport)
        assertEquals(ViewportTargetSpec.off(), plan.viewportTargetSpec)
        assertEquals(ViewportApplyMode.OFF, plan.viewportApplyMode)
        assertFalse(plan.publishFontScale)
        assertNull(plan.fontScalePercent)
        assertEquals(FontApplyMode.OFF, plan.fontApplyMode)
    }

    private fun template(
        packages: Set<String>,
        value: TemplateConfigValue,
    ) = QuickTemplateStore.QuickTemplate(
        "template_a",
        "Compact",
        1L,
        LinkedHashSet(packages),
        value,
    )

    private fun templateValue() = TemplateConfigValueAdapters.fromViewportTargetSpec(
        ViewportTargetSpec.absoluteDp(411),
        ViewportApplyMode.AUTO,
        115,
        FontApplyMode.SYSTEM_EMULATION,
        "font_a",
        "resources_font",
    )

    private fun orderedSet(vararg values: String) = LinkedHashSet(values.toList())

    private class FakeWriter : QuickTemplateApplyCoordinator.ConfigWriter<TemplateConfigValue?> {
        val configuredPackages = LinkedHashSet<String>()
        val failedPackages = LinkedHashSet<String>()
        val writes = ArrayList<String>()

        override fun hasRealPackageConfig(packageName: String?): Boolean =
            packageName in configuredPackages

        override fun writePackageTemplateConfigValue(
            packageName: String?,
            value: TemplateConfigValue?,
        ): Boolean {
            writes.add(packageName!!)
            return packageName !in failedPackages
        }
    }

    private class RecordingPublisher :
        QuickTemplateApplyCoordinator.RuntimePublisher<TemplateConfigValue?> {
        val publishedPackages = ArrayList<String>()

        override fun publish(packageName: String?, value: TemplateConfigValue?) {
            publishedPackages.add(packageName!!)
        }
    }
}
