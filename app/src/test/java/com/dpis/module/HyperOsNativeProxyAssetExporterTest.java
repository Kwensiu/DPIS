package com.dpis.module;

import org.junit.Test;

import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HyperOsNativeProxyAssetExporterTest {
    @Test
    public void resolverUsesFirstSupportedAvailableAbi() {
        LinkedHashSet<String> available = new LinkedHashSet<>();
        available.add("native/armeabi-v7a/libdpis_native.so");
        available.add("native/arm64-v8a/libdpis_native.so");

        String path = HyperOsNativeProxyAssetExporter.resolveNativeProxyAssetPath(available,
                new String[]{"x86_64", "arm64-v8a", "armeabi-v7a"});

        assertEquals("native/arm64-v8a/libdpis_native.so", path);
    }

    @Test
    public void resolverFallsBackToArm64ThenFirstAvailable() {
        LinkedHashSet<String> available = new LinkedHashSet<>();
        available.add("native/armeabi-v7a/libdpis_native.so");
        available.add("native/arm64-v8a/libdpis_native.so");

        String arm64Path = HyperOsNativeProxyAssetExporter.resolveNativeProxyAssetPath(available,
                new String[]{"x86_64"});
        String firstPath = HyperOsNativeProxyAssetExporter.resolveNativeProxyAssetPath(available,
                new String[]{"x86_64", "x86"});

        assertEquals("native/arm64-v8a/libdpis_native.so", arm64Path);
        assertEquals("native/arm64-v8a/libdpis_native.so", firstPath);
    }

    @Test
    public void resolverReturnsNullWhenNoAssetsAvailable() {
        assertNull(HyperOsNativeProxyAssetExporter.resolveNativeProxyAssetPath(new LinkedHashSet<>(),
                new String[]{"arm64-v8a"}));
        assertNull(HyperOsNativeProxyAssetExporter.resolveNativeProxyAssetPath(null,
                new String[]{"arm64-v8a"}));
    }
}