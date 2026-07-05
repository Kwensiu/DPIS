package com.dpis.module;

import com.dpis.module.viewport.DensityOverride;

import com.dpis.module.viewport.ViewportTargetSpec;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class WebApkRuntimeOwnerBridgeTest {
    private static final String OWNER = "org.chromium.webapk.ac19cf34f94565db5_v2";

    @org.junit.Before
    public void setUp() {
        WebApkRuntimeOwnerBridge.resetForTest();
    }

    @Test
    public void extractsOwnerOnlyFromChromeWebApkActivityText() {
        assertEquals(
                "org.chromium.webapk.ac19cf34f94565db5_v2",
                WebApkRuntimeOwnerBridge.currentWebApkOwnerForTest(
                        "ActivityRecord{u0 com.android.chrome/"
                                + "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity "
                                + "dat=webapp://webapk-org.chromium.webapk.ac19cf34f94565db5_v2}"));
    }

    @Test
    public void ignoresNormalChromeActivityText() {
        assertNull(WebApkRuntimeOwnerBridge.currentWebApkOwnerForTest(
                "ActivityRecord{u0 com.android.chrome/"
                        + "org.chromium.chrome.browser.ChromeTabbedActivity}"));
    }

    @Test
    public void extractsOwnerFromActivityRecordIntentText() {
        assertEquals(
                "org.chromium.webapk.ac19cf34f94565db5_v2",
                WebApkRuntimeOwnerBridge.currentWebApkOwnerForTest(
                        "ActivityClientRecord{intent=Intent { "
                                + "dat=webapp://webapk-org.chromium.webapk.ac19cf34f94565db5_v2 "
                                + "cmp=com.android.chrome/"
                                + "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity }}"));
    }

    @Test
    public void cachesOwnerFromWebApkLifecycleIntent() {
        assertEquals(
                "org.chromium.webapk.ac19cf34f94565db5_v2",
                WebApkRuntimeOwnerBridge.observeActivityForTest(
                        "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity",
                        "Intent { dat=webapp://webapk-org.chromium.webapk.ac19cf34f94565db5_v2 }",
                        null,
                        "onResume"));
    }

    @Test
    public void clearsOwnerWhenNormalChromeActivityResumes() {
        WebApkRuntimeOwnerBridge.observeActivityForTest(
                "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity",
                "Intent { dat=webapp://webapk-org.chromium.webapk.ac19cf34f94565db5_v2 }",
                null,
                "onResume");

        assertNull(WebApkRuntimeOwnerBridge.observeActivityForTest(
                "org.chromium.chrome.browser.ChromeTabbedActivity",
                "Intent { cmp=com.android.chrome/.ChromeTabbedActivity }",
                null,
                "onResume"));
    }

    @Test
    public void emptyOwnerConfigDoesNotOverrideChromeCarrier() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetDpisEnabled(OWNER, true);

        assertFalse(WebApkRuntimeOwnerBridge.hasActiveOwnerConfigForTest(store, OWNER));
    }

    @Test
    public void activeOwnerConfigCanOverrideChromeCarrier() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetDpisEnabled(OWNER, true);
        store.setTargetViewportSpec(OWNER, ViewportTargetSpec.relativeScale(150000));

        org.junit.Assert.assertTrue(
                WebApkRuntimeOwnerBridge.hasActiveOwnerConfigForTest(store, OWNER));
    }

    @Test
    public void doesNotUsePendingOwnerPropertyBridge() throws IOException {
        String source = new String(Files.readAllBytes(Path.of(
                "src/main/java/com/dpis/module/WebApkRuntimeOwnerBridge.java")),
                StandardCharsets.UTF_8);

        assertFalse(source.contains("debug.dpis.webapk.pending_owner"));
        assertFalse(source.contains("consumePendingOwnerForChrome"));
    }

    @Test
    public void ownerLifecycleTriggersActivityResourceSync() throws IOException {
        String source = new String(Files.readAllBytes(Path.of(
                "src/main/java/com/dpis/module/WebApkRuntimeOwnerBridge.java")),
                StandardCharsets.UTF_8);

        assertContains(source, "syncActivityResources(activity, sourceTag)");
        assertContains(source, "ResourcesManagerHookInstaller.applyResourceOverrides(");
        assertContains(source, "ResourcesImplHookInstaller.applyDensityOverride(");
        assertContains(source, "AutoViewportRuntimeRoute.ANY_ENABLED_TARGET");
        assertContains(source, "DPIS_WEBAPK activity resources synced");
    }

    private static void assertContains(String source, String expected) {
        org.junit.Assert.assertTrue(source.contains(expected));
    }
}
