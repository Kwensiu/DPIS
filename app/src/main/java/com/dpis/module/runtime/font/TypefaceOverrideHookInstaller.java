package com.dpis.module.runtime.font;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.diagnostics.RuntimeEvents;

import com.dpis.module.BuildConfig;

import com.dpis.module.config.ModulePackagePlan;


import com.dpis.module.DpisLog;


import com.dpis.module.fonts.FontLibraryEntry;
import com.dpis.module.fonts.FontLibraryStore;
import com.dpis.module.fonts.FontFace;

import com.dpis.module.runtime.hookapi.ModernApiCapabilities;

import com.dpis.module.fonts.PublishedFontFileResolver;

import com.dpis.module.fonts.FontTypefaceLoader;
import com.dpis.module.fonts.FontProviderTypefaceLoader;

import com.dpis.module.fonts.SystemFontRegistry;

import com.dpis.module.runtime.ProcessScopedInstallGate;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

public final class TypefaceOverrideHookInstaller {
    private static final String BRIDGE_LOG_PREFIX = "DPIS ";
    private static final String LOG_PREFIX = "DPIS_FONT_STYLE ";
    private static final String HOOK_ID_TEXTVIEW_SET_TYPEFACE =
            "typeface_textview_set_typeface";
    private static final String HOOK_ID_TEXTVIEW_SET_TYPEFACE_WITH_STYLE =
            "typeface_textview_set_typeface_with_style";
    private static final String HOOK_ID_PAINT_SET_TYPEFACE =
            "typeface_paint_set_typeface";
    private static final String HOOK_ID_TEXTVIEW_ON_ATTACHED_TO_WINDOW =
            "typeface_textview_on_attached_to_window";
    private static final String HOOK_ID_TEXTVIEW_ON_DRAW =
            "typeface_textview_on_draw";
    // Process-level hook matching existing app-process installers; ModulePackagePlan decides
    // whether it is loaded for the current package.
    private static volatile int installedPid = -1;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final Map<String, String> LAST_LOAD_SOURCES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> LAST_LOAD_TTC_INDICES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TypefaceOverrideHookInstaller() {
    }

    public static void resetForHotReload() {
        installedPid = -1;
        LAST_MESSAGES.clear();
        LAST_LOAD_SOURCES.clear();
        LAST_LOAD_TTC_INDICES.clear();
    }

    public static void install(XposedInterface xposed,
                        String packageName,
                        String targetTypefaceId,
                        DpisConfigStore store,
                        FontLibraryStore fontLibraryStore,
                        ModernApiCapabilities apiCapabilities) throws ReflectiveOperationException {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return;
        }
        synchronized (TypefaceOverrideHookInstaller.class) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
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
            apiCapabilities.applyStableHookId(
                            xposed.hook(setTypeface)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_TEXTVIEW_SET_TYPEFACE)
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
                            bridgeOverrideAppliedIfChanged(
                                    xposed, packageName, HOOK_ID_TEXTVIEW_SET_TYPEFACE);
                        }
                        return result;
                    });

            Method setTypefaceWithStyle =
                    textViewClass.getDeclaredMethod("setTypeface", Typeface.class, int.class);
            apiCapabilities.applyStableHookId(
                            xposed.hook(setTypefaceWithStyle)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_TEXTVIEW_SET_TYPEFACE_WITH_STYLE)
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
                            bridgeOverrideAppliedIfChanged(
                                    xposed, packageName, HOOK_ID_TEXTVIEW_SET_TYPEFACE_WITH_STYLE);
                        }
                        return result;
                    });

            Method paintSetTypeface = Paint.class.getDeclaredMethod("setTypeface", Typeface.class);
            apiCapabilities.applyStableHookId(
                            xposed.hook(paintSetTypeface)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_PAINT_SET_TYPEFACE)
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
                            bridgeOverrideAppliedIfChanged(
                                    xposed, packageName, HOOK_ID_PAINT_SET_TYPEFACE);
                        }
                        return result;
                    });
            installTextViewAttachHook(
                    xposed, textViewClass, baseTypeface, packageName, apiCapabilities);
            installTextViewDrawHook(
                    xposed, textViewClass, baseTypeface, packageName, apiCapabilities);
            installedPid = ProcessScopedInstallGate.currentPid();
            DpisLog.i(LOG_PREFIX + "hook ready for " + packageName
                    + ", hookIds=" + HOOK_ID_TEXTVIEW_SET_TYPEFACE + ","
                    + HOOK_ID_TEXTVIEW_SET_TYPEFACE_WITH_STYLE + ","
                    + HOOK_ID_PAINT_SET_TYPEFACE + ","
                    + HOOK_ID_TEXTVIEW_ON_ATTACHED_TO_WINDOW + ","
                    + HOOK_ID_TEXTVIEW_ON_DRAW);
            bridgeLog(xposed, LOG_PREFIX + "hook ready: package=" + packageName
                    + ", hookIds=" + HOOK_ID_TEXTVIEW_SET_TYPEFACE + ","
                    + HOOK_ID_TEXTVIEW_SET_TYPEFACE_WITH_STYLE + ","
                    + HOOK_ID_PAINT_SET_TYPEFACE + ","
                    + HOOK_ID_TEXTVIEW_ON_ATTACHED_TO_WINDOW + ","
                    + HOOK_ID_TEXTVIEW_ON_DRAW);
            RuntimeEvents.recordTypeface(
                    packageName,
                    "hook_installed",
                    "typefaceId=" + targetTypefaceId
                            + ", loadSource=" + loadSourceFor(packageName, targetTypefaceId)
                            + ", ttcIndex=" + ttcIndexFor(packageName, targetTypefaceId));
        }
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
            recordLoadSource(packageName, typefaceId, "system", 0);
            logIfChanged(packageName + ":loaded:" + typefaceId,
                    LOG_PREFIX + "target typeface loaded: package=" + packageName
                            + ", typefaceId=" + typefaceId);
            return systemTypeface;
        }
        if (SystemFontRegistry.isSystemFontId(typefaceId)) {
            logIfChanged(packageName + ":system-load-failed:" + typefaceId,
                    LOG_PREFIX + "system typeface unavailable: package=" + packageName
                            + ", typefaceId=" + typefaceId);
            return null;
        }
        FontFace selectedFace = FontFace.fromLegacyId(typefaceId);
        int ttcIndex = selectedFace != null ? selectedFace.ttcIndex : 0;
        Typeface providerTypeface = FontProviderTypefaceLoader.load(typefaceId, ttcIndex);
        if (providerTypeface != null) {
            recordLoadSource(packageName, typefaceId, "provider", ttcIndex);
            logTypefaceIfChanged(packageName, "source_provider_loaded", typefaceId,
                    packageName + ":loaded-provider:" + typefaceId,
                    LOG_PREFIX + "target typeface loaded through provider: package=" + packageName
                            + ", typefaceId=" + typefaceId);
            return providerTypeface;
        }
        if (fontLibraryStore == null) {
            recordLoadSource(packageName, typefaceId, "provider_failed", ttcIndex);
            logIfChanged(packageName + ":provider-load-failed:" + typefaceId,
                    LOG_PREFIX + "font provider unavailable and no fallback catalog: package="
                            + packageName + ", typefaceId=" + typefaceId);
            return null;
        }
        FontLibraryEntry entry = fontLibraryStore.findById(typefaceId);
        File file = null;
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
            recordLoadSource(packageName, typefaceId, "unreadable", ttcIndex);
            logIfChanged(packageName + ":unreadable:" + typefaceId,
                    LOG_PREFIX + "font file unreadable: package=" + packageName
                            + ", typefaceId=" + typefaceId);
            return null;
        }
        Typeface loaded = FontTypefaceLoader.load(file, ttcIndex);
        if (loaded == null) {
            recordLoadSource(packageName, typefaceId, "load_failed", ttcIndex);
            logTypefaceIfChanged(packageName, "load_failed", typefaceId,
                    packageName + ":load-failed:" + typefaceId,
                    LOG_PREFIX + "font load failed: package=" + packageName
                            + ", typefaceId=" + typefaceId);
        } else {
            recordLoadSource(packageName, typefaceId, "fallback", ttcIndex);
            logTypefaceIfChanged(packageName, "source_fallback_loaded", typefaceId,
                    packageName + ":loaded-fallback:" + typefaceId,
                    LOG_PREFIX + "target typeface loaded through fallback: package=" + packageName
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
            INTERNAL_UPDATE.remove();
        }
    }

    private static void applyPaintTypeface(Paint paint, Typeface replacement) {
        INTERNAL_UPDATE.set(Boolean.TRUE);
        try {
            paint.setTypeface(replacement);
        } finally {
            INTERNAL_UPDATE.remove();
        }
    }

    public static int resolveStyleForTest(Integer originalStyle, Integer explicitStyle) {
        return resolveStyle(originalStyle, explicitStyle);
    }

    public static int resolveReplacementStyleForTest(Integer originalStyle, Integer explicitStyle) {
        return resolveStyle(originalStyle, explicitStyle);
    }

    public static int parseTtcIndexFromIdForTest(String typefaceId) {
        return parseTtcIndexFromId(typefaceId);
    }

    public static Typeface resolveReplacementForTest(Typeface baseTypeface, Typeface original) {
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

    private static void recordLoadSource(
            String packageName,
            String typefaceId,
            String source,
            int ttcIndex
    ) {
        String key = packageName + ":" + typefaceId;
        LAST_LOAD_SOURCES.put(key, source);
        LAST_LOAD_TTC_INDICES.put(key, ttcIndex);
        RuntimeEvents.recordTypeface(
                packageName,
                "load_source",
                "typefaceId=" + typefaceId + ", source=" + source + ", ttcIndex=" + ttcIndex);
    }

    private static String loadSourceFor(String packageName, String typefaceId) {
        String value = LAST_LOAD_SOURCES.get(packageName + ":" + typefaceId);
        return value != null ? value : "unknown";
    }

    private static int ttcIndexFor(String packageName, String typefaceId) {
        Integer value = LAST_LOAD_TTC_INDICES.get(packageName + ":" + typefaceId);
        return value != null ? value : 0;
    }

    private static boolean logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
            return true;
        }
        return false;
    }

    private static void logTypefaceIfChanged(
            String packageName,
            String stage,
            String typefaceId,
            String key,
            String message
    ) {
        if (logIfChanged(key, message)) {
            RuntimeEvents.recordTypeface(
                    packageName, stage, "typefaceId=" + typefaceId);
        }
    }

    private static void installTextViewAttachHook(XposedInterface xposed,
                                                  Class<?> textViewClass,
                                                  Typeface baseTypeface,
                                                  String packageName,
                                                  ModernApiCapabilities apiCapabilities) {
        try {
            Method onAttachedToWindow = findOnAttachedToWindowMethod(textViewClass);
            // Stable id lets 102 replace the reinforcement hook without keeping
            // a stale attach-time typeface route after a module hot reload.
            apiCapabilities.applyStableHookId(
                            xposed.hook(onAttachedToWindow)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_TEXTVIEW_ON_ATTACHED_TO_WINDOW)
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
                        bridgeOverrideAppliedIfChanged(
                                xposed, packageName, HOOK_ID_TEXTVIEW_ON_ATTACHED_TO_WINDOW);
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
                                                String packageName,
                                                ModernApiCapabilities apiCapabilities) {
        try {
            Method onDraw = textViewClass.getDeclaredMethod("onDraw", Canvas.class);
            apiCapabilities.applyStableHookId(
                            xposed.hook(onDraw)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_TEXTVIEW_ON_DRAW)
                    .intercept(chain -> {
                        if (!Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            Object thisObject = chain.getThisObject();
                            if (thisObject instanceof TextView textView) {
                                Typeface replacement = resolveReplacement(
                                        baseTypeface, textView.getTypeface(), null);
                                if (replacement != null) {
                                    applyTextViewTypeface(textView, replacement, null);
                                    logReplacementHit(packageName, "TextView.onDraw");
                                    bridgeOverrideAppliedIfChanged(
                                            xposed, packageName, HOOK_ID_TEXTVIEW_ON_DRAW);
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
        if (logIfChanged(packageName + ":replacement-hit:" + source,
                LOG_PREFIX + "replacement hit: package=" + packageName
                        + ", source=" + source)) {
            RuntimeEvents.recordTypeface(
                    packageName, "replacement_hit", "source=" + source);
        }
    }

    private static void bridgeLog(XposedInterface xposed, String message) {
        if (xposed == null || (!BuildConfig.DEBUG && !DpisLog.isLoggingEnabled())) {
            return;
        }
        try {
            xposed.log(android.util.Log.INFO, DpisLog.TAG, BRIDGE_LOG_PREFIX + message);
        } catch (Throwable ignored) {
            // Bridge evidence is diagnostic-only; target app behavior wins.
        }
    }

    private static void bridgeOverrideAppliedIfChanged(
            XposedInterface xposed, String packageName, String hookId) {
        String key = packageName + ":bridge-override:" + hookId;
        String message = LOG_PREFIX + "override applied: package="
                + packageName + ", hookId=" + hookId;
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            bridgeLog(xposed, message);
        }
    }
}
