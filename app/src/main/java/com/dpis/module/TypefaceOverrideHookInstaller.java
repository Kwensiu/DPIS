package com.dpis.module;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Process;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class TypefaceOverrideHookInstaller {
    private static final String LOG_PREFIX = "DPIS_FONT_STYLE ";
    // Process-level hook matching existing app-process installers; ModulePackagePlan decides
    // whether it is loaded for the current package.
    private static volatile boolean hookInstalled;
    private static volatile int hookInstalledPid = -1;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TypefaceOverrideHookInstaller() {
    }

    static void install(XposedInterface xposed,
                        String packageName,
                        String targetTypefaceId,
                        DpiConfigStore store,
                        FontLibraryStore fontLibraryStore) throws ReflectiveOperationException {
        if (isHookInstalledForCurrentProcess()) {
            return;
        }
        synchronized (TypefaceOverrideHookInstaller.class) {
            if (isHookInstalledForCurrentProcess()) {
                return;
            }
            Typeface baseTypeface = loadTargetTypeface(
                    packageName, targetTypefaceId, store, fontLibraryStore);
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
                            logReplacementHit(packageName, "TextView.setTypeface(Typeface)");
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
                            logReplacementHit(packageName, "TextView.setTypeface(Typeface,int)");
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
                            logReplacementHit(packageName, "Paint.setTypeface");
                        }
                        return result;
                    });
            installTextViewAttachHook(xposed, textViewClass, baseTypeface, packageName);
            installTextViewDrawHook(xposed, textViewClass, baseTypeface, packageName);
            hookInstalled = true;
            hookInstalledPid = Process.myPid();
            DpisLog.i(LOG_PREFIX + "hook ready for " + packageName);
        }
    }

    private static boolean isHookInstalledForCurrentProcess() {
        return hookInstalled && hookInstalledPid == Process.myPid();
    }

    private static Typeface loadTargetTypeface(String packageName,
                                               String targetTypefaceId,
                                               DpiConfigStore store,
                                               FontLibraryStore fontLibraryStore) {
        String typefaceId = targetTypefaceId;
        if ((typefaceId == null || typefaceId.isBlank()) && store != null) {
            typefaceId = store.getTargetTypefaceId(packageName);
        }
        if (typefaceId == null || typefaceId.isBlank()) {
            logIfChanged(packageName + ":missing-typeface-id",
                    LOG_PREFIX + "target typeface missing: package=" + packageName);
            return null;
        }
        Typeface systemTypeface = SystemFontRegistry.loadTypeface(typefaceId);
        if (systemTypeface != null) {
            logIfChanged(packageName + ":loaded:" + typefaceId,
                    LOG_PREFIX + "target typeface loaded: package=" + packageName
                            + ", typefaceId=" + typefaceId);
            return systemTypeface;
        }
        if (SystemFontRegistry.isSystemFontId(typefaceId) || fontLibraryStore == null) {
            logIfChanged(packageName + ":system-load-failed:" + typefaceId,
                    LOG_PREFIX + "system typeface unavailable: package=" + packageName
                            + ", typefaceId=" + typefaceId);
            return null;
        }
        FontLibraryEntry entry = fontLibraryStore.findById(typefaceId);
        File file = null;
        int ttcIndex = 0;
        if (entry != null) {
            file = fontLibraryStore.resolveFontFile(typefaceId);
            ttcIndex = entry.ttcIndex;
        }
        if (file == null) {
            file = PublishedFontFileResolver.resolve(typefaceId);
            if (entry == null) {
                ttcIndex = parseTtcIndexFromId(typefaceId);
            }
        }
        if (file == null || !file.canRead()) {
            logIfChanged(packageName + ":unreadable:" + typefaceId,
                    LOG_PREFIX + "font file unreadable: package=" + packageName
                            + ", typefaceId=" + typefaceId);
            return null;
        }
        Typeface loaded = FontTypefaceLoader.load(file, ttcIndex);
        if (loaded == null) {
            logIfChanged(packageName + ":load-failed:" + typefaceId,
                    LOG_PREFIX + "font load failed: package=" + packageName
                            + ", typefaceId=" + typefaceId);
        }
        return loaded;
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

    static int resolveReplacementStyleForTest(Integer originalStyle, Integer explicitStyle) {
        return resolveStyle(originalStyle, explicitStyle);
    }

    static int parseTtcIndexFromIdForTest(String typefaceId) {
        return parseTtcIndexFromId(typefaceId);
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

    private static int parseTtcIndexFromId(String typefaceId) {
        int marker = typefaceId.lastIndexOf("_ttc_");
        if (marker <= 0 || marker + 5 >= typefaceId.length()) {
            return 0;
        }
        String suffix = typefaceId.substring(marker + 5);
        try {
            int index = Integer.parseInt(suffix);
            return index >= 0 ? index : 0;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }

    private static void installTextViewAttachHook(XposedInterface xposed,
                                                  Class<?> textViewClass,
                                                  Typeface baseTypeface,
                                                  String packageName) {
        try {
            Method onAttachedToWindow = findOnAttachedToWindowMethod(textViewClass);
            xposed.hook(onAttachedToWindow)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof TextView textView)) {
                            return result;
                        }
                        Typeface replacement = resolveReplacement(baseTypeface, textView.getTypeface(), null);
                        if (replacement == null) {
                            return result;
                        }
                        applyTextViewTypeface(textView, replacement, null);
                        logReplacementHit(packageName, "TextView.onAttachedToWindow");
                        return result;
                    });
            logIfChanged(packageName + ":attach-hook",
                    LOG_PREFIX + "TextView attach hook ready for " + packageName);
        } catch (Throwable throwable) {
            logIfChanged(packageName + ":attach-hook-skipped",
                    LOG_PREFIX + "TextView attach hook skipped: package=" + packageName
                            + ", error=" + throwable.getClass().getSimpleName());
        }
    }

    private static void installTextViewDrawHook(XposedInterface xposed,
                                                Class<?> textViewClass,
                                                Typeface baseTypeface,
                                                String packageName) {
        try {
            Method onDraw = textViewClass.getDeclaredMethod("onDraw", Canvas.class);
            xposed.hook(onDraw)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            Object thisObject = chain.getThisObject();
                            if (thisObject instanceof TextView textView) {
                                Typeface replacement = resolveReplacement(
                                        baseTypeface, textView.getTypeface(), null);
                                if (replacement != null) {
                                    applyTextViewTypeface(textView, replacement, null);
                                    logReplacementHit(packageName, "TextView.onDraw");
                                }
                            }
                        }
                        return chain.proceed();
                    });
            logIfChanged(packageName + ":draw-hook",
                    LOG_PREFIX + "TextView draw hook ready for " + packageName);
        } catch (Throwable throwable) {
            logIfChanged(packageName + ":draw-hook-skipped",
                    LOG_PREFIX + "TextView draw hook skipped: package=" + packageName
                            + ", error=" + throwable.getClass().getSimpleName());
        }
    }

    private static Method findOnAttachedToWindowMethod(Class<?> textViewClass)
            throws NoSuchMethodException {
        try {
            return textViewClass.getDeclaredMethod("onAttachedToWindow");
        } catch (NoSuchMethodException ignored) {
            return View.class.getDeclaredMethod("onAttachedToWindow");
        }
    }

    private static void logReplacementHit(String packageName, String source) {
        logIfChanged(packageName + ":replacement-hit:" + source,
                LOG_PREFIX + "replacement hit: package=" + packageName
                        + ", source=" + source);
    }
}
