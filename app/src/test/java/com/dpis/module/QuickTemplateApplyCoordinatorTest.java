package com.dpis.module;
import com.dpis.module.templates.QuickTemplateApplyAdapters;
import com.dpis.module.templates.TemplateConfigValueAdapters;

import com.dpis.module.templates.QuickTemplateStore;

import com.dpis.module.templates.TemplateConfigValue;

import com.dpis.module.templates.QuickTemplateApplyCoordinator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class QuickTemplateApplyCoordinatorTest {
    @Test
    public void appliesTemplateToMultiplePackagesAndPublishesSuccessfulWrites() {
        FakeWriter writer = new FakeWriter();
        RecordingPublisher publisher = new RecordingPublisher();
        QuickTemplateApplyCoordinator<TemplateConfigValue> coordinator =
                new QuickTemplateApplyCoordinator<>(writer, publisher);
        TemplateConfigValue value = templateValue();

        QuickTemplateApplyCoordinator.Result result = coordinator.apply(template(
                orderedSet("com.example.one", "com.example.two"), value));

        assertFalse(result.emptySelection);
        assertEquals(2, result.successCount());
        assertEquals(0, result.failureCount());
        assertEquals(List.of("com.example.one", "com.example.two"), writer.writes);
        assertEquals(writer.writes, publisher.publishedPackages);
    }

    @Test
    public void overwritesConfiguredPackagesAndCountsThemInPlan() {
        FakeWriter writer = new FakeWriter();
        writer.configuredPackages.add("com.example.configured");
        QuickTemplateApplyCoordinator<TemplateConfigValue> coordinator =
                new QuickTemplateApplyCoordinator<>(writer, new RecordingPublisher());

        QuickTemplateApplyCoordinator.Plan plan = coordinator.plan(template(
                orderedSet("com.example.configured", "com.example.new"), templateValue()));

        assertEquals(2, plan.targetCount);
        assertEquals(1, plan.overwriteCount);
    }

    @Test
    public void partialWriteFailureIsSummarizedWithoutRollback() {
        FakeWriter writer = new FakeWriter();
        writer.failedPackages.add("com.example.fail");
        RecordingPublisher publisher = new RecordingPublisher();
        QuickTemplateApplyCoordinator<TemplateConfigValue> coordinator =
                new QuickTemplateApplyCoordinator<>(writer, publisher);

        QuickTemplateApplyCoordinator.Result result = coordinator.apply(template(
                orderedSet("com.example.ok", "com.example.fail"), templateValue()));

        assertEquals(List.of("com.example.ok"), result.successfulPackages);
        assertEquals(List.of("com.example.fail"), result.failedPackages);
        assertEquals(List.of("com.example.ok", "com.example.fail"), writer.writes);
        assertEquals(List.of("com.example.ok"), publisher.publishedPackages);
    }

    @Test
    public void emptySelectionBlocksApplyBeforeWriting() {
        FakeWriter writer = new FakeWriter();
        QuickTemplateApplyCoordinator<TemplateConfigValue> coordinator =
                new QuickTemplateApplyCoordinator<>(writer, new RecordingPublisher());

        QuickTemplateApplyCoordinator.Result result = coordinator.apply(template(
                Set.of(), templateValue()));

        assertTrue(result.emptySelection);
        assertTrue(writer.writes.isEmpty());
    }

    @Test
    public void targetFilterSkipsStalePackagesBeforePlanningAndWriting() {
        FakeWriter writer = new FakeWriter();
        writer.configuredPackages.add("com.example.installed");
        writer.configuredPackages.add("com.example.removed");
        RecordingPublisher publisher = new RecordingPublisher();
        QuickTemplateApplyCoordinator<TemplateConfigValue> coordinator =
                new QuickTemplateApplyCoordinator<>(writer, publisher);
        QuickTemplateApplyCoordinator.TargetPackageFilter installedOnly =
                packageName -> !"com.example.removed".equals(packageName);
        QuickTemplateStore.QuickTemplate template = template(
                orderedSet("com.example.installed", "com.example.removed"),
                templateValue());

        QuickTemplateApplyCoordinator.Plan plan = coordinator.plan(template, installedOnly);
        QuickTemplateApplyCoordinator.Result result = coordinator.apply(template, installedOnly);

        assertEquals(1, plan.targetCount);
        assertEquals(1, plan.overwriteCount);
        assertEquals(List.of("com.example.installed"), writer.writes);
        assertEquals(List.of("com.example.installed"), publisher.publishedPackages);
        assertEquals(List.of("com.example.installed"), result.successfulPackages);
        assertTrue(result.failedPackages.isEmpty());
    }

    @Test
    public void storeBackedApplyUsesPackageTemplateAdapterOnly() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.writePackageConfig("com.tencent.mm", new PackageConfigValue(
                ViewportTargetSpec.relativeScale(120000),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.SYSTEM,
                130,
                FontApplyMode.FIELD_REWRITE,
                "source_font",
                "resources_font",
                false,
                600)));
        QuickTemplateApplyCoordinator<TemplateConfigValue> coordinator =
                QuickTemplateApplyAdapters.from(store);

        QuickTemplateApplyCoordinator.Result result = coordinator.apply(template(
                orderedSet("com.example.target"),
                store.readPackageTemplateConfigValue("com.tencent.mm")));

        assertEquals(1, result.successCount());
        assertEquals(Integer.valueOf(130),
                store.getTargetFontScalePercent("com.example.target"));
        assertEquals("source_font", store.getTargetTypefaceId("com.example.target"));
        assertTrue(store.isTargetDpisEnabled("com.example.target"));
        assertNull(store.getWechatDpi("com.example.target"));
        assertFalse(prefs.contains("target.com.example.target.dpis_enabled"));
        assertFalse(prefs.contains("package_config.com.example.target.target.dpis_enabled"));
        assertFalse(prefs.contains("wechat.com.example.target.dpi"));
        assertFalse(prefs.contains("package_config.com.example.target.app.wechat_dpi"));
    }

    @Test
    public void runtimePublishPlanMirrorsSingleAppSaveForEnabledTemplateValues() {
        QuickTemplateApplyAdapters.RuntimePublishPlan plan =
                QuickTemplateApplyAdapters.RuntimePublishPlan.from(
                        templateValue(),
                        new HookDomainOverride(
                                true,
                                orderedSet("resources_font", "typeface"),
                                Set.of("unknown_domain")),
                        true);

        assertTrue(plan.publishViewport);
        assertEquals(ViewportTargetSpec.absoluteDp(411), plan.viewportTargetSpec);
        assertEquals(ViewportApplyMode.AUTO, plan.viewportApplyMode);
        assertTrue(plan.publishFontScale);
        assertEquals(Integer.valueOf(115), plan.fontScalePercent);
        assertEquals(FontApplyMode.SYSTEM_EMULATION, plan.fontApplyMode);
        assertTrue(plan.hyperOsNativeFontHookEnabled);
        assertEquals("font_a", plan.typefaceId);
        assertTrue(plan.publishFontHookDomains);
        assertEquals(orderedSet("resources_font", "typeface"), plan.fontHookDomains);
    }

    @Test
    public void runtimePublishPlanClearsRuntimeStateForEmptyTemplateValues() {
        QuickTemplateApplyAdapters.RuntimePublishPlan plan =
                QuickTemplateApplyAdapters.RuntimePublishPlan.from(
                        TemplateConfigValue.EMPTY,
                        HookDomainOverride.automatic(),
                        false);

        assertFalse(plan.publishViewport);
        assertEquals(ViewportTargetSpec.off(), plan.viewportTargetSpec);
        assertEquals(ViewportApplyMode.OFF, plan.viewportApplyMode);
        assertFalse(plan.publishFontScale);
        assertNull(plan.fontScalePercent);
        assertEquals(FontApplyMode.OFF, plan.fontApplyMode);
        assertNull(plan.typefaceId);
        assertFalse(plan.publishFontHookDomains);
        assertTrue(plan.fontHookDomains.isEmpty());
    }

    @Test
    public void runtimePublishPlanIgnoresModeIntentWithoutRuntimeValues() {
        QuickTemplateApplyAdapters.RuntimePublishPlan plan =
                QuickTemplateApplyAdapters.RuntimePublishPlan.from(
                        TemplateConfigValueAdapters.fromViewportTargetSpec(
                                ViewportTargetSpec.off(),
                                ViewportTargetType.ABSOLUTE_DP,
                                ViewportApplyMode.COMPAT,
                                null,
                                FontApplyMode.FIELD_REWRITE,
                                null,
                                null),
                        HookDomainOverride.automatic(),
                        false);

        assertFalse(plan.publishViewport);
        assertEquals(ViewportTargetSpec.off(), plan.viewportTargetSpec);
        assertEquals(ViewportApplyMode.OFF, plan.viewportApplyMode);
        assertFalse(plan.publishFontScale);
        assertNull(plan.fontScalePercent);
        assertEquals(FontApplyMode.OFF, plan.fontApplyMode);
    }

    private static QuickTemplateStore.QuickTemplate template(
            Set<String> packages,
            TemplateConfigValue value) {
        return new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Compact",
                1L,
                new LinkedHashSet<>(packages),
                value);
    }

    private static TemplateConfigValue templateValue() {
        return TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.absoluteDp(411),
                ViewportApplyMode.AUTO,
                115,
                FontApplyMode.SYSTEM_EMULATION,
                "font_a",
                "resources_font");
    }

    private static LinkedHashSet<String> orderedSet(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    private static final class FakeWriter
            implements QuickTemplateApplyCoordinator.ConfigWriter<TemplateConfigValue> {
        final LinkedHashSet<String> configuredPackages = new LinkedHashSet<>();
        final LinkedHashSet<String> failedPackages = new LinkedHashSet<>();
        final ArrayList<String> writes = new ArrayList<>();

        @Override
        public boolean hasRealPackageConfig(String packageName) {
            return configuredPackages.contains(packageName);
        }

        @Override
        public boolean writePackageTemplateConfigValue(String packageName, TemplateConfigValue value) {
            writes.add(packageName);
            return !failedPackages.contains(packageName);
        }
    }

    private static final class RecordingPublisher
            implements QuickTemplateApplyCoordinator.RuntimePublisher<TemplateConfigValue> {
        final ArrayList<String> publishedPackages = new ArrayList<>();

        @Override
        public void publish(String packageName, TemplateConfigValue value) {
            publishedPackages.add(packageName);
        }
    }
}

