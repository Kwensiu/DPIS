package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    public void keepsWorkingWhenSourceSmallestWidthIsZero() {
        VirtualDisplayOverride.Result result = DisplayOverridePipeline.derive(
                360, 736, 0, 480, 1080, 2208, 300);

        assertNotNull(result);
        assertEquals(300, result.widthDp);
        assertEquals(576, result.densityDpi);
    }
}
