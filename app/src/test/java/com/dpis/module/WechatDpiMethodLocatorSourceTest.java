package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WechatDpiMethodLocatorSourceTest {
    @Test
    public void locatorUsesStaticRouteBeforeDexKitForKnownVersions() throws Exception {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/WechatDpiMethodLocator.java");

        assertTrue(source.contains("WechatDpiRoutes.forVersionCode(versionCode)"));
        assertTrue(source.contains("Class.forName(route.className, false, classLoader)"));
        assertTrue(source.contains("DexKitBridge.create(applicationInfo.sourceDir)"));
        assertTrue(source.contains("FindMethod.create()"));
        assertTrue(source.contains("usingEqStrings("));
        assertTrue(source.contains("\"MicroMsg.MMDensityManager\""));
        assertTrue(source.contains("\"screenResolution_target_field\""));
        assertTrue(source.contains(".modifiers(Modifier.PUBLIC, MatchType.Contains)"));
        assertTrue(source.contains(".returnType(DisplayMetrics.class)"));
        assertTrue(source.contains(".paramCount(0)"));
        assertTrue(source.contains(".addInvoke(MethodMatcher.create()"));
        assertTrue(source.contains(".returnType(\"boolean\")"));
        assertTrue(source.contains("methodData.getMethodInstance(classLoader)"));
        assertTrue(source.contains("densityManagerMethods(method.getDeclaringClass())"));
        assertTrue(source.contains("parameterTypes[0] == Configuration.class"));
        assertTrue(source.contains("parameterTypes[1] == DisplayMetrics.class"));
        assertTrue(source.contains("LOADED_CLASS(\"loaded-class\")"));
        assertTrue(source.contains("static List<Method> densityManagerMethods("));
        assertTrue(source.contains("isTargetFieldGetter(method)"));
        assertTrue(source.contains("isTargetFieldSetter(method)"));
        assertTrue(source.contains("loadDexKitLibrary()"));
        assertTrue(source.contains("System.load(path)"));
        assertTrue(source.contains("\"libdexkit.so\""));
        assertTrue(source.indexOf("locateByStaticRoute(classLoader, versionCode)")
                < source.indexOf("locateByDexKit(classLoader, applicationInfo)"));
    }

    @Test
    public void locatorKeepsWechatDisplayMetricsRouteOnly() throws Exception {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/WechatDpiMethodLocator.java");

        assertFalse(source.contains("resourcesClassName"));
        assertFalse(source.contains("TabIconView"));
        assertFalse(source.contains("installDpiGetterHook("));
        assertFalse(source.contains("installDpiSetterHook("));
    }

    @Test
    public void parsesLsposedModuleApkPathForNativeLibraryFallback() {
        String text = "LspModuleClassLoader[module=/data/app/~~abc==/"
                + "io.github.kwensiu.dpis-def==/base.apk, e1[DexPathList[]]]";

        assertEquals("/data/app/~~abc==/io.github.kwensiu.dpis-def==/base.apk",
                WechatDpiMethodLocator.parseModuleApkPathForTest(text));
        assertNull(WechatDpiMethodLocator.parseModuleApkPathForTest("PathClassLoader[]"));
        assertNull(WechatDpiMethodLocator.parseModuleApkPathForTest(null));
    }

    @Test
    public void mapsAndroidAbiToInstalledNativeDirectoryNames() {
        assertArrayEquals(new String[] {"arm64", "arm64-v8a"},
                WechatDpiMethodLocator.nativeDirectoryNamesForAbi("arm64-v8a"));
        assertArrayEquals(new String[] {"arm", "armeabi-v7a"},
                WechatDpiMethodLocator.nativeDirectoryNamesForAbi("armeabi-v7a"));
        assertArrayEquals(new String[] {"x86_64"},
                WechatDpiMethodLocator.nativeDirectoryNamesForAbi("x86_64"));
    }
}
