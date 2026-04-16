package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LegacySmokeProbeTest {
    @Test
    public void emitsStableMarker() {
        assertEquals("legacy-smoke-system-v1", LegacySmokeProbe.marker());
    }

    @Test
    public void formatsLoadMessage() {
        assertEquals(
                "legacy smoke loaded: package=android, process=system, marker=legacy-smoke-system-v1",
                LegacySmokeProbe.loadMessage("android", "system"));
    }
}
