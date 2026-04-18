package com.dpis.module;

import android.util.TypedValue;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class ForceTextSizeHookInstaller {
    private static volatile boolean hookInstalled;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ForceTextSizeHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName, DpiConfigStore store)
            throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (ForceTextSizeHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            FontScaleOverride.Result fontScale = FontScaleOverride.resolve(store, packageName, 1.0f);
            final Integer targetPercent = fontScale.targetPercent;
            final float factor = fontScale.effective;
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> textViewClass = Class.forName("android.widget.TextView", false, bootClassLoader);
            Method setTextSizeMethod = textViewClass.getDeclaredMethod("setTextSize", int.class, float.class);
            xposed.hook(setTextSizeMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        if (targetPercent == null || targetPercent <= 0 || targetPercent == 100) {
                            return result;
                        }
                        int unit = (Integer) chain.getArg(0);
                        float size = (Float) chain.getArg(1);
                        if (size <= 0f) {
                            return result;
                        }
                        if (!FontScaleOverride.shouldForceTextUnit(unit)) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof TextView textView)) {
                            return result;
                        }
                        float originalPx = FontScaleOverride.toPx(
                                unit, size, textView.getResources().getDisplayMetrics());
                        if (originalPx <= 0f) {
                            return result;
                        }
                        float forcedPx = originalPx * factor;
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, forcedPx);
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                        }
                        logIfChanged(packageName + ":text-size-unit-" + unit,
                                "DPIS_FONT ForceTextSize override: unit=" + unit
                                        + ", size=" + size
                                        + ", px=" + originalPx + " -> " + forcedPx
                                        + ", factor=" + factor
                                        + ", percent=" + targetPercent);
                        return result;
                    });
            hookInstalled = true;
            DpisLog.i("ForceTextSize hook ready");
        }
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }
}
