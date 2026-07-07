package com.dpis.module;

import com.dpis.module.fonts.FontLibraryStore;

import com.dpis.module.fonts.PublishedFontFileResolver;

import com.dpis.module.fonts.SystemFontRegistry;

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

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class LegacyTypefaceOverrideHookInstaller {
    private static final String LOG_PREFIX = "DPIS_FONT_STYLE ";
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static volatile boolean hookInstalled;
    private static volatile int hookInstalledPid = -1;

    private LegacyTypefaceOverrideHookInstaller() {
    }

    static void install(String packageName,
                        String targetTypefaceId,
                        DpisConfigStore store,
                        FontLibraryStore fontLibraryStore) throws ReflectiveOperationException {
        if (isHookInstalledForCurrentProcess()) {
            return;
        }
        synchronized (LegacyTypefaceOverrideHookInstaller.class) {
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
            XposedBridge.hookMethod(setTypeface, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                        return;
                    }
                    Object thisObject = param.thisObject;
                    if (!(thisObject instanceof TextView textView)) {
                        return;
                    }
                    Typeface original = (Typeface) param.args[0];
                    Typeface replacement = resolveReplacement(baseTypeface, original, null);
                    if (replacement != null) {
                        applyTextViewTypeface(textView, replacement, null);
                        logReplacementHit(packageName, "TextView.setTypeface(Typeface)");
                    }
                }
            });

            Method setTypefaceWithStyle =
                    textViewClass.getDeclaredMethod("setTypeface", Typeface.class, int.class);
            XposedBridge.hookMethod(setTypefaceWithStyle, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                        return;
                    }
                    Object thisObject = param.thisObject;
                    if (!(thisObject instanceof TextView textView)) {
                        return;
                    }
                    Typeface original = (Typeface) param.args[0];
                    Integer style = (Integer) param.args[1];
                    Typeface replacement = resolveReplacement(baseTypeface, original, style);
                    if (replacement != null) {
                        applyTextViewTypeface(textView, replacement, style);
                        logReplacementHit(packageName, "TextView.setTypeface(Typeface,int)");
                    }
                }
            });

            Method paintSetTypeface = Paint.class.getDeclaredMethod("setTypeface", Typeface.class);
            XposedBridge.hookMethod(paintSetTypeface, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                        return;
                    }
                    Object thisObject = param.thisObject;
                    if (!(thisObject instanceof Paint paint)) {
                        return;
                    }
                    Typeface original = (Typeface) param.args[0];
                    Typeface replacement = resolveReplacement(baseTypeface, original, null);
                    if (replacement != null) {
                        applyPaintTypeface(paint, replacement);
                        logReplacementHit(packageName, "Paint.setTypeface");
                    }
                }
            });
            installTextViewAttachHook(textViewClass, baseTypeface, packageName);
            installTextViewDrawHook(textViewClass, baseTypeface, packageName);
            hookInstalled = true;
            hookInstalledPid = Process.myPid();
            DpisLog.i(LOG_PREFIX + "hook ready for " + packageName);
            XposedBridge.log("DPIS " + LOG_PREFIX + "hook ready for " + packageName);
        }
    }

    private static boolean isHookInstalledForCurrentProcess() {
        return hookInstalled && hookInstalledPid == Process.myPid();
    }

    private static Typeface loadTargetTypeface(String packageName,
                                               String targetTypefaceId,
                                               DpisConfigStore store,
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
        File file = fontLibraryStore.resolveFontFile(typefaceId);
        if (file == null) {
            file = PublishedFontFileResolver.resolve(typefaceId);
        }
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

    private static Typeface resolveReplacement(Typeface baseTypeface,
                                               Typeface original,
                                               Integer explicitStyle) {
        if (baseTypeface == null) {
            return original;
        }
        int style = resolveStyle(original != null ? original.getStyle() : null, explicitStyle);
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

    private static void installTextViewAttachHook(Class<?> textViewClass,
                                                  Typeface baseTypeface,
                                                  String packageName) {
        try {
            Method onAttachedToWindow = findOnAttachedToWindowMethod(textViewClass);
            XposedBridge.hookMethod(onAttachedToWindow, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                        return;
                    }
                    Object thisObject = param.thisObject;
                    if (!(thisObject instanceof TextView textView)) {
                        return;
                    }
                    Typeface replacement = resolveReplacement(baseTypeface, textView.getTypeface(), null);
                    if (replacement != null) {
                        applyTextViewTypeface(textView, replacement, null);
                        logReplacementHit(packageName, "TextView.onAttachedToWindow");
                    }
                }
            });
            logIfChanged(packageName + ":attach-hook",
                    LOG_PREFIX + "TextView attach hook ready for " + packageName);
        } catch (Throwable throwable) {
            logIfChanged(packageName + ":attach-hook-skipped",
                    LOG_PREFIX + "TextView attach hook skipped: package=" + packageName
                            + ", error=" + throwable.getClass().getSimpleName());
        }
    }

    private static void installTextViewDrawHook(Class<?> textViewClass,
                                                Typeface baseTypeface,
                                                String packageName) {
        try {
            Method onDraw = textViewClass.getDeclaredMethod("onDraw", Canvas.class);
            XposedBridge.hookMethod(onDraw, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                        return;
                    }
                    Object thisObject = param.thisObject;
                    if (!(thisObject instanceof TextView textView)) {
                        return;
                    }
                    Typeface replacement = resolveReplacement(baseTypeface, textView.getTypeface(), null);
                    if (replacement != null) {
                        applyTextViewTypeface(textView, replacement, null);
                        logReplacementHit(packageName, "TextView.onDraw");
                    }
                }
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
