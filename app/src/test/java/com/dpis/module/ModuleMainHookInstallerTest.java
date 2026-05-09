package com.dpis.module;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class ModuleMainHookInstallerTest {
    @Test
    public void moduleMainUsesExplicitSystemServerPolicyGuard() throws IOException {
        String source = read("src/main/java/com/dpis/module/ModuleMain.java");

        assertTrue(source.contains("SystemServerMutationPolicy.shouldInstallSystemServerHooks("));
    }

    @Test
    public void moduleMainConfiguresHyperOsFlutterNativeFontHook() throws IOException {
        String moduleMain = read("src/main/java/com/dpis/module/ModuleMain.java");
        String nativeInit = read("src/main/resources/META-INF/xposed/native_init.list");
        String build = read("build.gradle.kts");

        assertTrue(moduleMain.contains("HyperOsFlutterFontHookInstaller.install("));
        assertTrue(read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java")
                .contains("HyperOsRustProcessHookInstaller.install("));
        assertTrue(nativeInit.contains("libdpis_native.so"));
        assertTrue(build.contains("externalNativeBuild"));
    }

    @Test
    public void nativeFontHookUsesRustEnvironmentAsRuntimeFontSource() throws IOException {
        String nativeSource = read("src/main/cpp/dpis_native.cpp");

        assertTrue(nativeSource.contains("DPIS_FONT_SCALE_PERCENT"));
        assertTrue(nativeSource.contains("std::getenv"));
        assertTrue(nativeSource.contains("read_proc_cmdline_value"));
        assertTrue(nativeSource.contains("value == \"false\" || value == \"disabled\""));
        assertTrue(nativeSource.contains("return g_enabled.load(std::memory_order_relaxed)"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
