package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LegacyModuleManifestMetadataTest {
    @Test
    @Throws(IOException::class)
    fun legacyManifestAndScopeDeclareClassicXposedContract() {
        val manifest = read("src/legacy/AndroidManifest.xml")
        val scope = read("src/main/res/values/arrays.xml")
        assertTrue(manifest.contains("android:name=\"xposedmodule\""))
        assertTrue(manifest.contains("android:name=\"xposeddescription\""))
        assertTrue(manifest.contains("android:name=\"xposedminversion\""))
        assertTrue(manifest.contains("android:name=\"xposedsharedprefs\""))
        assertTrue(manifest.contains("android:name=\"xposedscope\""))
        assertTrue(scope.contains("<item>android</item>"))
        assertFalse(scope.contains("<item>system</item>"))
    }

    @Test
    @Throws(IOException::class)
    fun modernFlavorOwnsLibxposedMetadata() {
        val manifest = read("src/main/AndroidManifest.xml")
        val moduleProp = read("src/modern/resources/META-INF/xposed/module.prop")
        val init = read("src/modern/resources/META-INF/xposed/java_init.list")
        val scopeList = read("src/modern/resources/META-INF/xposed/scope.list")
        assertFalse(manifest.contains("android:name=\"xposedminversion\""))
        assertFalse(manifest.contains("android:name=\"xposedsharedprefs\""))
        assertTrue(moduleProp.contains("minApiVersion=101"))
        assertTrue(moduleProp.contains("targetApiVersion=102"))
        assertTrue(init.contains("com.dpis.module.ModuleMain"))
        assertTrue(scopeList.contains("system"))
    }

    @Test
    @Throws(IOException::class)
    fun legacyFlavorUsesClassicEntryPointAndNativeProxyAsset() {
        val init = read("src/legacy/assets/xposed_init")
        val nativeInit = read("src/legacy/assets/native_init")
        assertTrue(init.contains("com.dpis.module.LegacyModuleHook"))
        assertTrue(nativeInit.contains("libdpis_native.so"))
        assertFalse(SourceSmokeTestPaths.exists("src", "main", "assets", "native_init"))
        assertFalse(
            SourceSmokeTestPaths.exists(
                "src",
                "modern",
                "resources",
                "META-INF",
                "xposed",
                "native_init.list"
            )
        )
        assertFalse(
            SourceSmokeTestPaths.exists(
                "src",
                "legacy",
                "resources",
                "META-INF",
                "xposed",
                "module.prop"
            )
        )
    }

    @Test
    @Throws(IOException::class)
    fun launcherAndModuleSettingsRemainExposed() {
        val manifest = read("src/main/AndroidManifest.xml")
        assertTrue(manifest.contains("android:name=\".MainActivityLauncher\""))
        assertTrue(manifest.contains("android:targetActivity=\".MainActivity\""))
        assertTrue(manifest.contains("de.robv.android.xposed.category.MODULE_SETTINGS"))
        assertTrue(manifest.contains("android.permission.QUERY_ALL_PACKAGES"))
    }

    @Test
    @Throws(IOException::class)
    fun buildAndCiUseFlavorAwareTasks() {
        val build = read("build.gradle.kts")
        val ci = readRoot(".github/workflows/ci.yml")
        val release = readRoot(".github/workflows/release.yml")
        assertTrue(build.contains("testAllDebugUnitTests"))
        assertTrue(build.contains("testModernDebugUnitTest"))
        assertTrue(build.contains("isMinifyEnabled = true"))
        assertTrue(build.contains("getDefaultProguardFile(\"proguard-android-optimize.txt\")"))
        assertFalse(build.contains("compileLegacyApi100Entry"))
        assertTrue(ci.contains(":app:assembleModernDebug"))
        assertTrue(ci.contains(":app:assembleLegacyDebug"))
        assertFalse(ci.contains(":app:assembleDebug"))
        assertTrue(release.contains("app/build/outputs/apk/modern/release/\${APK_NAME}"))
        assertFalse(release.contains("app/build/outputs/apk/release/\${APK_NAME}"))
    }

    @Test
    @Throws(IOException::class)
    fun bootFlowResynchronizesConfiguredTargets() {
        val manifest = read("src/main/AndroidManifest.xml")
        val app = read("src/main/java/com/dpis/module/DpisApplication.kt")
        val receiver =
            read("src/main/java/com/dpis/module/runtime/lifecycle/DpisPackageLifecycleReceiver.java")
        assertTrue(manifest.contains("android.permission.RECEIVE_BOOT_COMPLETED"))
        assertTrue(manifest.contains("android.intent.action.BOOT_COMPLETED"))
        assertTrue(receiver.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(store)"))
        assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore)"))
        assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(refreshedStore)"))
    }

    private fun read(path: String): String = SourceSmokeTestPaths.read(path)

    private fun readRoot(path: String): String = SourceSmokeTestPaths.readRepositoryRoot(path)
}
