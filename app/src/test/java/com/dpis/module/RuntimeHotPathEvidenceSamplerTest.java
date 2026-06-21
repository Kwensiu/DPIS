package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RuntimeHotPathEvidenceSamplerTest {
    @Test
    public void emitsFirstHitAndCountedSampleWithSuppressedCount() {
        RuntimeHotPathEvidenceSampler sampler = new RuntimeHotPathEvidenceSampler();

        RuntimeHotPathEvidenceSampler.Sample first = sampler.sample("route|detail", "detail=value");

        assertTrue(first.emit);
        assertTrue(first.detail.contains("hitCount=1"));
        assertTrue(first.detail.contains("suppressedCount=0"));

        for (int i = 0; i < 48; i++) {
            RuntimeHotPathEvidenceSampler.Sample skipped =
                    sampler.sample("route|detail", "detail=value");
            assertFalse(skipped.emit);
        }

        RuntimeHotPathEvidenceSampler.Sample counted =
                sampler.sample("route|detail", "detail=value");

        assertTrue(counted.emit);
        assertEquals("detail=value, hitCount=50, suppressedCount=48", counted.detail);
    }
}
