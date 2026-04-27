package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

public class HyperOsNativeProxyStatusTest {
    @Test
    public void inspectNativeLibraryDirDetectsProxyLibrary() throws Exception {
        File nativeDir = Files.createTempDirectory("dpis-native-proxy").toFile();
        File proxy = new File(nativeDir, "libdpis_native.so");
        assertTrue(proxy.createNewFile());

        HyperOsNativeProxyStatus status = HyperOsNativeProxyStatus.inspectNativeLibraryDir(
                nativeDir.getAbsolutePath());

        assertEquals(HyperOsNativeProxyStatus.State.PRESENT, status.state);
        assertTrue(status.isPresent());
    }

    @Test
    public void inspectNativeLibraryDirReportsMissingWhenDirectoryExistsWithoutProxy() throws Exception {
        File nativeDir = Files.createTempDirectory("dpis-native-proxy-missing").toFile();

        HyperOsNativeProxyStatus status = HyperOsNativeProxyStatus.inspectNativeLibraryDir(
                nativeDir.getAbsolutePath());

        assertEquals(HyperOsNativeProxyStatus.State.MISSING, status.state);
        assertFalse(status.isPresent());
    }

    @Test
    public void inspectNativeLibraryDirReportsUnknownForBlankOrAbsentDirectory() {
        HyperOsNativeProxyStatus blank = HyperOsNativeProxyStatus.inspectNativeLibraryDir("");
        HyperOsNativeProxyStatus absent = HyperOsNativeProxyStatus.inspectNativeLibraryDir(
                new File("build/nonexistent-dpis-native-proxy-dir").getAbsolutePath());

        assertEquals(HyperOsNativeProxyStatus.State.UNKNOWN, blank.state);
        assertEquals(HyperOsNativeProxyStatus.State.UNKNOWN, absent.state);
    }
}
