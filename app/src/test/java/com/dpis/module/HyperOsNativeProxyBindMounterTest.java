package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

public class HyperOsNativeProxyBindMounterTest {
    @Test
    public void createPlanAllowsMissingTargetMountPointWhenParentExists() throws Exception {
        File moduleDir = Files.createTempDirectory("dpis-module-native").toFile();
        File targetDir = Files.createTempDirectory("dpis-target-native").toFile();
        assertTrue(new File(moduleDir, "libdpis_native.so").createNewFile());

        HyperOsNativeProxyBindMounter.MountPlan valid =
                HyperOsNativeProxyBindMounter.createPlan(
                        moduleDir.getAbsolutePath(), targetDir.getAbsolutePath());

        assertTrue(valid.valid);
        assertEquals(new File(moduleDir, "libdpis_native.so").getAbsolutePath(), valid.sourcePath);
        assertEquals(new File(targetDir, "libdpis_native.so").getAbsolutePath(), valid.targetPath);
    }

    @Test
    public void createPlanRejectsMissingTargetParentDirectory() throws Exception {
        File moduleDir = Files.createTempDirectory("dpis-module-native").toFile();
        File targetParent = new File(Files.createTempDirectory("dpis-target-parent").toFile(), "missing");
        assertTrue(new File(moduleDir, "libdpis_native.so").createNewFile());

        HyperOsNativeProxyBindMounter.MountPlan plan =
                HyperOsNativeProxyBindMounter.createPlan(
                        moduleDir.getAbsolutePath(), targetParent.getAbsolutePath());

        assertFalse(plan.valid);
        assertTrue(plan.reason.contains("target native library directory missing"));
    }

    @Test
    public void applyCommandCopiesProxyAndPrintsHashes() {
        String command = HyperOsNativeProxyBindMounter.buildApplyCommand(
                "/data/app/module/lib/arm64/libdpis_native.so",
                "/data/app/MIUIGallery/lib/arm64/libdpis_native.so");

        assertTrue(command.contains("umount -l '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("test ! -s '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"
                + " || cmp -s '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("cp -f '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("cat '/data/app/module/lib/arm64/libdpis_native.so'"
                + " > '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("chmod 755 '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("md5sum '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertFalse(command.contains("mount -o bind"));
    }

    @Test
    public void unmountCommandRemovesCopiedProxy() {
        String command = HyperOsNativeProxyBindMounter.buildUnmountCommand(
                "/data/app/module/lib/arm64/libdpis_native.so",
                "/data/app/MIUIGallery/lib/arm64/libdpis_native.so");

        assertTrue(command.contains("umount -l '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("cmp -s '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("rm -f '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("test ! -e '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
    }

    @Test
    public void applyCommandQuotesSingleQuotes() {
        String command = HyperOsNativeProxyBindMounter.buildApplyCommand(
                "/data/app/module's/libdpis_native.so",
                "/data/app/target/libdpis_native.so");

        assertTrue(command.contains("'/data/app/module'\''s/libdpis_native.so'"));
    }
}
