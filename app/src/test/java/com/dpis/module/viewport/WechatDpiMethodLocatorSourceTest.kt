package com.dpis.module

import com.dpis.module.quirks.WechatDpiMethodLocator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WechatDpiMethodLocatorSourceTest {
    @Test
    fun locatorUsesStaticRouteBeforeDexKitForKnownVersions() {
        val source =
            SourceSmokeTestPaths.read("src/main/java/com/dpis/module/quirks/WechatDpiMethodLocator.kt")
        assertTrue(source.contains("WechatDpiRoutes.forVersionCode(versionCode)"))
        assertTrue(source.contains("Class.forName(route.className, false, classLoader)"))
        assertTrue(source.contains("staticRouteMethods(densityManagerClass, route)"))
        assertTrue(source.contains("route.hasDensityMethodTargets()"))
        assertTrue(source.contains("route.densityMethodTargets"))
        assertTrue(source.contains("matchesStaticMethodTarget("))
        assertTrue(source.contains("DexKitBridge.create(sourceDir)"))
        assertTrue(source.contains("FindMethod.create()"))
        assertTrue(source.contains("usingEqStrings("))
        assertTrue(source.contains("\"MicroMsg.MMDensityManager\""))
        assertTrue(source.contains("\"screenResolution_target_field\""))
        assertTrue(source.contains(".modifiers(Modifier.PUBLIC, MatchType.Contains)"))
        assertTrue(source.contains(".returnType(DisplayMetrics::class.java)"))
        assertTrue(source.contains(".paramCount(0)"))
        assertTrue(source.contains(".addInvoke(MethodMatcher.create()"))
        assertTrue(source.contains(".returnType(\"boolean\")"))
        assertTrue(source.contains("data.getMethodInstance(classLoader)"))
        assertTrue(source.contains("densityManagerMethods(method.declaringClass)"))
        assertTrue(source.contains("fun densityManagerMethods("))
        assertTrue(source.contains("isDisplayMetricsGetter(method)"))
        assertTrue(source.contains("loadDexKitLibrary()"))
        assertTrue(source.contains("System.load(path)"))
        assertTrue(source.contains("\"libdexkit.so\""))
        assertTrue(
            source.indexOf("locateByStaticRoute(classLoader, versionCode)") < source.indexOf(
                "locateByDexKit(classLoader, applicationInfo)"
            )
        )
    }

    @Test
    fun locatorKeepsWechatDisplayMetricsRouteOnly() {
        val source =
            SourceSmokeTestPaths.read("src/main/java/com/dpis/module/quirks/WechatDpiMethodLocator.kt")
        val routes =
            SourceSmokeTestPaths.read("src/main/java/com/dpis/module/quirks/WechatDpiRoutes.kt")
        assertFalse(source.contains("resourcesClassName"))
        assertFalse(source.contains("TabIconView"))
        assertFalse(source.contains("installDpiGetterHook("))
        assertFalse(source.contains("installDpiSetterHook("))
        assertFalse(source.contains("WechatDpiRouteMode"))
        assertFalse(source.contains("LOADED_CLASS(\"loaded-class\")"))
        assertFalse(routes.contains("BuildConfig.DEBUG"))
        assertTrue(routes.contains("routeWithBottomTabIconScale("))
        assertTrue(routes.contains("\"j65.f\""))
        assertTrue(routes.contains("MethodTarget.displayMetricsGetter(\"d\")"))
        assertTrue(routes.contains("MethodTarget.displayMetricsGetter(\"e\")"))
        assertTrue(routes.contains("Historical 8.0.74 route shape kept for reference only"))
    }

    @Test
    fun parsesLsposedModuleApkPathForNativeLibraryFallback() {
        val text =
            "LspModuleClassLoader[module=/data/app/~~abc==/io.github.kwensiu.dpis-def==/base.apk, e1[DexPathList[]]]"
        assertEquals(
            "/data/app/~~abc==/io.github.kwensiu.dpis-def==/base.apk",
            WechatDpiMethodLocator.parseModuleApkPathForTest(text)
        )
        assertNull(WechatDpiMethodLocator.parseModuleApkPathForTest("PathClassLoader[]"))
        assertNull(WechatDpiMethodLocator.parseModuleApkPathForTest(null))
    }

    @Test
    fun mapsAndroidAbiToInstalledNativeDirectoryNames() {
        assertArrayEquals(
            arrayOf("arm64", "arm64-v8a"),
            WechatDpiMethodLocator.nativeDirectoryNamesForAbi("arm64-v8a")
        )
        assertArrayEquals(
            arrayOf("arm", "armeabi-v7a"),
            WechatDpiMethodLocator.nativeDirectoryNamesForAbi("armeabi-v7a")
        )
        assertArrayEquals(
            arrayOf("x86_64"),
            WechatDpiMethodLocator.nativeDirectoryNamesForAbi("x86_64")
        )
    }
}
