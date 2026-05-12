package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class Compat100LegacyModuleHookSourceTest {
    @Test
    public void legacyEntryInstallsReplacementHooks() throws Exception {
        String source = read("src/compat100/java/com/dpis/module/Compat100LegacyModuleHook.java");

        assertTrue(source.contains("installDisplayHooks(packageName)"));
        assertTrue(source.contains("installWindowMetricsHook()"));
        assertTrue(source.contains("installFontFieldRewriteHooks(packageName, store)"));
        assertTrue(source.contains("DisplayHookInstaller.applyDisplayMetrics"));
        assertTrue(source.contains("DisplayHookInstaller.applyPoint"));
        assertTrue(source.contains("DisplayHookInstaller.applyDisplayInfo"));
        assertTrue(source.contains("DisplayHookInstaller.setTargetPackageNameForCompat100(packageName)"));
        assertTrue(source.contains("FONT_TEXTVIEW_UPDATE"));
        assertTrue(source.contains("installResourcesReadHooks(packageName, store)"));
        assertTrue(source.contains("ResourcesReadHookInstaller.applyConfigurationOverride"));
        assertTrue(source.contains("ResourcesReadHookInstaller.applyMetricsOverride"));
        assertTrue(source.contains("resolveActivePackageName(packageName)"));
        assertTrue(source.contains("resolveStoreForPackage(activePackage, store)"));
        assertTrue(source.contains("ConfigStoreFactory.createForCompat100Host(packageName)"));
        assertTrue(source.contains("FONT_FIELD_REWRITE_HOOKED.set(false);"));
        assertTrue(source.contains("if (Boolean.TRUE.equals(FONT_TEXTVIEW_UPDATE.get()))"));
        assertTrue(source.contains("Android's one-argument TextView#setTextSize delegates"));
        assertTrue(source.contains("getDeclaredMethod(\"getConfiguration\")"));
        assertTrue(source.contains("getDeclaredMethod(\"getDisplayMetrics\")"));
        assertTrue(source.contains("getDeclaredMethod(\"getSystem\")"));
        assertTrue(source.contains("SystemServerProcess.isSystemServer"));
        assertTrue(source.contains("installSystemServerHooksForCompat100()"));
        assertTrue(source.contains("Compat100SystemServerHookInstaller.install"));
        assertTrue(source.indexOf("DisplayHookInstaller.setTargetPackageNameForCompat100(packageName)")
                < source.indexOf("DISPLAY_HOOKED.compareAndSet(false, true)"));
        assertTrue(source.contains("implements IXposedHookLoadPackage, IXposedHookZygoteInit"));
        assertTrue(source.contains("public void initZygote(StartupParam startupParam)"));
        assertTrue(source.contains("installSystemServerHooksForCompat100();"));

        String systemServerSource = read("src/compat100/java/com/dpis/module/Compat100SystemServerHookInstaller.java");
        assertTrue(systemServerSource.contains("android.app.servertransaction.LaunchActivityItem"));
        assertTrue(systemServerSource.contains("applyLaunchActivityItemArgs"));
        assertTrue(systemServerSource.contains(
                "PerAppDisplayConfigSource.withCompat100RuntimePropertyFallback"));
        assertTrue(systemServerSource.contains("createForCompat100SystemServerHost"));
        assertTrue(systemServerSource.contains("applyLaunchActivityItemArgs(source, param.args)"));
        assertTrue(systemServerSource.contains("PerAppDisplayOverrideCalculator.calculate"));
        assertTrue(systemServerSource.contains("ViewportOverride.apply"));
        assertTrue(systemServerSource.contains("FontApplyMode.SYSTEM_EMULATION"));
        assertTrue(systemServerSource.contains("Compat100RustProcessHookInstaller.install(source)"));

        String compatRustSource = read("src/compat100/java/com/dpis/module/Compat100RustProcessHookInstaller.java");
        assertTrue(compatRustSource.contains("XposedBridge.hookMethod(method"));
        assertTrue(compatRustSource.contains("param.args = updatedArgs"));
        String rustSource = read("src/main/java/com/dpis/module/HyperOsRustProcessHookInstaller.java");
        assertTrue(rustSource.contains("applyEnvironmentArgsForLegacy"));
        assertTrue(rustSource.indexOf("return null;")
                < rustSource.indexOf("Object existingValue = args.get(ARG_ENVIRONMENTS);"));
        assertTrue(!rustSource.contains("HyperOsFlutterFontBridge.clearTarget(packageName);"));
    }

    private static String read(String relativePath) throws Exception {
        Path path = Path.of(relativePath);
        if (!Files.exists(path)) {
            path = Path.of("app", relativePath);
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
