package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WebApkCarrierResolverTest {
    @Test
    public void extractsOwnerFromChromeExtraText() {
        assertEquals(
                "org.chromium.webapk.a5e359e2ce8b830bb_v2",
                WebApkCarrierResolver.ownerPackageFromText(
                        "extras={org.chromium.chrome.browser.webapk_package_name="
                                + "org.chromium.webapk.a5e359e2ce8b830bb_v2}"));
    }

    @Test
    public void extractsOwnerFromWebappUri() {
        assertEquals(
                "org.chromium.webapk.a5e359e2ce8b830bb_v2",
                WebApkCarrierResolver.ownerPackageFromText(
                        "dat=webapp://webapk-org.chromium.webapk.a5e359e2ce8b830bb_v2/..."));
    }

    @Test
    public void rejectsNonWebApkPackages() {
        assertNull(WebApkCarrierResolver.ownerPackageFromText(
                "extras={org.chromium.chrome.browser.webapk_package_name=com.android.chrome}"));
        assertFalse(WebApkCarrierResolver.isWebApkOwnerPackage("com.android.chrome"));
        assertTrue(WebApkCarrierResolver.isWebApkOwnerPackage(
                "org.chromium.webapk.a5e359e2ce8b830bb_v2"));
    }
}
