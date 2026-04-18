package com.dpis.module;

import android.graphics.Paint;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class PaintTextSizeFallbackHookInstaller {
    private static final String FONT_LOG_KEY_PREFIX = "font";
    private static volatile boolean hookInstalled;
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Map<Paint, Float> BASE_TEXT_SIZES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> CALLER_SAMPLE_COUNTS = new ConcurrentHashMap<>();
    private static final float SIZE_EPSILON_PX = 0.5f;
    private static final int MAX_SAMPLES_PER_CALLER = 2;
    private static final int MAX_STACK_FRAMES = 6;

    private PaintTextSizeFallbackHookInstaller() {
    }

    static void install(XposedInterface xposed, String packageName, DpiConfigStore store)
            throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (PaintTextSizeFallbackHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            final Integer targetPercent = store != null
                    ? store.getTargetFontScalePercent(packageName)
                    : null;
            final float factor = resolveFieldRewriteFactor(store, packageName);
            if (!isTargetPercentActive(targetPercent)) {
                return;
            }
            if (!isScaleFactorActive(factor)) {
                return;
            }
            Method paintSetTextSize = Paint.class.getDeclaredMethod("setTextSize", float.class);
            xposed.hook(paintSetTextSize)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return chain.proceed();
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof Paint paint)) {
                            return chain.proceed();
                        }
                        float incoming = (Float) chain.getArg(0);
                        float adjusted = resolveScaledPaintSize(incoming, factor, paint);
                        Object result = chain.proceed();
                        if (Math.abs(adjusted - incoming) < SIZE_EPSILON_PX) {
                            return result;
                        }
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            paint.setTextSize(adjusted);
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                        }
                        logIfChanged(buildFontLogKey(packageName, "paint-fallback"),
                                "DPIS_FONT Paint fallback override: in=" + incoming
                                        + ", out=" + adjusted
                                        + ", factor=" + factor
                                        + ", percent=" + targetPercent);
                        logCallerSample(packageName);
                        return result;
                    });
            hookInstalled = true;
            DpisLog.i("Paint text size fallback hook ready");
        }
    }

    static float resolveFieldRewriteFactor(DpiConfigStore store, String packageName) {
        if (store == null) {
            return 1.0f;
        }
        String mode = store.getTargetFontApplyMode(packageName);
        if (!FontApplyMode.FIELD_REWRITE.equals(mode)) {
            return 1.0f;
        }
        Integer percent = store.getTargetFontScalePercent(packageName);
        if (percent == null || percent <= 0 || percent == 100) {
            return 1.0f;
        }
        return percent / 100.0f;
    }

    private static float resolveScaledPaintSize(float incoming, float factor, Paint paint) {
        if (incoming <= 0f || !isScaleFactorActive(factor)) {
            return incoming;
        }
        Float base = BASE_TEXT_SIZES.get(paint);
        if (base == null || base <= 0f) {
            base = incoming;
            BASE_TEXT_SIZES.put(paint, base);
        }
        float expectedScaled = base * factor;
        if (Math.abs(incoming - expectedScaled) < SIZE_EPSILON_PX) {
            return incoming;
        }
        if (Math.abs(incoming - base) >= SIZE_EPSILON_PX) {
            base = incoming;
            BASE_TEXT_SIZES.put(paint, base);
            expectedScaled = base * factor;
        }
        return expectedScaled;
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }

    private static String buildFontLogKey(String packageName, String suffix) {
        String pkg = packageName == null ? "unknown" : packageName;
        return pkg + ":" + FONT_LOG_KEY_PREFIX + ":" + suffix;
    }

    private static void logCallerSample(String packageName) {
        if (!DpisLog.isLoggingEnabled()) {
            return;
        }
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        String stackSummary = summarizeStack(trace);
        if (stackSummary == null || stackSummary.isEmpty()) {
            return;
        }
        String callerKey = stackSummary;
        int count = CALLER_SAMPLE_COUNTS.getOrDefault(callerKey, 0);
        if (count >= MAX_SAMPLES_PER_CALLER) {
            return;
        }
        CALLER_SAMPLE_COUNTS.put(callerKey, count + 1);
        DpisLog.i("DPIS_FONT Paint fallback caller(" + packageName + "): " + stackSummary);
    }

    private static String summarizeStack(StackTraceElement[] trace) {
        if (trace == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        int added = 0;
        for (StackTraceElement element : trace) {
            if (element == null) {
                continue;
            }
            String className = element.getClassName();
            if (className == null) {
                continue;
            }
            if (className.startsWith("java.lang.Thread")
                    || className.startsWith("de.robv.android.xposed")
                    || className.startsWith("io.github.libxposed")
                    || className.startsWith("com.dpis.module.PaintTextSizeFallbackHookInstaller")) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" <- ");
            }
            builder.append(className)
                    .append("#")
                    .append(element.getMethodName())
                    .append(":")
                    .append(element.getLineNumber());
            added++;
            if (added >= MAX_STACK_FRAMES) {
                break;
            }
        }
        return builder.toString();
    }

    private static boolean isTargetPercentActive(Integer targetPercent) {
        return targetPercent != null && targetPercent > 0 && targetPercent != 100;
    }

    private static boolean isScaleFactorActive(float factor) {
        return factor > 0f && factor != 1.0f;
    }
}
