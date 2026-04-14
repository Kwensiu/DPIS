package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProbeHookInstallerTest {
    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
    }

    @Test
    public void buildsResourcesDisplayMetricsLog() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 900;
        metrics.heightPixels = 1840;
        metrics.densityDpi = 576;
        metrics.density = 3.6f;
        metrics.scaledDensity = 3.6f;

        assertEquals(
                "Resources probe(getDisplayMetrics): widthPx=900, heightPx=1840, densityDpi=576, density=3.6, scaledDensity=3.6",
                ResourcesProbeHookInstaller.buildDisplayMetricsLog(metrics));
    }

    @Test
    public void buildsResourcesConfigurationLog() {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = 300;
        configuration.screenHeightDp = 613;
        configuration.smallestScreenWidthDp = 300;
        configuration.densityDpi = 576;

        assertEquals(
                "Resources probe(getConfiguration): widthDp=300, heightDp=613, smallestWidthDp=300, densityDpi=576",
                ResourcesProbeHookInstaller.buildConfigurationLog(configuration));
    }

    @Test
    public void buildsViewRootProbeLog() {
        FakeViewRoot root = new FakeViewRoot(900, 1840);

        assertEquals("ViewRoot probe(performTraversals): width=900, height=1840",
                ViewRootProbeHookInstaller.buildPerformTraversalsLog(root));
    }

    @Test
    public void appliesVirtualRootSizeToFakeViewRoot() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(300, 613, 300,
                576, 900, 1840));
        FakeViewRoot root = new FakeViewRoot(1080, 2376);

        assertTrue(ViewRootProbeHookInstaller.applyRootSizeOverride(root));
        assertEquals(900, root.mWidth);
        assertEquals(1840, root.mHeight);
    }

    @Test
    public void skipsVirtualRootSizeWhenAlreadyAligned() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(300, 613, 300,
                576, 900, 1840));
        FakeViewRoot root = new FakeViewRoot(900, 1840);

        assertFalse(ViewRootProbeHookInstaller.applyRootSizeOverride(root));
        assertEquals(900, root.mWidth);
        assertEquals(1840, root.mHeight);
    }

    @Test
    public void skipsVirtualRootSizeWhenRootNotInitialized() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(300, 613, 300,
                576, 900, 1840));
        FakeViewRoot root = new FakeViewRoot(-1, -1);

        assertFalse(ViewRootProbeHookInstaller.applyRootSizeOverride(root));
        assertEquals(-1, root.mWidth);
        assertEquals(-1, root.mHeight);
    }

    private static final class FakeViewRoot {
        @SuppressWarnings("unused")
        private int mWidth;
        @SuppressWarnings("unused")
        private int mHeight;

        private FakeViewRoot(int width, int height) {
            this.mWidth = width;
            this.mHeight = height;
        }
    }
}
