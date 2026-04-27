package com.dpis.module;

final class HyperOsFlutterFontHookInstaller {
    private HyperOsFlutterFontHookInstaller() {
    }

    static void install(String packageName, DpiConfigStore store) {
        if (store == null || !store.isHyperOsFlutterFontHookEnabled()) {
            return;
        }
        Integer targetFontScalePercent = store.getTargetFontScalePercent(packageName);
        if (targetFontScalePercent == null || targetFontScalePercent <= 0) {
            return;
        }
        try {
            System.loadLibrary("dpis_native");
            configure(packageName, targetFontScalePercent, true);
            DpisLog.i("DPIS_FONT HyperOS Flutter native hook configured: package="
                    + packageName + ", targetFontScalePercent=" + targetFontScalePercent);
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT HyperOS Flutter native hook configure failed: package="
                    + packageName, throwable);
        }
    }

    private static native void configure(String packageName, int targetFontScalePercent, boolean enabled);
}
