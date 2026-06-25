package com.dpis.module;

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
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        TemplateConfigValue globalPrefill = new TemplateConfigValue(
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
                new TemplateConfigValue(
                        ViewportTargetSpec.relativeScale(110000),
                        ViewportApplyMode.COMPAT,
                        115,
                        FontApplyMode.FIELD_REWRITE,
                        "font_template",
                        "textview_sp"))));
        assertTrue(localTemplates.reorder(List.of("template_a")));

        FakePrefs deliveryPrefs = new FakePrefs();
        DpiConfigStore runtimeDelivery = new DpiConfigStore(deliveryPrefs);
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
        DpiConfigStore local = new DpiConfigStore(localPrefs);

        FakePrefs deliveryPrefs = new FakePrefs();
        DpiConfigStore runtimeDelivery = new DpiConfigStore(deliveryPrefs);
        assertTrue(new GlobalPrefillStore(deliveryPrefs).write(new TemplateConfigValue(
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
                new TemplateConfigValue(
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
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setHyperOsFlutterFontHookEnabled(false));

        FakePrefs deliveryPrefs = new FakePrefs();
        DpiConfigStore runtimeDelivery = new DpiConfigStore(deliveryPrefs);
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
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setWechatDpi("com.tencent.mm", 600));

        FakePrefs deliveryPrefs = new FakePrefs();
        DpiConfigStore runtimeDelivery = new DpiConfigStore(deliveryPrefs);

        invokePublish(local, runtimeDelivery);

        assertEquals(Integer.valueOf(600), runtimeDelivery.getWechatDpi("com.tencent.mm"));
        assertTrue(runtimeDelivery.getConfiguredPackages().contains("com.tencent.mm"));
    }

    @Test
    public void publishRuntimeConfigDoesNotSendLocalStartupDisclaimerConsent() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setStartupDisclaimerAccepted(true));

        FakePrefs deliveryPrefs = new FakePrefs();
        DpiConfigStore runtimeDelivery = new DpiConfigStore(deliveryPrefs);
        assertTrue(runtimeDelivery.setTargetFontScalePercent("com.miui.weather2", 200));

        invokePublish(local, runtimeDelivery);

        assertTrue(local.isStartupDisclaimerAccepted());
        assertFalse(runtimeDelivery.isStartupDisclaimerAccepted());
        assertNull(runtimeDelivery.getTargetFontScalePercent("com.miui.weather2"));
    }

    @Test
    public void publishRuntimeConfigDoesNotSendLocalOnlyUiState() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);
        assertTrue(local.setStartupDisclaimerAccepted(true));
        assertTrue(local.setInterfaceScalePercent(73));
        assertTrue(local.setLauncherIconHidden(true));
        assertTrue(local.setTargetFontScalePercent("com.miui.weather2", 200));

        FakePrefs deliveryPrefs = new FakePrefs();
        DpiConfigStore runtimeDelivery = new DpiConfigStore(deliveryPrefs);

        invokePublish(local, runtimeDelivery);

        assertTrue(local.isStartupDisclaimerAccepted());
        assertEquals(73, local.getInterfaceScalePercent());
        assertTrue(local.isLauncherIconHidden());
        assertFalse(runtimeDelivery.isStartupDisclaimerAccepted());
        assertEquals(AppUiScaleManager.DEFAULT_SCALE_PERCENT, runtimeDelivery.getInterfaceScalePercent());
        assertFalse(runtimeDelivery.isLauncherIconHidden());
        assertEquals(Integer.valueOf(200), runtimeDelivery.getTargetFontScalePercent("com.miui.weather2"));
    }

    @Test
    public void publishRuntimeConfigClearsRemoteOnlyUiState() throws Exception {
        FakePrefs localPrefs = new FakePrefs();
        DpiConfigStore local = new DpiConfigStore(localPrefs);

        FakePrefs deliveryPrefs = new FakePrefs();
        deliveryPrefs.edit()
                .putBoolean(DpiConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
                .putInt(DpiConfigStore.KEY_INTERFACE_SCALE_PERCENT, 73)
                .putBoolean(DpiConfigStore.KEY_HIDE_LAUNCHER_ICON, true)
                .commit();
        DpiConfigStore runtimeDelivery = new DpiConfigStore(deliveryPrefs);

        invokePublish(local, runtimeDelivery);

        assertFalse(local.isStartupDisclaimerAccepted());
        assertEquals(AppUiScaleManager.DEFAULT_SCALE_PERCENT, local.getInterfaceScalePercent());
        assertFalse(local.isLauncherIconHidden());
        assertFalse(runtimeDelivery.isStartupDisclaimerAccepted());
        assertEquals(AppUiScaleManager.DEFAULT_SCALE_PERCENT, runtimeDelivery.getInterfaceScalePercent());
        assertFalse(runtimeDelivery.isLauncherIconHidden());
    }

    private static void invokePublish(DpiConfigStore from, DpiConfigStore to) throws Exception {
        Method method = DpisApplication.class.getDeclaredMethod(
                "publishRuntimeConfig", DpiConfigStore.class, DpiConfigStore.class);
        method.setAccessible(true);
        method.invoke(null, from, to);
    }
}

