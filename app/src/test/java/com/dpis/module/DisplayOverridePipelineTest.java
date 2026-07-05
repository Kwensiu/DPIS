package com.dpis.module;

import com.dpis.module.viewport.VirtualDisplayOverride;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DisplayOverridePipelineTest {
    @Test
    public void returnsNullWhenTargetWidthIsZero() {
        assertNull(DisplayOverridePipeline.derive(360, 736, 360, 480, 1080, 2208, 0));
    }

    @Test
    public void keepsPhysicalPixelsWhileShrinkingLogicalViewport() {
        VirtualDisplayOverride.Result result = DisplayOverridePipeline.derive(
                360, 736, 360, 480, 1080, 2208, 300);

        assertEquals(300, result.widthDp);
        assertEquals(613, result.heightDp);
        assertEquals(300, result.smallestWidthDp);
        assertEquals(576, result.densityDpi);
        assertEquals(1080, result.widthPx);
        assertEquals(2208, result.heightPx);
    }

    @Test
    public void returnsNullWhenSourceSmallestWidthIsZero() {
        VirtualDisplayOverride.Result result = DisplayOverridePipeline.derive(
                360, 736, 0, 480, 1080, 2208, 300);

        assertNull(result);
    }

    @Test
    public void targetMatchingSourceSmallestWidthIsIdentity() {
        VirtualDisplayOverride.Result result = DisplayOverridePipeline.derive(
                393, 800, 360, 480, 1080, 2208, 360);

        assertEquals(393, result.widthDp);
        assertEquals(800, result.heightDp);
        assertEquals(360, result.smallestWidthDp);
        assertEquals(480, result.densityDpi);
        assertEquals(1080, result.widthPx);
        assertEquals(2208, result.heightPx);
    }
}
