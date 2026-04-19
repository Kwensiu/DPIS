package com.dpis.module;

import android.util.TypedValue;
import android.widget.TextView;
import android.text.NoCopySpan;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewParent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class ForceTextSizeHookInstaller {
    private static final String XIAOHEIHE_EXPRESSION_TEXT_VIEW =
            "com.max.xiaoheihe.module.expression.widget.ExpressionTextView";
    private static final String FONT_LOG_KEY_PREFIX = "font";
    private static final String FONT_HOT_LOG_KEY_PREFIX = "font-hot";
    private static volatile boolean hookInstalled;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> HOT_LOG_COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> CALLER_SAMPLE_COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> CALLER_SOURCE_COUNTS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> INTERNAL_TEXT_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final Map<TextView, Float> EXPRESSION_BASE_TEXT_SIZES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<TextView, Float> TEXT_VIEW_BASE_TEXT_SIZES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<TextView, Float> COMMENT_TEXT_BASE_TEXT_SIZES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<TextView, Float> LAST_TARGET_TEXT_SIZES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Paint, Float> PAINT_BASE_TEXT_SIZES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<TextPaint, Float> TEXT_PAINT_BASE_TEXT_SIZES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final float SIZE_EPSILON_PX = 0.5f;
    private static final int MAX_SAMPLES_PER_CALLER = 1;
    private static final int MAX_SAMPLES_PER_SOURCE = 1;
    private static final int HOT_LOG_INTERVAL = 32;
    private static final int MAX_STACK_FRAMES = 6;
    private static volatile boolean verboseFontLogsEnabled;

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
            final float factor = PaintTextSizeFallbackHookInstaller.resolveFieldRewriteFactor(
                    store, packageName);
            verboseFontLogsEnabled = isVerboseFontLogsEnabled(store);
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
                        if (!isTargetPercentActive(targetPercent)) {
                            return result;
                        }
                        int unit = (Integer) chain.getArg(0);
                        float size = (Float) chain.getArg(1);
                        if (size <= 0f) {
                            return result;
                        }
                        if (!shouldForceTextUnit(unit)) {
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
                        if (!shouldApplyTargetSize(textView, forcedPx)) {
                            return result;
                        }
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, forcedPx);
                            TEXT_VIEW_BASE_TEXT_SIZES.put(textView, originalPx);
                            markAppliedTargetSize(textView, forcedPx);
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                        }
                        if (verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                            logSampled(buildHotFontLogKey(packageName, "text-size-unit-" + unit),
                                    "DPIS_FONT ForceTextSize override: unit=" + unit
                                            + ", size=" + size
                                            + ", px=" + originalPx + " -> " + forcedPx
                                            + ", view=" + textView.getClass().getName()
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent,
                                    HOT_LOG_INTERVAL);
                            logCallerSample(packageName, "text-size-unit");
                        }
                        FontDebugStatsReporter.record(
                                "text-size-unit-" + unit,
                                textView.getClass().getName(),
                                textView.getContext());
                        return result;
                    });
            Method setTextSizeFloatMethod = textViewClass.getDeclaredMethod("setTextSize", float.class);
            xposed.hook(setTextSizeFloatMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        if (isForwardedFromSetTextSizeWithUnit()) {
                            return result;
                        }
                        if (!isTargetPercentActive(targetPercent)) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof TextView textView)) {
                            return result;
                        }
                        float sizeSp = (Float) chain.getArg(0);
                        if (sizeSp <= 0f) {
                            return result;
                        }
                        float originalPx = FontScaleOverride.toPx(
                                TypedValue.COMPLEX_UNIT_SP, sizeSp, textView.getResources().getDisplayMetrics());
                        if (originalPx <= 0f) {
                            return result;
                        }
                        float forcedPx = originalPx * factor;
                        if (!shouldApplyTargetSize(textView, forcedPx)) {
                            return result;
                        }
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, forcedPx);
                            TEXT_VIEW_BASE_TEXT_SIZES.put(textView, originalPx);
                            markAppliedTargetSize(textView, forcedPx);
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                        }
                        if (verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                            logSampled(buildHotFontLogKey(packageName, "text-size-float"),
                                    "DPIS_FONT ForceTextSize override: unit=SP(default)"
                                            + ", size=" + sizeSp
                                            + ", px=" + originalPx + " -> " + forcedPx
                                            + ", view=" + textView.getClass().getName()
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent,
                                    HOT_LOG_INTERVAL);
                            logCallerSample(packageName, "text-size-float");
                        }
                        FontDebugStatsReporter.record(
                                "text-size-float",
                                textView.getClass().getName(),
                                textView.getContext());
                        return result;
                    });
            // Keep TextAppearance fallback for views that only style text via appearances.
            // Keep onDraw disabled to avoid repeated scaling in hot draw paths.
            installTextAppearanceHooks(xposed, textViewClass, factor, targetPercent, packageName);
            installPaintTextSizeHooks(xposed, factor, targetPercent, packageName);
            installExpressionTextSetTextHook(xposed, textViewClass, factor, targetPercent, packageName);
            hookInstalled = true;
            DpisLog.i("ForceTextSize hook ready");
        }
    }

    private static void installPaintTextSizeHooks(XposedInterface xposed,
                                                  float factor,
                                                  Integer targetPercent,
                                                  String packageName) throws ReflectiveOperationException {
        Method paintSetTextSize = Paint.class.getDeclaredMethod("setTextSize", float.class);
        xposed.hook(paintSetTextSize)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                        return chain.proceed();
                    }
                    if (!isTargetPercentActive(targetPercent)) {
                        return chain.proceed();
                    }
                    Object thisObject = chain.getThisObject();
                    if (!(thisObject instanceof Paint paint)) {
                        return chain.proceed();
                    }
                    float incoming = (Float) chain.getArg(0);
                    float adjusted = FontFieldRewriteMath.resolveScaledPaintSize(
                            incoming, factor, PAINT_BASE_TEXT_SIZES, paint);
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
                    if (verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                        logSampled(buildHotFontLogKey(packageName, "paint-size"),
                                "DPIS_FONT Paint.setTextSize override: in=" + incoming
                                        + ", out=" + adjusted
                                        + ", factor=" + factor
                                        + ", percent=" + targetPercent,
                                HOT_LOG_INTERVAL);
                        logCallerSample(packageName, "paint-size");
                    }
                    FontDebugStatsReporter.record(
                            "paint-size",
                            paint.getClass().getName(),
                            null);
                    return result;
                });
        try {
            Method textPaintSetTextSize = TextPaint.class.getMethod("setTextSize", float.class);
            xposed.hook(textPaintSetTextSize)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return chain.proceed();
                        }
                        if (!isTargetPercentActive(targetPercent)) {
                            return chain.proceed();
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof TextPaint textPaint)) {
                            return chain.proceed();
                        }
                        float incoming = (Float) chain.getArg(0);
                        float adjusted = FontFieldRewriteMath.resolveScaledPaintSize(
                                incoming, factor, TEXT_PAINT_BASE_TEXT_SIZES, textPaint);
                        Object result = chain.proceed();
                        if (Math.abs(adjusted - incoming) < SIZE_EPSILON_PX) {
                            return result;
                        }
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            textPaint.setTextSize(adjusted);
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                        }
                        if (verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                            logSampled(buildHotFontLogKey(packageName, "textpaint-size"),
                                    "DPIS_FONT TextPaint.setTextSize override: in=" + incoming
                                            + ", out=" + adjusted
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent,
                                    HOT_LOG_INTERVAL);
                            logCallerSample(packageName, "textpaint-size");
                        }
                        FontDebugStatsReporter.record(
                                "textpaint-size",
                                textPaint.getClass().getName(),
                                null);
                        return result;
                    });
        } catch (Throwable t) {
            logIfChanged(buildFontLogKey(packageName, "textpaint-hook-skip"),
                    "DPIS_FONT TextPaint.setTextSize hook skipped: "
                            + t.getClass().getSimpleName());
        }
    }

    private static void installExpressionTextSetTextHook(XposedInterface xposed,
                                                         Class<?> textViewClass,
                                                         float factor,
                                                         Integer targetPercent,
                                                         String packageName)
            throws ReflectiveOperationException {
        Method setTextMethod = textViewClass.getDeclaredMethod(
                "setText", CharSequence.class, TextView.BufferType.class);
        xposed.hook(setTextMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    if (Boolean.TRUE.equals(INTERNAL_TEXT_UPDATE.get())) {
                        return chain.proceed();
                    }
                    if (!isTargetPercentActive(targetPercent)) {
                        return chain.proceed();
                    }
                    Object thisObject = chain.getThisObject();
                    if (!(thisObject instanceof TextView textView)) {
                        return chain.proceed();
                    }
                    if (XIAOHEIHE_EXPRESSION_TEXT_VIEW.equals(textView.getClass().getName())) {
                        applyExpressionTextSizeOverride(textView, factor);
                    }
                    CharSequence sourceText = (CharSequence) chain.getArg(0);
                    if (!(sourceText instanceof Spanned spanned)) {
                        Object result = chain.proceed();
                        if (reinforceTextViewTarget(textView, factor)) {
                            FontDebugStatsReporter.record(
                                    "textview-settext-reinforce",
                                    textView.getClass().getName(),
                                    textView.getContext());
                        }
                        return result;
                    }
                    CharSequence patched = scaleSpans(spanned, factor);
                    if (patched == sourceText) {
                        Object result = chain.proceed();
                        if (reinforceTextViewTarget(textView, factor)) {
                            FontDebugStatsReporter.record(
                                    "textview-settext-reinforce",
                                    textView.getClass().getName(),
                                    textView.getContext());
                        }
                        return result;
                    }
                    TextView.BufferType bufferType = (TextView.BufferType) chain.getArg(1);
                    INTERNAL_TEXT_UPDATE.set(Boolean.TRUE);
                    try {
                        textView.setText(patched, bufferType);
                    } finally {
                        INTERNAL_TEXT_UPDATE.set(Boolean.FALSE);
                    }
                    if (reinforceTextViewTarget(textView, factor)) {
                        FontDebugStatsReporter.record(
                                "textview-settext-reinforce",
                                textView.getClass().getName(),
                                textView.getContext());
                    }
                    if (verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                        logSampled(buildHotFontLogKey(
                                        packageName, "textview-span-" + textView.getClass().getName()),
                                "DPIS_FONT TextView span override: view="
                                        + textView.getClass().getName()
                                        + ", factor=" + factor
                                        + ", percent=" + targetPercent
                                        + ", length=" + patched.length(),
                                HOT_LOG_INTERVAL);
                    }
                    FontDebugStatsReporter.record(
                            "textview-span",
                            textView.getClass().getName(),
                            textView.getContext());
                    return null;
                });
    }

    private static boolean reinforceTextViewTarget(TextView textView, float factor) {
        if (textView == null) {
            return false;
        }
        Float desiredPx = LAST_TARGET_TEXT_SIZES.get(textView);
        if (desiredPx == null || desiredPx <= 0f) {
            if (!isCommentLikeNode(textView) || !isScaleFactorActive(factor)) {
                return false;
            }
            float currentPx = textView.getTextSize();
            desiredPx = FontFieldRewriteMath.resolveScaledTextSize(
                    currentPx, factor, COMMENT_TEXT_BASE_TEXT_SIZES, textView);
            if (desiredPx <= 0f) {
                return false;
            }
        }
        if (!shouldApplyTargetSize(textView, desiredPx)) {
            return false;
        }
        INTERNAL_UPDATE.set(Boolean.TRUE);
        try {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, desiredPx);
            markAppliedTargetSize(textView, desiredPx);
            return true;
        } finally {
            INTERNAL_UPDATE.set(Boolean.FALSE);
        }
    }

    private static boolean isCommentLikeNode(TextView textView) {
        if (textView == null) {
            return false;
        }
        if (containsCommentHint(textView.getClass().getName())) {
            return true;
        }
        try {
            int viewId = textView.getId();
            if (viewId != View.NO_ID) {
                String entryName = textView.getResources().getResourceEntryName(viewId);
                if (containsCommentHint(entryName)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        ViewParent parent = textView.getParent();
        int depth = 0;
        while (parent != null && depth < 4) {
            if (containsCommentHint(parent.getClass().getName())) {
                return true;
            }
            parent = parent.getParent();
            depth++;
        }
        return false;
    }

    private static boolean containsCommentHint(String text) {
        return FontFieldRewriteMath.containsCommentHint(text);
    }

    private static void installTextAppearanceHooks(XposedInterface xposed,
                                                   Class<?> textViewClass,
                                                   float factor,
                                                   Integer targetPercent,
                                                   String packageName) {
        try {
            Method setTextAppearanceCtx = textViewClass.getDeclaredMethod(
                    "setTextAppearance", android.content.Context.class, int.class);
            xposed.hook(setTextAppearanceCtx)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!isTargetPercentActive(targetPercent)) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof TextView textView)) {
                            return result;
                        }
                        applyTextViewSizeOverride(textView, factor);
                        logIfChanged(buildFontLogKey(packageName, "text-appearance-ctx"),
                                "DPIS_FONT TextAppearance override: view="
                                        + textView.getClass().getName()
                                        + ", factor=" + factor
                                        + ", percent=" + targetPercent);
                        return result;
                    });
        } catch (Throwable ignored) {
        }
        try {
            Method setTextAppearanceRes = textViewClass.getDeclaredMethod("setTextAppearance", int.class);
            xposed.hook(setTextAppearanceRes)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!isTargetPercentActive(targetPercent)) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof TextView textView)) {
                            return result;
                        }
                        applyTextViewSizeOverride(textView, factor);
                        logIfChanged(buildFontLogKey(packageName, "text-appearance-res"),
                                "DPIS_FONT TextAppearance(int) override: view="
                                        + textView.getClass().getName()
                                        + ", factor=" + factor
                                        + ", percent=" + targetPercent);
                        return result;
                    });
        } catch (Throwable ignored) {
        }
    }

    private static void installTextViewDrawHook(XposedInterface xposed,
                                                Class<?> textViewClass,
                                                float factor,
                                                Integer targetPercent,
                                                String packageName) {
        try {
            Method onDrawMethod = textViewClass.getDeclaredMethod("onDraw", android.graphics.Canvas.class);
            xposed.hook(onDrawMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (isTargetPercentActive(targetPercent)) {
                            Object thisObject = chain.getThisObject();
                            if (thisObject instanceof TextView textView
                                    && !Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                                applyTextViewSizeOverride(textView, factor);
                            }
                        }
                        return chain.proceed();
                    });
            logIfChanged(buildFontLogKey(packageName, "textview-ondraw-hook"),
                    "DPIS_FONT TextView onDraw guard enabled");
        } catch (Throwable ignored) {
        }
    }

    private static void applyExpressionTextSizeOverride(TextView textView, float factor) {
        float currentPx = textView.getTextSize();
        float desiredPx = FontFieldRewriteMath.resolveScaledTextSize(
                currentPx, factor, EXPRESSION_BASE_TEXT_SIZES, textView);
        if (!shouldApplyTargetSize(textView, desiredPx)) {
            return;
        }
        INTERNAL_UPDATE.set(Boolean.TRUE);
        try {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, desiredPx);
            markAppliedTargetSize(textView, desiredPx);
        } finally {
            INTERNAL_UPDATE.set(Boolean.FALSE);
        }
    }

    private static void applyTextViewSizeOverride(TextView textView, float factor) {
        float currentPx = textView.getTextSize();
        float expectedPx = FontFieldRewriteMath.resolveScaledTextSize(
                currentPx, factor, TEXT_VIEW_BASE_TEXT_SIZES, textView);
        if (!shouldApplyTargetSize(textView, expectedPx)) {
            return;
        }
        INTERNAL_UPDATE.set(Boolean.TRUE);
        try {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, expectedPx);
            markAppliedTargetSize(textView, expectedPx);
        } finally {
            INTERNAL_UPDATE.set(Boolean.FALSE);
        }
    }

    private static boolean shouldApplyTargetSize(TextView textView, float targetPx) {
        if (textView == null || targetPx <= 0f) {
            return false;
        }
        float currentPx = textView.getTextSize();
        return Math.abs(currentPx - targetPx) >= SIZE_EPSILON_PX;
    }

    private static void markAppliedTargetSize(TextView textView, float targetPx) {
        if (textView == null || targetPx <= 0f) {
            return;
        }
        LAST_TARGET_TEXT_SIZES.put(textView, targetPx);
    }

    private static CharSequence scaleSpans(Spanned source, float factor) {
        if (!isScaleFactorActive(factor)) {
            return source;
        }
        FontScaledMarker[] markers = source.getSpans(0, source.length(), FontScaledMarker.class);
        if (markers != null && markers.length > 0) {
            return source;
        }
        SpannableStringBuilder builder = null;
        boolean changed = false;

        AbsoluteSizeSpan[] absoluteSizeSpans = source.getSpans(0, source.length(), AbsoluteSizeSpan.class);
        if (absoluteSizeSpans != null && absoluteSizeSpans.length > 0) {
            for (AbsoluteSizeSpan span : absoluteSizeSpans) {
                int start = source.getSpanStart(span);
                int end = source.getSpanEnd(span);
                int flags = source.getSpanFlags(span);
                if (start < 0 || end <= start) {
                    continue;
                }
                int originalSize = span.getSize();
                int scaledSize = FontFieldRewriteMath.scaleAbsoluteSize(originalSize, factor);
                if (scaledSize == originalSize) {
                    continue;
                }
                if (builder == null) {
                    builder = new SpannableStringBuilder(source);
                }
                builder.removeSpan(span);
                builder.setSpan(new AbsoluteSizeSpan(scaledSize, span.getDip()), start, end, flags);
                changed = true;
            }
        }

        RelativeSizeSpan[] relativeSizeSpans = source.getSpans(0, source.length(), RelativeSizeSpan.class);
        if (relativeSizeSpans != null && relativeSizeSpans.length > 0) {
            for (RelativeSizeSpan span : relativeSizeSpans) {
                int start = source.getSpanStart(span);
                int end = source.getSpanEnd(span);
                int flags = source.getSpanFlags(span);
                if (start < 0 || end <= start) {
                    continue;
                }
                float originalSize = span.getSizeChange();
                float scaledSize = FontFieldRewriteMath.scaleRelativeSize(originalSize, factor);
                if (Math.abs(scaledSize - originalSize) < 0.0001f) {
                    continue;
                }
                if (builder == null) {
                    builder = new SpannableStringBuilder(source);
                }
                builder.removeSpan(span);
                builder.setSpan(new RelativeSizeSpan(scaledSize), start, end, flags);
                changed = true;
            }
        }
        if (changed && builder != null && builder.length() > 0) {
            builder.setSpan(new FontScaledMarker(), 0, builder.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return changed ? builder : source;
    }

    private static final class FontScaledMarker implements NoCopySpan {
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }

    private static void logSampled(String key, String message, int interval) {
        if (!verboseFontLogsEnabled) {
            return;
        }
        if (interval <= 1) {
            DpisLog.i(message);
            return;
        }
        int count = HOT_LOG_COUNTS.getOrDefault(key, 0) + 1;
        HOT_LOG_COUNTS.put(key, count);
        if (count == 1 || (count % interval) == 0) {
            DpisLog.i(message + ", sample=" + count);
        }
    }

    private static String buildFontLogKey(String packageName, String suffix) {
        String pkg = packageName == null ? "unknown" : packageName;
        return pkg + ":" + FONT_LOG_KEY_PREFIX + ":" + suffix;
    }

    private static String buildHotFontLogKey(String packageName, String suffix) {
        String pkg = packageName == null ? "unknown" : packageName;
        return pkg + ":" + FONT_HOT_LOG_KEY_PREFIX + ":" + suffix;
    }

    private static void logCallerSample(String packageName, String sourceTag) {
        if (!verboseFontLogsEnabled || !DpisLog.isLoggingEnabled()) {
            return;
        }
        int sourceCount = CALLER_SOURCE_COUNTS.getOrDefault(sourceTag, 0);
        if (sourceCount >= MAX_SAMPLES_PER_SOURCE) {
            return;
        }
        String stackSummary = summarizeStack(Thread.currentThread().getStackTrace());
        if (stackSummary == null || stackSummary.isEmpty()) {
            return;
        }
        String callerKey = sourceTag + "|" + stackSummary;
        int count = CALLER_SAMPLE_COUNTS.getOrDefault(callerKey, 0);
        if (count >= MAX_SAMPLES_PER_CALLER) {
            return;
        }
        CALLER_SAMPLE_COUNTS.put(callerKey, count + 1);
        CALLER_SOURCE_COUNTS.put(sourceTag, sourceCount + 1);
        DpisLog.i("DPIS_FONT caller(" + packageName + "," + sourceTag + "): " + stackSummary);
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
                    || className.startsWith("com.dpis.module.ForceTextSizeHookInstaller")) {
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

    private static boolean isForwardedFromSetTextSizeWithUnit() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (trace == null) {
            return false;
        }
        int textViewSetTextSizeFrames = 0;
        for (StackTraceElement element : trace) {
            if (element == null) {
                continue;
            }
            String className = element.getClassName();
            String methodName = element.getMethodName();
            if ("android.widget.TextView".equals(className)
                    && "setTextSize".equals(methodName)) {
                textViewSetTextSizeFrames++;
                if (textViewSetTextSizeFrames >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldForceTextUnit(int unit) {
        return unit == TypedValue.COMPLEX_UNIT_SP
                || unit == TypedValue.COMPLEX_UNIT_PX
                || unit == TypedValue.COMPLEX_UNIT_DIP
                || unit == TypedValue.COMPLEX_UNIT_PT
                || unit == TypedValue.COMPLEX_UNIT_IN
                || unit == TypedValue.COMPLEX_UNIT_MM;
    }

    private static boolean isTargetPercentActive(Integer targetPercent) {
        return targetPercent != null && targetPercent > 0 && targetPercent != 100;
    }

    private static boolean isScaleFactorActive(float factor) {
        return factor > 0f && factor != 1.0f;
    }

    private static boolean isVerboseFontLogsEnabled(DpiConfigStore store) {
        return store != null && store.isFontDebugOverlayEnabled();
    }
}
