package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public final class LegacyModuleManifestMetadataTest {

    @Test
    public void manifestDeclaresLegacyXposedMetadata() throws IOException {
        String manifest = readProjectFile("src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android:name=\"xposedmodule\""));
        assertTrue(manifest.contains("android:name=\"xposeddescription\""));
        assertTrue(manifest.contains("android:name=\"xposedminversion\""));
        assertTrue(manifest.contains("android:name=\"xposedsharedprefs\""));
        assertTrue(manifest.contains("android:name=\"xposedscope\""));
        assertTrue(manifest.contains("de.robv.android.xposed.category.MODULE_SETTINGS"));
    }

    @Test
    public void scopeResourceDeclaresLegacySystemRecommendation() throws IOException {
        String arrays = readProjectFile("src/main/res/values/arrays.xml");
        assertTrue(arrays.contains("<string-array name=\"xposedscope\">"));
        assertTrue(arrays.contains("<item>android</item>"));
        assertFalse(arrays.contains("<item>system</item>"));
    }

    @Test
    public void modernScopeListIncludesSystemAsRecommendedScope() throws IOException {
        String scopeList = readProjectFile("src/modern101/resources/META-INF/xposed/scope.list");
        assertTrue(scopeList.contains("system"));
    }

    @Test
    public void modernXposedMetadataLivesInModern101Flavor() throws IOException {
        String moduleProp = readProjectFile(
                "src/modern101/resources/META-INF/xposed/module.prop");
        String javaInit = readProjectFile(
                "src/modern101/resources/META-INF/xposed/java_init.list");

        assertTrue(moduleProp.contains("minApiVersion=101"));
        assertTrue(moduleProp.contains("targetApiVersion=101"));
        assertTrue(javaInit.contains("com.dpis.module.ModuleMain"));
        assertFalse(Files.exists(Path.of(
                "src", "main", "resources", "META-INF", "xposed", "module.prop")));
    }

    @Test
    public void compat100UsesLegacyXposedInitEntryPoint() throws IOException {
        String xposedInit = readProjectFile("src/compat100/assets/xposed_init");

        assertTrue(xposedInit.contains("com.dpis.module.Compat100LegacyModuleHook"));
        assertFalse(Files.exists(Path.of(
                "src", "compat100", "resources", "META-INF", "xposed", "java_init.list")));
        assertFalse(Files.exists(Path.of(
                "src", "compat100", "resources", "META-INF", "xposed", "module.prop")));
    }

    @Test
    public void compat100IntentionallySharesNativeInitAssetForNativeProxySupport()
            throws IOException {
        String nativeInit = readProjectFile("src/main/assets/native_init");
        String buildScript = readProjectFile("build.gradle.kts");

        assertTrue(nativeInit.contains("libdpis_native.so"));
        assertTrue(buildScript.contains("selector().all()"));
        assertTrue(buildScript.contains("sync${capitalizedName}NativeProxyAsset"));
        assertTrue(buildScript.contains("variant.sources.assets?.addGeneratedSourceDirectory"));
    }

    @Test
    public void compat100BuildDoesNotInjectLibxposedApiStubs() throws IOException {
        String buildScript = readProjectFile("build.gradle.kts");

        assertFalse(Files.exists(Path.of("src", "compat100Api100Entry")));
        assertFalse(buildScript.contains("generated.copyRecursively(target, overwrite = true)"));
        assertFalse(buildScript.contains("generated.resolve(\"Compat100ModuleMain.class\").copyTo"));
        assertFalse(buildScript.contains("dexBuilderCompat100Debug"));
        assertFalse(buildScript.contains("compileCompat100Api100Entry"));
    }

    @Test
    public void compat100LegacyHookUsesClassicXposedHookSurface() throws IOException {
        String source = readProjectFile(
                "src/compat100/java/com/dpis/module/Compat100LegacyModuleHook.java");

        assertTrue(source.contains("implements IXposedHookLoadPackage"));
        assertTrue(source.contains("createForCompat100Host(packageName)"));
        assertTrue(source.contains("shouldInstallCompat100LegacyHooks()"));
        assertTrue(source.contains("XposedBridge.hookMethod("));
        assertTrue(source.contains("ResourcesImplHookInstaller.applyDensityOverride("));
        assertTrue(source.contains("Compat100LegacyResourcesManager"));
        assertTrue(source.contains("updateResourcesForActivity"));
        assertTrue(source.contains("installResourceCreationHooks("));
        assertFalse(source.contains("package config pending, installing app hooks"));
    }

    @Test
    public void compat100DoesNotExposeConfigProvider() throws IOException {
        String factory = readProjectFile("src/main/java/com/dpis/module/ConfigStoreFactory.java");

        assertFalse(Files.exists(Path.of("src", "compat100", "AndroidManifest.xml")));
        assertFalse(Files.exists(Path.of(
                "src", "main", "java", "com", "dpis", "module", "CompatConfigProvider.java")));
        assertFalse(factory.contains("CompatConfigProviderPreferences"));
        String compatFactory = factory.substring(factory.indexOf("createForCompat100Host"));
        int propertyIndex = compatFactory.indexOf("SystemPropertyConfigPreferences");
        int xSharedIndex = compatFactory.indexOf("XSharedPreferencesAdapter");
        assertTrue(propertyIndex >= 0);
        assertTrue(xSharedIndex >= 0);
        assertTrue(compatFactory.contains("new DpiConfigStore("));
    }

    @Test
    public void buildKeepsFilteredCompatibilityTestTaskAndFlavorAggregate()
            throws IOException {
        String buildScript = readProjectFile("build.gradle.kts");

        assertTrue(buildScript.contains("tasks.register<Test>(\"testDebugUnitTest\")"));
        assertTrue(buildScript.contains("tasks.named<Test>(\"testModern101DebugUnitTest\")"));
        assertTrue(buildScript.contains("tasks.register(\"testAllDebugUnitTests\")"));
        assertTrue(buildScript.contains("dependsOn(\"testModern101DebugUnitTest\", \"testCompat100DebugUnitTest\")"));
        assertTrue(buildScript.contains("outputs/apk/modern101/release/app-modern101-release.apk"));
        assertFalse(buildScript.contains("outputs/apk/release/app-release.apk"));
    }

    @Test
    public void releaseWorkflowUsesFlavorAwareModern101ApkPath() throws IOException {
        String workflow = readProjectRootFile(".github/workflows/release.yml");

        assertTrue(workflow.contains("app/build/outputs/apk/modern101/release/${APK_NAME}"));
        assertTrue(workflow.contains(
                "app/build/outputs/apk/modern101/release/app-modern101-release.apk"));
        assertFalse(workflow.contains("app/build/outputs/apk/release/${APK_NAME}"));
        assertFalse(workflow.contains("app/build/outputs/apk/release/app-release.apk"));
    }

    @Test
    public void readmeDocumentsFlavorAwareDebugBuildAndInstallPaths() throws IOException {
        String readme = readProjectRootFile("README.md");

        assertTrue(readme.contains(":app:assembleModern101Debug :app:assembleCompat100Debug"));
        assertTrue(readme.contains(":app:testAllDebugUnitTests"));
        assertTrue(readme.contains(
                "app/build/outputs/apk/modern101/debug/app-modern101-debug.apk"));
        assertFalse(readme.contains("app/build/outputs/apk/debug/app-debug.apk"));
    }

    @Test
    public void ciWorkflowUsesFlavorAwareDebugBuildTestAndLintTasks()
            throws IOException {
        String workflow = readProjectRootFile(".github/workflows/ci.yml");

        assertTrue(workflow.contains(":app:assembleModern101Debug"));
        assertTrue(workflow.contains(":app:assembleCompat100Debug"));
        assertTrue(workflow.contains(":app:testAllDebugUnitTests"));
        assertTrue(workflow.contains(":app:testDebugUnitTest --tests"));
        assertTrue(workflow.contains(":app:lintModern101Debug"));
        assertFalse(workflow.contains(":app:assembleDebug"));
        assertFalse(workflow.contains(":app:lintDebug"));
        assertTrue(workflow.contains("app/build/reports/lint-results-modern101Debug.html"));
    }

    @Test
    public void manifestUsesLauncherAliasWhileKeepingModuleSettingsEntry() throws IOException {
        String manifest = readProjectFile("src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android:name=\".MainActivityLauncher\""));
        assertTrue(manifest.contains("android:targetActivity=\".MainActivity\""));
        assertTrue(manifest.contains("android.intent.category.LAUNCHER"));
        assertTrue(manifest.contains("de.robv.android.xposed.category.MODULE_SETTINGS"));
    }

    @Test
    public void manifestDeclaresXiaomiInstalledAppsPermission() throws IOException {
        String manifest = readProjectFile("src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android.permission.QUERY_ALL_PACKAGES"));
        assertTrue(manifest.contains("com.android.permission.GET_INSTALLED_APPS"));
    }

    @Test
    public void manifestSyncsCompatPropertiesAfterBoot() throws IOException {
        String manifest = readProjectFile("src/main/AndroidManifest.xml");
        String app = readProjectFile("src/main/java/com/dpis/module/DpisApplication.java");
        String receiver = readProjectFile(
                "src/main/java/com/dpis/module/DpisPackageLifecycleReceiver.java");

        assertTrue(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"));
        assertTrue(manifest.contains("android.intent.action.BOOT_COMPLETED"));
        assertTrue(receiver.contains("Intent.ACTION_BOOT_COMPLETED"));
        assertTrue(receiver.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(store)"));
        assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore)"));
        assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(remoteStore)"));
    }

    private static String readProjectFile(String relativePath) throws IOException {
        Path path = Path.of(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static String readProjectRootFile(String relativePath) throws IOException {
        Path path = Path.of("..", relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
