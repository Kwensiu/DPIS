package com.dpis.module;

import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import com.dpis.module.templates.TemplateConfigValueAdapters;

import com.dpis.module.templates.GlobalPrefillStore;

import com.dpis.module.templates.QuickTemplateStore;

import com.dpis.module.templates.TemplateConfigValue;

import com.dpis.module.settings.AppUiScaleManager;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DpisApplicationRuntimeDeliveryTest {

    @Test
    public void publishRuntimeConfigKeepsTemplatesAndGlobalPrefillLocalOnly() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpisConfigStore local = new DpisConfigStore(localPrefs);
        TemplateConfigValue globalPrefill = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.absoluteDp(411),
                ViewportApplyMode.AUTO,
                120,
                FontApplyMode.SYSTEM_EMULATION,
                "font_default",
                "resources_font");
        assertTrue(new GlobalPrefillStore(localPrefs).write(globalPrefill));
        QuickTemplateStore localTemplates = new QuickTemplateStore(localPrefs);
        assertTrue(localTemplates.save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Compact",
                1000L,
                Set.of("com.example.one"),
                TemplateConfigValueAdapters.fromViewportTargetSpec(
                        ViewportTargetSpec.relativeScale(110000),
                        ViewportApplyMode.COMPAT,
                        115,
                        FontApplyMode.FIELD_REWRITE,
                        "font_template",
                        "textview_sp"))));
        assertTrue(localTemplates.reorder(List.of("template_a")));

        FakePrefs deliveryPrefs = new FakePrefs();
        DpisConfigStore runtimeDelivery = new DpisConfigStore(deliveryPrefs);
        assertTrue(runtimeDelivery.setTargetFontScalePercent("com.miui.weather2", 200));

        invokePublish(local, runtimeDelivery);

        assertEquals(TemplateConfigValue.EMPTY, new GlobalPrefillStore(deliveryPrefs).read());
        assertNull(new QuickTemplateStore(deliveryPrefs).read("template_a"));
        assertEquals(globalPrefill, new GlobalPrefillStore(localPrefs).read());
        QuickTemplateStore.QuickTemplate localTemplate =
                new QuickTemplateStore(localPrefs).read("template_a");
        assertTrue(localTemplate != null);
        assertEquals("Compact", localTemplate.name);
        assertEquals(Set.of("com.example.one"), localTemplate.selectedPackages);
        assertEquals("font_template", localTemplate.configValue.typefaceId);
        assertNull(runtimeDelivery.getTargetFontScalePercent("com.miui.weather2"));
    }

    @Test
    public void publishRuntimeConfigClearsRemoteTemplateAndPrefillConfigAfterLocalDelete() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpisConfigStore local = new DpisConfigStore(localPrefs);

        FakePrefs deliveryPrefs = new FakePrefs();
        DpisConfigStore runtimeDelivery = new DpisConfigStore(deliveryPrefs);
        assertTrue(new GlobalPrefillStore(deliveryPrefs).write(TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.absoluteDp(512),
                ViewportApplyMode.SYSTEM,
                null,
                FontApplyMode.OFF,
                "remote_font",
                null)));
        assertTrue(new QuickTemplateStore(deliveryPrefs).save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Remote",
                2000L,
                Set.of("com.example.remote"),
                TemplateConfigValueAdapters.fromViewportTargetSpec(
                        ViewportTargetSpec.off(),
                        ViewportApplyMode.OFF,
                        null,
                        FontApplyMode.OFF,
                        "remote_template_font",
                        null))));
        assertTrue(runtimeDelivery.setTargetFontScalePercent("com.miui.weather2", 200));

        invokePublish(local, runtimeDelivery);

        assertEquals(TemplateConfigValue.EMPTY, new GlobalPrefillStore(localPrefs).read());
        assertNull(new QuickTemplateStore(localPrefs).read("template_a"));
        assertEquals(TemplateConfigValue.EMPTY, new GlobalPrefillStore(deliveryPrefs).read());
        assertNull(new QuickTemplateStore(deliveryPrefs).read("template_a"));
        assertNull(runtimeDelivery.getTargetFontScalePercent("com.miui.weather2"));
    }

    @Test
    public void publishRuntimeConfigOverwritesRemoteFromLocalAfterServiceBind() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpisConfigStore local = new DpisConfigStore(localPrefs);
        assertTrue(local.setHyperOsFlutterFontHookEnabled(false));

        FakePrefs deliveryPrefs = new FakePrefs();
        DpisConfigStore runtimeDelivery = new DpisConfigStore(deliveryPrefs);
        assertTrue(runtimeDelivery.setHyperOsFlutterFontHookEnabled(true));
        assertTrue(runtimeDelivery.setTargetFontScalePercent("com.miui.weather2", 200));
        assertTrue(runtimeDelivery.setTargetFontApplyMode("com.miui.weather2", FontApplyMode.FIELD_REWRITE));
        assertTrue(runtimeDelivery.setStartupDisclaimerAccepted(true));

        invokePublish(local, runtimeDelivery);

        assertFalse(local.isHyperOsFlutterFontHookEnabled());
        assertFalse(local.getConfiguredPackages().contains("com.miui.weather2"));
        assertFalse(runtimeDelivery.isHyperOsFlutterFontHookEnabled());
        assertFalse(runtimeDelivery.getConfiguredPackages().contains("com.miui.weather2"));
        assertNull(runtimeDelivery.getTargetFontScalePercent("com.miui.weather2"));
        assertFalse(local.isStartupDisclaimerAccepted());
        assertFalse(runtimeDelivery.isStartupDisclaimerAccepted());
    }

    @Test
    public void publishesWechatDpiToRemoteConfigOnServiceBind() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpisConfigStore local = new DpisConfigStore(localPrefs);
        assertTrue(local.setWechatDpi("com.tencent.mm", 600));

        FakePrefs deliveryPrefs = new FakePrefs();
        DpisConfigStore runtimeDelivery = new DpisConfigStore(deliveryPrefs);

        invokePublish(local, runtimeDelivery);

        assertEquals(Integer.valueOf(600), runtimeDelivery.getWechatDpi("com.tencent.mm"));
        assertTrue(runtimeDelivery.getConfiguredPackages().contains("com.tencent.mm"));
    }

    @Test
    public void publishRuntimeConfigDoesNotSendLocalStartupDisclaimerConsent() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpisConfigStore local = new DpisConfigStore(localPrefs);
        assertTrue(local.setStartupDisclaimerAccepted(true));

        FakePrefs deliveryPrefs = new FakePrefs();
        DpisConfigStore runtimeDelivery = new DpisConfigStore(deliveryPrefs);
        assertTrue(runtimeDelivery.setTargetFontScalePercent("com.miui.weather2", 200));

        invokePublish(local, runtimeDelivery);

        assertTrue(local.isStartupDisclaimerAccepted());
        assertFalse(runtimeDelivery.isStartupDisclaimerAccepted());
        assertNull(runtimeDelivery.getTargetFontScalePercent("com.miui.weather2"));
    }

    @Test
    public void publishRuntimeConfigDoesNotSendLocalOnlyUiState() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpisConfigStore local = new DpisConfigStore(localPrefs);
        assertTrue(local.setStartupDisclaimerAccepted(true));
        assertTrue(local.setInterfaceScalePercent(73));
        assertTrue(local.setTargetFontScalePercent("com.miui.weather2", 200));

        FakePrefs deliveryPrefs = new FakePrefs();
        DpisConfigStore runtimeDelivery = new DpisConfigStore(deliveryPrefs);

        invokePublish(local, runtimeDelivery);

        assertTrue(local.isStartupDisclaimerAccepted());
        assertEquals(73, local.getInterfaceScalePercent());
        assertFalse(runtimeDelivery.isStartupDisclaimerAccepted());
        assertEquals(AppUiScaleManager.DEFAULT_SCALE_PERCENT, runtimeDelivery.getInterfaceScalePercent());
        assertEquals(Integer.valueOf(200), runtimeDelivery.getTargetFontScalePercent("com.miui.weather2"));
    }

    @Test
    public void publishRuntimeConfigClearsRemoteOnlyUiState() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpisConfigStore local = new DpisConfigStore(localPrefs);

        FakePrefs deliveryPrefs = new FakePrefs();
        deliveryPrefs.edit()
                .putBoolean(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
                .putInt(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT, 73)
                .commit();
        DpisConfigStore runtimeDelivery = new DpisConfigStore(deliveryPrefs);

        invokePublish(local, runtimeDelivery);

        assertFalse(local.isStartupDisclaimerAccepted());
        assertEquals(AppUiScaleManager.DEFAULT_SCALE_PERCENT, local.getInterfaceScalePercent());
        assertFalse(runtimeDelivery.isStartupDisclaimerAccepted());
        assertEquals(AppUiScaleManager.DEFAULT_SCALE_PERCENT, runtimeDelivery.getInterfaceScalePercent());
    }

    private static void invokePublish(DpisConfigStore from, DpisConfigStore to) throws Exception {
        Method method = DpisApplication.class.getDeclaredMethod(
                "publishRuntimeConfig", DpisConfigStore.class, DpisConfigStore.class);
        method.setAccessible(true);
        method.invoke(null, from, to);
    }
}

