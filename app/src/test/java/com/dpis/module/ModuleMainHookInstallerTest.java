package com.dpis.module;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModuleMainHookInstallerTest {
    @Test
    public void moduleMainUsesExplicitSystemServerPolicyGuard() throws IOException {
        String source = read("src/modern101/java/com/dpis/module/ModuleMain.java");

        assertTrue(source.contains("SystemServerMutationPolicy.shouldInstallSystemServerHooks("));
        assertTrue(source.contains("ModulePackagePlan.resolve("));
        assertFalse(Files.exists(Path.of(
                "src", "main", "java", "com", "dpis", "module", "ModuleMain.java")));
    }

    @Test
    public void moduleMainConfiguresHyperOsFlutterNativeFontHook() throws IOException {
        String moduleMain = read("src/modern101/java/com/dpis/module/ModuleMain.java");
        String build = read("build.gradle.kts");
        String flutterInstaller = read("src/main/java/com/dpis/module/HyperOsFlutterFontHookInstaller.java");
        String appProcessInstaller = read("src/main/java/com/dpis/module/AppProcessHookInstaller.java");

        assertFalse(moduleMain.contains("HyperOsFlutterFontHookInstaller.install("));
        assertTrue(moduleMain.contains("packagePlan.hyperOsNativeFlutterFontEnabled"));
        assertTrue(appProcessInstaller.contains("fontDomainPlan.hyperOsNativeFlutterEnabled"));
        assertTrue(appProcessInstaller.contains("HyperOsFlutterFontHookInstaller.install(xposed, packageName, store)"));
        assertTrue(moduleMain.contains("maybeInstallAppProcessFromModuleLoaded("));
        assertTrue(moduleMain.contains("installAppProcessHooksIfConfigured("));
        assertTrue(moduleMain.contains("new SystemPropertyConfigPreferences(processName)"));
        assertTrue(moduleMain.contains("module-loaded app hook install enter"));
        assertTrue(moduleMain.contains("module-loaded app hook install failed"));
        assertTrue(moduleMain.contains("rawBridgeLog("));
        assertTrue(moduleMain.contains("module-loaded app config fallback"));
        assertTrue(moduleMain.contains("module-loaded app config unavailable"));
        assertTrue(moduleMain.contains("appProcessInstallAttempted"));
        assertTrue(moduleMain.contains("\"module-loaded\""));
        assertTrue(moduleMain.contains("\"module-loaded-fallback\""));
        assertTrue(moduleMain.contains("\"package-ready\""));
        assertTrue(read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java")
                .contains("HyperOsRustProcessHookInstaller.install("));
        assertFalse(Files.exists(Path.of(
                "src", "modern101", "resources", "META-INF", "xposed", "native_init.list")));
        assertFalse(Files.exists(Path.of("src", "main", "assets", "native_init")));
        assertTrue(flutterInstaller.contains("System.loadLibrary(\"dpis_native\")"));
        assertTrue(build.contains("externalNativeBuild"));
    }

    @Test
    public void moduleMainRetriesFlutterHooksWithAppClassLoaderFromPackageReady() throws IOException {
        String source = read("src/modern101/java/com/dpis/module/ModuleMain.java");

        assertTrue(source.contains("retryFlutterHooksWithAppClassLoader("));
        assertTrue(source.contains("param.getClassLoader()"));
        assertTrue(source.contains("FlutterSettingsFontHookInstaller.retryWithAppClassLoader("));
        assertTrue(source.contains("resolveDebugFontOverrideForPackage(packageName)"));
        assertTrue(source.contains("packagePlan.buildExecutionPlan("));
        assertTrue(source.contains("HookExecutionPlan executionPlan"));
        assertTrue(source.contains("!packagePlan.targetDpisEnabled || !packagePlan.fontScaleActive"));
        assertTrue(source.contains("packagePlan.flutterSettingsFontEnabled"));
        assertTrue(source.contains("packagePlan.hyperOsNativeFlutterFontEnabled"));
    }

    @Test
    public void hyperOsNativeFlutterDoesNotBypassAppProcessInstaller() throws IOException {
        String moduleMain = read("src/modern101/java/com/dpis/module/ModuleMain.java");
        String appProcessInstaller = read("src/main/java/com/dpis/module/AppProcessHookInstaller.java");

        assertFalse(moduleMain.contains("HyperOsFlutterFontHookInstaller.install("));
        assertTrue(moduleMain.contains("AppProcessHookInstaller.install(this, store, policy, packagePlan)"));
        assertTrue(appProcessInstaller.contains("HyperOsFlutterFontHookInstaller.install("));
        assertTrue(appProcessInstaller.contains("resolveFontDomainPlan("));
        assertTrue(appProcessInstaller.contains("hyperOsNativeFlutterEnabled"));
    }

    @Test
    public void systemServerHookDoesNotRetryOriginalAfterProceedThrows() throws IOException {
        String installer = read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java");

        assertTrue(installer.contains("boolean proceedAttempted = false;"));
        assertTrue(installer.contains("proceedAttempted = true;"));
        assertTrue(installer.contains("if (proceedAttempted)"));
        assertTrue(installer.contains("throw throwable;"));
        assertTrue(installer.contains("return chain.proceed();"));
    }

    @Test
    public void issueSpecificDiagnosticsDoNotRemainInRuntimeSources() throws IOException {
        assertFalse(read("src/modern101/java/com/dpis/module/ModuleMain.java")
                .contains("DPIS_DIAG"));
        assertFalse(read("src/main/java/com/dpis/module/ConfigStoreFactory.java")
                .contains("DPIS_DIAG"));
        assertFalse(read("src/main/java/com/dpis/module/SystemServerDisplayEnvironmentInstaller.java")
                .contains("DPIS_DIAG"));
    }

    @Test
    public void temporaryProbesAndPackerReferencesDoNotRemainInRuntimeSources() throws IOException {
        String moduleMain = read("src/modern101/java/com/dpis/module/ModuleMain.java");
        String flutterInstaller = read("src/main/java/com/dpis/module/FlutterSettingsFontHookInstaller.java");

        // Temporary selftest/probe prefixes must not remain
        assertFalse("SELFTEST probe must be removed", moduleMain.contains("DPIS_HOOK_SELFTEST"));
        assertFalse("RUNTIME_PROBE must be removed", moduleMain.contains("DPIS_RUNTIME_PROBE"));
        assertFalse("APPCLASS probe must be removed", moduleMain.contains("DPIS_APPCLASS"));
        assertFalse("SHELL probe must be removed", moduleMain.contains("DPIS_SHELL"));
        assertFalse("CL_CAPABILITY probe must be removed", moduleMain.contains("DPIS_CL_CAPABILITY"));
        assertFalse("module-loaded install path must not keep temporary probe wording",
                moduleMain.contains("module-loaded app hook probe"));

        // Packer-specific class names must not appear in production sources
        assertFalse("shell packer class must not be in ModuleMain",
                moduleMain.contains("s.h.e.l.l"));
        assertFalse("target app package must not be hardcoded in ModuleMain",
                moduleMain.contains("com.mfcloudcalculate.networkdisk"));
        assertFalse("shell packer class must not be in FlutterSettingsFontHookInstaller",
                flutterInstaller.contains("s.h.e.l.l"));
        assertFalse("target app package must not be in FlutterSettingsFontHookInstaller",
                flutterInstaller.contains("com.mfcloudcalculate.networkdisk"));
    }

    @Test
    public void debugBuildKeepsRuntimeHookLogsVisible() throws IOException {
        String source = read("src/main/java/com/dpis/module/DpisLog.java");

        assertTrue(source.contains("BuildConfig.DEBUG || isLoggingEnabled()"));
    }

    @Test
    public void moduleMainUsesPackagePlanHookEligibilityGate() throws IOException {
        String moduleMain = read("src/modern101/java/com/dpis/module/ModuleMain.java");

        assertTrue(moduleMain.contains("if (!packagePlan.shouldInstallHooks())"));
        assertFalse(moduleMain.contains("packagePlan.targetViewportWidthDp == null"
                + System.lineSeparator()
                + "                && !packagePlan.fontScaleActive"));
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
