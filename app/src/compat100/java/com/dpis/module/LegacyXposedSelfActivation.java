package com.dpis.module;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class LegacyXposedSelfActivation {
    private static final String APPLICATION_CLASS_NAME = "com.dpis.module.DpisApplication";
    private static final String MARK_METHOD_NAME = "markXposedSelfLoaded";
    private static final String LEGACY_CONSTRUCTOR_MARKER_FIELD_NAME =
            "xposedSelfLoadedByLegacyConstructorHook";
    private static final AtomicBoolean CONSTRUCTOR_HOOK_INSTALLED = new AtomicBoolean(false);

    private LegacyXposedSelfActivation() {
    }

    static void markIfSelfPackage(String packageName, ClassLoader classLoader, String source) {
        if (!BuildConfig.APPLICATION_ID.equals(packageName) || classLoader == null) {
            return;
        }
        if (XposedSelfActivation.markIfSelfPackage(packageName, classLoader, source)) {
            XposedBridge.log("DPIS legacy xposed self activation marked: source=" + source);
        }
        installConstructorMarker(classLoader, source);
    }

    static void markIfSelfPackageForTest(String packageName, ClassLoader classLoader,
            String source) {
        markIfSelfPackage(packageName, classLoader, source);
    }

    private static void installConstructorMarker(ClassLoader classLoader, String source) {
        if (!CONSTRUCTOR_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> applicationClass = classLoader.loadClass(APPLICATION_CLASS_NAME);
            XposedBridge.hookAllConstructors(applicationClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Field marker = applicationClass.getDeclaredField(
                            LEGACY_CONSTRUCTOR_MARKER_FIELD_NAME);
                    marker.setAccessible(true);
                    marker.setBoolean(param.thisObject, true);
                    Method mark = applicationClass.getDeclaredMethod(MARK_METHOD_NAME);
                    mark.setAccessible(true);
                    mark.invoke(null);
                    XposedBridge.log("DPIS legacy xposed self activation constructor marked: "
                            + "source=" + source + ", appClass=" + applicationClass.getName());
                    DpisLog.i("legacy xposed self activation constructor marked: source="
                            + source);
                }
            });
            XposedBridge.log("DPIS legacy xposed self activation constructor hook ready: source="
                    + source + ", appClass=" + applicationClass.getName());
            DpisLog.i("legacy xposed self activation constructor hook ready: source=" + source);
        } catch (Throwable throwable) {
            CONSTRUCTOR_HOOK_INSTALLED.set(false);
            XposedBridge.log("DPIS legacy xposed self activation constructor hook failed: source="
                    + source + ", error=" + throwable);
            DpisLog.e("legacy xposed self activation constructor hook failed: source="
                    + source, throwable);
        }
    }
}
