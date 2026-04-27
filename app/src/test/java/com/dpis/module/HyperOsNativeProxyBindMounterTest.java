package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

public class HyperOsNativeProxyBindMounterTest {
    @Test
    public void createPlanRequiresBothProxyFiles() throws Exception {
        File moduleDir = Files.createTempDirectory("dpis-module-native").toFile();
        File targetDir = Files.createTempDirectory("dpis-target-native").toFile();
        assertTrue(new File(moduleDir, "libdpis_native.so").createNewFile());

        HyperOsNativeProxyBindMounter.MountPlan missingTarget =
                HyperOsNativeProxyBindMounter.createPlan(
                        moduleDir.getAbsolutePath(), targetDir.getAbsolutePath());

        assertFalse(missingTarget.valid);

        assertTrue(new File(targetDir, "libdpis_native.so").createNewFile());
        HyperOsNativeProxyBindMounter.MountPlan valid =
                HyperOsNativeProxyBindMounter.createPlan(
                        moduleDir.getAbsolutePath(), targetDir.getAbsolutePath());

        assertTrue(valid.valid);
        assertEquals(new File(moduleDir, "libdpis_native.so").getAbsolutePath(), valid.sourcePath);
        assertEquals(new File(targetDir, "libdpis_native.so").getAbsolutePath(), valid.targetPath);
    }

    @Test
    public void applyCommandBindMountsAndVerifiesHash() {
        String command = HyperOsNativeProxyBindMounter.buildApplyCommand(
                "/data/app/module/lib/arm64/libdpis_native.so",
                "/data/app/MIUIGallery/lib/arm64/libdpis_native.so");

        assertTrue(command.contains("umount -l '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("&& mount -o bind '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("&& mount | grep -F -- '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("&& md5sum '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
    }

    @Test
    public void unmountCommandFailsWhenMountStillExists() {
        String command = HyperOsNativeProxyBindMounter.buildUnmountCommand(
                "/data/app/MIUIGallery/lib/arm64/libdpis_native.so");

        assertTrue(command.contains("umount -l '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("mount | grep -F -- '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("&& exit 1 || exit 0"));
    }

    @Test
    public void applyCommandQuotesSingleQuotes() {
        String command = HyperOsNativeProxyBindMounter.buildApplyCommand(
                "/data/app/module's/libdpis_native.so",
                "/data/app/target/libdpis_native.so");

        assertTrue(command.contains("'/data/app/module'\''s/libdpis_native.so'"));
    }
}
