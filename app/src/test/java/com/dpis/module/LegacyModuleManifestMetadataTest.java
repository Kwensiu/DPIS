package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
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
        String scopeList = readProjectFile("src/modern/resources/META-INF/xposed/scope.list");
        assertTrue(scopeList.contains("system"));
    }

    @Test
    public void modernXposedMetadataLivesInModernFlavor() throws IOException {
        String moduleProp = readProjectFile(
                "src/modern/resources/META-INF/xposed/module.prop");
        String javaInit = readProjectFile(
                "src/modern/resources/META-INF/xposed/java_init.list");

        assertTrue(moduleProp.contains("minApiVersion=102"));
        assertTrue(moduleProp.contains("targetApiVersion=102"));
        assertTrue(moduleProp.contains("autoHotReload=true"));
        assertTrue(javaInit.contains("com.dpis.module.ModuleMain"));
        assertFalse(SourceSmokeTestPaths.exists("src", "main", "resources", "META-INF", "xposed", "module.prop"));
    }

    @Test
    public void legacyUsesLegacyXposedInitEntryPoint() throws IOException {
        String xposedInit = readProjectFile("src/legacy/assets/xposed_init");

        assertTrue(xposedInit.contains("com.dpis.module.LegacyModuleHook"));
        assertFalse(SourceSmokeTestPaths.exists("src", "legacy", "resources", "META-INF", "xposed", "java_init.list"));
        assertFalse(SourceSmokeTestPaths.exists("src", "legacy", "resources", "META-INF", "xposed", "module.prop"));
    }

    @Test
    public void legacyIntentionallySharesNativeInitAssetForNativeProxySupport()
            throws IOException {
        String nativeInit = readProjectFile("src/legacy/assets/native_init");
        String buildScript = readProjectFile("build.gradle.kts");

        assertTrue(nativeInit.contains("libdpis_native.so"));
        assertFalse(SourceSmokeTestPaths.exists("src", "main", "assets", "native_init"));
        assertTrue(buildScript.contains("selector().all()"));
        assertTrue(buildScript.contains("sync${capitalizedName}NativeProxyAsset"));
        assertTrue(buildScript.contains("variant.sources.assets?.addGeneratedSourceDirectory"));
    }

    @Test
    public void modernDoesNotDeclareNativeInitListForScopedApps() {
        assertFalse(SourceSmokeTestPaths.exists("src", "modern", "resources", "META-INF", "xposed", "native_init.list"));
    }

    @Test
    public void legacyBuildDoesNotInjectLibxposedApiStubs() throws IOException {
        String buildScript = readProjectFile("build.gradle.kts");

        assertFalse(SourceSmokeTestPaths.exists("src", "legacyApi100Entry"));
        assertFalse(buildScript.contains("generated.copyRecursively(target, overwrite = true)"));
        assertFalse(buildScript.contains("generated.resolve(\"LegacyModuleMain.class\").copyTo"));
        assertFalse(buildScript.contains("dexBuilderLegacyDebug"));
        assertFalse(buildScript.contains("compileLegacyApi100Entry"));
    }

    @Test
    public void legacyLegacyHookUsesClassicXposedHookSurface() throws IOException {
        String source = readProjectFile(
                "src/legacy/java/com/dpis/module/LegacyModuleHook.java");

        assertTrue(source.contains("implements IXposedHookLoadPackage"));
        assertTrue(source.contains("createForLegacyHost(packageName)"));
        assertTrue(source.contains("shouldInstallLegacyHooks()"));
        assertFalse(source.contains("shouldInstallEarlyViewportHooks("));
        assertTrue(source.contains("XposedBridge.hookMethod("));
        assertTrue(source.contains("ResourcesImplHookInstaller.applyDensityOverride("));
        assertTrue(source.contains("LegacyResourcesManager"));
        assertTrue(source.contains("updateResourcesForActivity"));
        assertTrue(source.contains("installResourceCreationHooks("));
        assertFalse(source.contains("package config pending, installing app hooks"));
    }

    @Test
    public void legacyDoesNotExposeConfigProvider() throws IOException {
        String factory = readProjectFile("src/main/java/com/dpis/module/ConfigStoreFactory.java");

        assertFalse(SourceSmokeTestPaths.exists("src", "legacy", "AndroidManifest.xml"));
        assertFalse(SourceSmokeTestPaths.exists("src", "main", "java", "com", "dpis", "module", "CompatConfigProvider.java"));
        assertFalse(factory.contains("CompatConfigProviderPreferences"));
        String compatFactory = factory.substring(factory.indexOf("createForLegacyHost"));
        int propertyIndex = compatFactory.indexOf("RuntimePropertyConfigPreferences");
        int xSharedIndex = compatFactory.indexOf("XSharedPreferencesAdapter");
        assertTrue(propertyIndex >= 0);
        assertTrue(xSharedIndex >= 0);
        assertTrue(factory.contains("AutoViewportRuntimeRoute.ANY_ENABLED_TARGET"));
        assertTrue(compatFactory.contains("new DpiConfigStore("));
    }

    @Test
    public void buildKeepsFilteredCompatibilityTestTaskAndFlavorAggregate()
            throws IOException {
        String buildScript = readProjectFile("build.gradle.kts");

        assertTrue(buildScript.contains("tasks.register<Test>(\"testDebugUnitTest\")"));
        assertTrue(buildScript.contains("tasks.named<Test>(\"testModernDebugUnitTest\")"));
        assertTrue(buildScript.contains("tasks.register(\"testAllDebugUnitTests\")"));
        assertTrue(buildScript.contains("dependsOn(\"testModernDebugUnitTest\", \"testLegacyDebugUnitTest\")"));
        assertTrue(buildScript.contains("outputs/apk/modern/release/app-modern-release.apk"));
        assertFalse(buildScript.contains("outputs/apk/release/app-release.apk"));
    }

    @Test
    public void releaseWorkflowUsesFlavorAwareModernApkPath() throws IOException {
        String workflow = readProjectRootFile(".github/workflows/release.yml");

        assertTrue(workflow.contains("app/build/outputs/apk/modern/release/${APK_NAME}"));
        assertTrue(workflow.contains(
                "app/build/outputs/apk/modern/release/app-modern-release.apk"));
        assertFalse(workflow.contains("app/build/outputs/apk/release/${APK_NAME}"));
        assertFalse(workflow.contains("app/build/outputs/apk/release/app-release.apk"));
    }

    @Test
    public void ciWorkflowUsesFlavorAwareDebugBuildTestAndLintTasks()
            throws IOException {
        String workflow = readProjectRootFile(".github/workflows/ci.yml");

        assertTrue(workflow.contains(":app:assembleModernDebug"));
        assertTrue(workflow.contains(":app:assembleLegacyDebug"));
        assertTrue(workflow.contains(":app:testAllDebugUnitTests"));
        assertTrue(workflow.contains(":app:testDebugUnitTest --tests"));
        assertTrue(workflow.contains(":app:lintModernDebug"));
        assertFalse(workflow.contains(":app:assembleDebug"));
        assertFalse(workflow.contains(":app:lintDebug"));
        assertTrue(workflow.contains("app/build/reports/lint-results-modernDebug.html"));
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
        assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(refreshedStore)"));
    }

    private static String readProjectFile(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static String readProjectRootFile(String relativePath) throws IOException {
        return SourceSmokeTestPaths.readRepositoryRoot(relativePath);
    }
}
