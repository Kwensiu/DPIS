package com.dpis.module.runtime;

import com.dpis.module.*;

import java.lang.reflect.Method;

public final class XposedSelfActivation {
    private static final String APPLICATION_CLASS_NAME = "com.dpis.module.DpisApplication";
    private static final String MARK_METHOD_NAME = "markXposedSelfLoaded";

    private XposedSelfActivation() {
    }

    public static boolean markIfSelfPackage(String packageName, ClassLoader classLoader, String source) {
        if (!BuildConfig.APPLICATION_ID.equals(packageName) || classLoader == null) {
            return false;
        }
        try {
            Class<?> applicationClass = classLoader.loadClass(APPLICATION_CLASS_NAME);
            Method mark = applicationClass.getDeclaredMethod(MARK_METHOD_NAME);
            mark.setAccessible(true);
            mark.invoke(null);
            DpisLog.i("xposed self activation marked: source=" + source
                    + ", appClass=" + applicationClass.getName());
            return true;
        } catch (Throwable throwable) {
            DpisLog.e("xposed self activation mark failed: source=" + source, throwable);
            return false;
        }
    }
}
