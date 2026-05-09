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
    public void applyCommandBindMountsProxyAndPrintsHashes() {
        String command = HyperOsNativeProxyBindMounter.buildApplyCommand(
                "/data/app/module/lib/arm64/libdpis_native.so",
                "/data/app/MIUIGallery/lib/arm64/libdpis_native.so");

        assertTrue(command.contains("umount -l '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("if ! test -e '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'; then"));
        assertTrue(command.contains("touch '/data/app/MIUIGallery/lib/arm64/libdpis_native.so' || exit 1"));
        assertTrue(command.contains("chown system:system '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("mount -o bind '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so' 2>/dev/null"));
        assertTrue(command.contains("if ! cmp -s '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'; then"));
        assertTrue(command.contains("echo dpis_proxy_apply=copy"));
        assertTrue(command.contains("cp -f '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("cat '/data/app/module/lib/arm64/libdpis_native.so'"
                + " > '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("else echo dpis_proxy_apply=bind"));
        assertTrue(command.contains("chmod 755 '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertTrue(command.contains("cmp -s '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"
                + " || exit 1"));
        assertTrue(command.contains("md5sum '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so'"));
        assertFalse(command.contains("md5sum '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIGallery/lib/arm64/libdpis_native.so' || exit 1"));
    }

    @Test
    public void applyCommandFallsBackToCopyWhenBindFails() {
        String command = HyperOsNativeProxyBindMounter.buildApplyCommand(
                "/data/app/module/lib/arm64/libdpis_native.so",
                "/data/app/MIUIWeather/lib/arm64/libdpis_native.so");

        assertTrue(command.contains("mount -o bind '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIWeather/lib/arm64/libdpis_native.so' 2>/dev/null"));
        assertTrue(command.contains("cp -f '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIWeather/lib/arm64/libdpis_native.so'"));
        assertTrue(command.indexOf("touch '/data/app/MIUIWeather/lib/arm64/libdpis_native.so'")
                < command.indexOf("mount -o bind '/data/app/module/lib/arm64/libdpis_native.so'"));
        assertTrue(command.indexOf("chown system:system '/data/app/MIUIWeather/lib/arm64/libdpis_native.so'")
                < command.indexOf("mount -o bind '/data/app/module/lib/arm64/libdpis_native.so'"));
        assertTrue(command.indexOf("mount -o bind '/data/app/module/lib/arm64/libdpis_native.so'")
                < command.indexOf("if ! cmp -s '/data/app/module/lib/arm64/libdpis_native.so'"));
        assertTrue(command.indexOf("if ! cmp -s '/data/app/module/lib/arm64/libdpis_native.so'")
                < command.indexOf("cp -f '/data/app/module/lib/arm64/libdpis_native.so'"));
        assertTrue(command.indexOf("cp -f '/data/app/module/lib/arm64/libdpis_native.so'")
                < command.lastIndexOf("cmp -s '/data/app/module/lib/arm64/libdpis_native.so'"));
        assertTrue(command.lastIndexOf("chcon u:object_r:apk_data_file:s0"
                + " '/data/app/MIUIWeather/lib/arm64/libdpis_native.so'")
                < command.indexOf("else echo dpis_proxy_apply=bind"));
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
    public void unmountCommandVerifiesProxyBeforeRemovingTarget() {
        String command = HyperOsNativeProxyBindMounter.buildUnmountCommand(
                "/data/app/module/lib/arm64/libdpis_native.so",
                "/data/app/MIUIWeather/lib/arm64/libdpis_native.so");

        int compareIndex = command.indexOf("cmp -s '/data/app/module/lib/arm64/libdpis_native.so'"
                + " '/data/app/MIUIWeather/lib/arm64/libdpis_native.so'"
                + " || exit 1");
        int removeIndex = command.indexOf("rm -f '/data/app/MIUIWeather/lib/arm64/libdpis_native.so'");

        assertTrue(compareIndex >= 0);
        assertTrue(removeIndex >= 0);
        assertTrue(compareIndex < removeIndex);
    }

    @Test
    public void applyCommandQuotesSingleQuotes() {
        String command = HyperOsNativeProxyBindMounter.buildApplyCommand(
                "/data/app/module's/libdpis_native.so",
                "/data/app/target/libdpis_native.so");

        assertTrue(command.contains("'/data/app/module'\''s/libdpis_native.so'"));
    }
}
