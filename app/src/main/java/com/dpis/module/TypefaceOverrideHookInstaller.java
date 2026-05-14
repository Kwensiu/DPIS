package com.dpis.module;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class TypefaceOverrideHookInstaller {
    private static final String LOG_PREFIX = "DPIS_FONT_STYLE ";
    private static volatile boolean hookInstalled;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TypefaceOverrideHookInstaller() {
    }

    static void install(XposedInterface xposed,
                        String packageName,
                        DpiConfigStore store,
                        FontLibraryStore fontLibraryStore) throws ReflectiveOperationException {
        if (hookInstalled) {
            return;
        }
        synchronized (TypefaceOverrideHookInstaller.class) {
            if (hookInstalled) {
                return;
            }
            Typeface baseTypeface = loadTargetTypeface(packageName, store, fontLibraryStore);
            if (baseTypeface == null) {
                return;
            }
            ClassLoader bootClassLoader = ClassLoader.getSystemClassLoader();
            Class<?> textViewClass = Class.forName("android.widget.TextView", false, bootClassLoader);
            Method setTypeface = textViewClass.getDeclaredMethod("setTypeface", Typeface.class);
            xposed.hook(setTypeface)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return chain.proceed();
                        }
                        Typeface original = (Typeface) chain.getArg(0);
                        Typeface replacement = resolveReplacement(baseTypeface, original, null);
                        if (replacement == null) {
                            return chain.proceed();
                        }
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof TextView textView) {
                            applyTextViewTypeface(textView, replacement, null);
                        }
                        return result;
                    });

            Method setTypefaceWithStyle =
                    textViewClass.getDeclaredMethod("setTypeface", Typeface.class, int.class);
            xposed.hook(setTypefaceWithStyle)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return chain.proceed();
                        }
                        Typeface original = (Typeface) chain.getArg(0);
                        Integer style = (Integer) chain.getArg(1);
                        Typeface replacement = resolveReplacement(baseTypeface, original, style);
                        if (replacement == null) {
                            return chain.proceed();
                        }
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof TextView textView) {
                            applyTextViewTypeface(textView, replacement, style);
                        }
                        return result;
                    });

            Method paintSetTypeface = Paint.class.getDeclaredMethod("setTypeface", Typeface.class);
            xposed.hook(paintSetTypeface)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return chain.proceed();
                        }
                        Typeface original = (Typeface) chain.getArg(0);
                        Typeface replacement = resolveReplacement(baseTypeface, original, null);
                        if (replacement == null) {
                            return chain.proceed();
                        }
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof Paint paint) {
                            applyPaintTypeface(paint, replacement);
                            return replacement;
                        }
                        return result;
                    });
            hookInstalled = true;
            DpisLog.i(LOG_PREFIX + "hook ready for " + packageName);
        }
    }

    private static Typeface loadTargetTypeface(String packageName,
                                               DpiConfigStore store,
                                               FontLibraryStore fontLibraryStore) {
        String typefaceId = store != null ? store.getTargetTypefaceId(packageName) : null;
        if (typefaceId == null || typefaceId.isBlank() || fontLibraryStore == null) {
            return null;
        }
        File file = fontLibraryStore.resolveFontFile(typefaceId);
        if (file == null || !file.canRead()) {
            logIfChanged(packageName + ":unreadable:" + typefaceId,
                    LOG_PREFIX + "font file unreadable: package=" + packageName
                            + ", typefaceId=" + typefaceId);
            return null;
        }
        try {
            return Typeface.createFromFile(file);
        } catch (Throwable throwable) {
            logIfChanged(packageName + ":load-failed:" + typefaceId,
                    LOG_PREFIX + "font load failed: package=" + packageName
                            + ", typefaceId=" + typefaceId
                            + ", error=" + throwable.getClass().getSimpleName());
            return null;
        }
    }

    private static void applyTextViewTypeface(TextView textView, Typeface replacement, Integer explicitStyle) {
        INTERNAL_UPDATE.set(Boolean.TRUE);
        try {
            if (explicitStyle != null) {
                textView.setTypeface(replacement, explicitStyle);
                return;
            }
            textView.setTypeface(replacement);
        } finally {
            INTERNAL_UPDATE.set(Boolean.FALSE);
        }
    }

    private static void applyPaintTypeface(Paint paint, Typeface replacement) {
        INTERNAL_UPDATE.set(Boolean.TRUE);
        try {
            paint.setTypeface(replacement);
        } finally {
            INTERNAL_UPDATE.set(Boolean.FALSE);
        }
    }

    static int resolveStyleForTest(Integer originalStyle, Integer explicitStyle) {
        return resolveStyle(originalStyle, explicitStyle);
    }

    static Typeface resolveReplacementForTest(Typeface baseTypeface, Typeface original) {
        return resolveReplacement(baseTypeface, original, null);
    }

    private static Typeface resolveReplacement(Typeface baseTypeface,
                                               Typeface original,
                                               Integer explicitStyle) {
        if (baseTypeface == null) {
            return original;
        }
        Integer originalStyle = original != null ? original.getStyle() : null;
        int style = resolveStyle(originalStyle, explicitStyle);
        try {
            Typeface styled = Typeface.create(baseTypeface, style);
            return styled != null ? styled : baseTypeface;
        } catch (Throwable ignored) {
            return baseTypeface;
        }
    }

    private static int resolveStyle(Integer originalStyle, Integer explicitStyle) {
        if (explicitStyle != null) {
            return explicitStyle;
        }
        return originalStyle != null ? originalStyle : Typeface.NORMAL;
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }
}
