package com.dpis.module.root;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class RootAccessProbeTest {
    @Test
    public void identifiesKernelSuFromSuVersionWhenEnvironmentIsEmpty() {
        assertEquals(
                "KernelSU",
                RootAccessProbe.resolveProvider(
                        "uid=0(root)\nDPIS_KSU=\nDPIS_MAGISK_VER=",
                        "4.2.0-rc1-10-gfaccf4c5:KernelSU"
                )
        );
    }

    @Test
    public void identifiesMagiskFromSuVersionBeforeShellEnvironment() {
        assertEquals(
                "Magisk",
                RootAccessProbe.resolveProvider(
                        "uid=0(root)\nDPIS_KSU=true",
                        "26.4:MAGISKSU"
                )
        );
    }

    @Test
    public void fallsBackToShellEnvironmentWhenSuVersionIsUnavailable() {
        assertEquals(
                "KernelSU",
                RootAccessProbe.resolveProvider(
                        "DPIS_KSU_VER=1.0.0",
                        ""
                )
        );
    }
}
