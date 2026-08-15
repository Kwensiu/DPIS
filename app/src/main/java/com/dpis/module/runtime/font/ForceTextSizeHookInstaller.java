package com.dpis.module.runtime.font;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.diagnostics.FeedbackDiagnosticRuntimeTransport;

import com.dpis.module.diagnostics.FeedbackDiagnosticRuntimeHotPathEvents;

import com.dpis.module.BuildConfig;

import com.dpis.module.fonts.hookdomain.FontHookArbitration;




import com.dpis.module.DpisLog;


import com.dpis.module.runtime.hookapi.ModernApiCapabilities;
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver;

import com.dpis.module.fonts.FontDebugStatsReporter;
import com.dpis.module.fonts.TextViewFontProvenanceTracker;

import com.dpis.module.fonts.FontFieldRewriteMath;
import com.dpis.module.fonts.FontMutationScheduler;
import com.dpis.module.fonts.PaintProvenanceTracker;
import com.dpis.module.fonts.PaintDiagnosticCallerSampler;

import com.dpis.module.runtime.ProcessScopedInstallGate;

import android.util.DisplayMetrics;
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

public final class ForceTextSizeHookInstaller {
    private static final String BRIDGE_LOG_PREFIX = "DPIS ";
    private static final String XIAOHEIHE_EXPRESSION_TEXT_VIEW =
            "com.max.xiaoheihe.module.expression.widget.ExpressionTextView";
    private static final String FONT_LOG_KEY_PREFIX = "font";
    private static final String FONT_HOT_LOG_KEY_PREFIX = "font-hot";
    private static final String BRIDGE_LOG_KEY_PREFIX = "font-bridge";
    private static final String HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_WITH_UNIT =
            "textview_set_text_size_with_unit";
    private static final String HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_DEFAULT_SP =
            "textview_set_text_size_default_sp";
    private static final String HOOK_ID_TEXTVIEW_SET_TEXT =
            "textview_set_text";
    private static final String HOOK_ID_TEXTVIEW_ATTACH =
            "textview_on_attached_to_window";
    private static final String HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_CONTEXT =
            "textview_set_text_appearance_context";
    private static final String HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_RES =
            "textview_set_text_appearance_res";
    private static final String HOOK_ID_PAINT_SET_TEXT_SIZE =
            "paint_set_text_size";
    private static final String HOOK_ID_TEXTPAINT_SET_TEXT_SIZE =
            "textpaint_set_text_size";
    private static volatile int installedPid = -1;
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> HOT_LOG_COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> CALLER_SAMPLE_COUNTS = new ConcurrentHashMap<>();
    private static final Map<String, Integer> CALLER_SOURCE_COUNTS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> INTERNAL_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> INTERNAL_TEXT_UPDATE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Integer> TEXT_VIEW_SET_TEXT_SIZE_DEPTH =
            ThreadLocal.withInitial(() -> 0);
    private static final Map<TextView, Float> EXPRESSION_BASE_TEXT_SIZES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<TextView, Float> TEXT_VIEW_BASE_TEXT_SIZES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<TextView, Float> COMMENT_TEXT_BASE_TEXT_SIZES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<TextView, TargetTextSize> LAST_TARGET_TEXT_SIZES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final float SIZE_EPSILON_PX = 0.5f;
    private static final int MAX_SAMPLES_PER_CALLER = 1;
    private static final int MAX_SAMPLES_PER_SOURCE = 1;
    private static final int HOT_LOG_INTERVAL = 32;
    private static final int MAX_STACK_FRAMES = 6;
    private static final long SLOW_TEXT_MUTATION_THRESHOLD_NS = 1_000_000L;
    private static volatile boolean verboseFontLogsEnabled;

    private ForceTextSizeHookInstaller() {
    }

    public static void resetForHotReload() {
        installedPid = -1;
    }

    public static void install(XposedInterface xposed, String packageName, DpisConfigStore store)
            throws ReflectiveOperationException {
        install(xposed, packageName, store,
                FontHookArbitration.resolveDomainPlan(true, true),
                ModernApiCapabilitiesResolver.fromXposed(xposed));
    }

    public static void install(XposedInterface xposed,
                        String packageName,
                        DpisConfigStore store,
                        FontHookArbitration.FontDomainPlan domainPlan)
            throws ReflectiveOperationException {
        install(
                xposed,
                packageName,
                store,
                domainPlan,
                ModernApiCapabilitiesResolver.fromXposed(xposed));
    }

    public static void install(XposedInterface xposed,
                        String packageName,
                        DpisConfigStore store,
                        FontHookArbitration.FontDomainPlan domainPlan,
                        ModernApiCapabilities apiCapabilities)
            throws ReflectiveOperationException {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return;
        }
        synchronized (ForceTextSizeHookInstaller.class) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
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
            apiCapabilities.applyStableHookId(
                            xposed.hook(setTextSizeMethod)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_WITH_UNIT)
                    .intercept(chain -> {
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return chain.proceed();
                        }
                        if (!isTargetPercentActive(targetPercent)) {
                            return chain.proceed();
                        }
                        int unit = (Integer) chain.getArg(0);
                        float size = (Float) chain.getArg(1);
                        if (size <= 0f) {
                            return chain.proceed();
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof TextView textView)) {
                            return chain.proceed();
                        }
                        float originalPx = FontScaleOverride.toPx(
                                unit, size, textView.getResources().getDisplayMetrics());
                        if (shouldKeepCurrentTextViewTarget(textView, originalPx, factor)) {
                            FeedbackDiagnosticRuntimeHotPathEvents.kept(
                                    packageName,
                                    routeNameForTextViewSetTextSize(unit),
                                    "reason=current_target, view=" + textView.getClass().getName()
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent);
                            return null;
                        }
                        int depth = TEXT_VIEW_SET_TEXT_SIZE_DEPTH.get();
                        TEXT_VIEW_SET_TEXT_SIZE_DEPTH.set(depth + 1);
                        Object result;
                        try {
                            result = chain.proceed();
                        } finally {
                            TEXT_VIEW_SET_TEXT_SIZE_DEPTH.set(depth);
                        }
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        if (originalPx <= 0f) {
                            return result;
                        }
                        if (isKnownAppliedTextSize(textView, originalPx, factor)) {
                            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                                    packageName,
                                    routeNameForTextViewSetTextSize(unit),
                                    "reason=known_applied, view=" + textView.getClass().getName()
                                            + ", px=" + originalPx
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent
                            );
                            markAppliedTargetSize(textView, originalPx, factor);
                            return result;
                        }
                        if (unit == TypedValue.COMPLEX_UNIT_SP
                                && isSpTextHandledByResources(textView, factor, domainPlan)) {
                            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                                    packageName,
                                    "textview_sp_rewrite",
                                    "reason=resources_handled, view=" + textView.getClass().getName()
                                            + ", px=" + originalPx
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent
                            );
                            recordResourcesHandledTextSize(textView, originalPx, factor);
                            return result;
                        }
                        boolean shouldForceUnit = shouldForceTextUnit(unit, domainPlan);
                        if (!shouldForceUnit) {
                            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                                    packageName,
                                    routeNameForTextViewSetTextSize(unit),
                                    "reason=domain_disabled, unit=" + unit
                                            + ", view=" + textView.getClass().getName()
                                            + ", px=" + originalPx
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent
                            );
                            recordTextViewBase(textView, originalPx, factor);
                            return result;
                        }
                        float forcedPx = originalPx * factor;
                        if (!shouldApplyTargetSize(textView, forcedPx)) {
                            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                                    packageName,
                                    routeNameForTextViewSetTextSize(unit),
                                    "reason=no_change, unit=" + unit
                                            + ", view=" + textView.getClass().getName()
                                            + ", in=" + originalPx
                                            + ", out=" + forcedPx
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent
                            );
                            return result;
                        }
                        String routeName = routeNameForTextViewSetTextSize(unit);
                        String detail = "unit=" + unit
                                + ", view=" + textView.getClass().getName()
                                + ", in=" + originalPx
                                + ", out=" + forcedPx
                                + ", factor=" + factor
                                + ", percent=" + targetPercent;
                        FeedbackDiagnosticRuntimeHotPathEvents.begin(packageName, routeName, detail);
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            boolean diagnosticCaptureActive =
                                    FeedbackDiagnosticRuntimeTransport.isCaptureActive();
                            long frameworkStartedAt = diagnosticCaptureActive
                                    ? System.nanoTime()
                                    : 0L;
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, forcedPx);
                            long frameworkDurationNs = diagnosticCaptureActive
                                    ? Math.max(0L, System.nanoTime() - frameworkStartedAt)
                                    : 0L;
                            long bookkeepingStartedAt = diagnosticCaptureActive
                                    ? System.nanoTime()
                                    : 0L;
                            recordTextViewBase(textView, originalPx, factor);
                            markAppliedTargetSize(textView, forcedPx, factor);
                            recordTextViewRewrite(textView, originalPx, forcedPx, factor, unit);
                            long bookkeepingDurationNs = diagnosticCaptureActive
                                    ? Math.max(0L, System.nanoTime() - bookkeepingStartedAt)
                                    : 0L;
                            FeedbackDiagnosticRuntimeHotPathEvents.applied(packageName, routeName, detail);
                            recordSlowTextMutationEvidence(
                                    packageName,
                                    routeName,
                                    detail,
                                    frameworkDurationNs,
                                    bookkeepingDurationNs,
                                    diagnosticCaptureActive);
                            bridgeMutationAppliedIfChanged(
                                    xposed,
                                    packageName,
                                    HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_WITH_UNIT,
                                    "textview setTextSize(unit) override applied");
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                            FeedbackDiagnosticRuntimeHotPathEvents.end(packageName, routeName, detail);
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
                        FontDebugStatsReporter.recordUnit(
                                unit,
                                textView.getClass().getName(),
                                textView.getContext());
                        return result;
                    });
            Method setTextSizeFloatMethod = textViewClass.getDeclaredMethod("setTextSize", float.class);
            apiCapabilities.applyStableHookId(
                            xposed.hook(setTextSizeFloatMethod)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_DEFAULT_SP)
                    .intercept(chain -> {
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return chain.proceed();
                        }
                        // Skip when this one-arg setTextSize(float) call is a nested
                        // forwarding call from the two-arg setTextSize(int, float)
                        // overload (observed on OEM ROMs where the unit overload
                        // delegates down to the float one). The depth ThreadLocal is
                        // maintained by the WITH_UNIT hook around its chain.proceed(),
                        // so depth > 0 here is equivalent to seeing >= 2 TextView#setTextSize
                        // frames on the stack, without the per-call getStackTrace() cost.
                        if (isInsideTextViewSetTextSize()) {
                            return chain.proceed();
                        }
                        if (!isTargetPercentActive(targetPercent)) {
                            return chain.proceed();
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof TextView textView)) {
                            return chain.proceed();
                        }
                        float sizeSp = (Float) chain.getArg(0);
                        if (sizeSp <= 0f) {
                            return chain.proceed();
                        }
                        float originalPx = FontScaleOverride.toPx(
                                TypedValue.COMPLEX_UNIT_SP,
                                sizeSp,
                                textView.getResources().getDisplayMetrics());
                        if (shouldKeepCurrentTextViewTarget(textView, originalPx, factor)) {
                            FeedbackDiagnosticRuntimeHotPathEvents.kept(
                                    packageName,
                                    "textview_sp_rewrite",
                                    "reason=current_target, view=" + textView.getClass().getName()
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent);
                            return null;
                        }
                        int depth = TEXT_VIEW_SET_TEXT_SIZE_DEPTH.get();
                        TEXT_VIEW_SET_TEXT_SIZE_DEPTH.set(depth + 1);
                        Object result;
                        try {
                            result = chain.proceed();
                        } finally {
                            TEXT_VIEW_SET_TEXT_SIZE_DEPTH.set(depth);
                        }
                        if (Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                            return result;
                        }
                        if (originalPx <= 0f) {
                            return result;
                        }
                        if (isKnownAppliedTextSize(textView, originalPx, factor)) {
                            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                                    packageName,
                                    "textview_sp_rewrite",
                                    "reason=known_applied, view=" + textView.getClass().getName()
                                            + ", px=" + originalPx
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent
                            );
                            markAppliedTargetSize(textView, originalPx, factor);
                            return result;
                        }
                        if (isSpTextHandledByResources(textView, factor, domainPlan)) {
                            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                                    packageName,
                                    "textview_sp_rewrite",
                                    "reason=resources_handled, view=" + textView.getClass().getName()
                                            + ", px=" + originalPx
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent
                            );
                            recordResourcesHandledTextSize(textView, originalPx, factor);
                            return result;
                        }
                        if (!shouldRewriteDefaultSpTextSize(domainPlan)) {
                            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                                    packageName,
                                    "textview_sp_rewrite",
                                    "reason=domain_disabled, view=" + textView.getClass().getName()
                                            + ", px=" + originalPx
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent
                            );
                            recordTextViewBase(textView, originalPx, factor);
                            return result;
                        }
                        float forcedPx = originalPx * factor;
                        if (!shouldApplyTargetSize(textView, forcedPx)) {
                            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                                    packageName,
                                    "textview_sp_rewrite",
                                    "reason=no_change, view=" + textView.getClass().getName()
                                            + ", in=" + originalPx
                                            + ", out=" + forcedPx
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent
                            );
                            return result;
                        }
                        String detail = "view=" + textView.getClass().getName()
                                + ", in=" + originalPx
                                + ", out=" + forcedPx
                                + ", factor=" + factor
                                + ", percent=" + targetPercent;
                        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                                packageName,
                                "textview_sp_rewrite",
                                detail
                        );
                        INTERNAL_UPDATE.set(Boolean.TRUE);
                        try {
                            boolean diagnosticCaptureActive =
                                    FeedbackDiagnosticRuntimeTransport.isCaptureActive();
                            long frameworkStartedAt = diagnosticCaptureActive
                                    ? System.nanoTime()
                                    : 0L;
                            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, forcedPx);
                            long frameworkDurationNs = diagnosticCaptureActive
                                    ? Math.max(0L, System.nanoTime() - frameworkStartedAt)
                                    : 0L;
                            long bookkeepingStartedAt = diagnosticCaptureActive
                                    ? System.nanoTime()
                                    : 0L;
                            recordTextViewBase(textView, originalPx, factor);
                            markAppliedTargetSize(textView, forcedPx, factor);
                            TextViewFontProvenanceTracker.recordApplied(
                                    textView,
                                    originalPx,
                                    forcedPx,
                                    factor,
                                    TextViewFontProvenanceTracker.Source.TEXTVIEW_SP_REWRITE,
                                    TextViewFontProvenanceTracker.UnitKind.SP);
                            long bookkeepingDurationNs = diagnosticCaptureActive
                                    ? Math.max(0L, System.nanoTime() - bookkeepingStartedAt)
                                    : 0L;
                            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                    packageName,
                                    "textview_sp_rewrite",
                                    detail
                            );
                            recordSlowTextMutationEvidence(
                                    packageName,
                                    "textview_sp_rewrite",
                                    detail,
                                    frameworkDurationNs,
                                    bookkeepingDurationNs,
                                    diagnosticCaptureActive);
                            bridgeMutationAppliedIfChanged(
                                    xposed,
                                    packageName,
                                    HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_DEFAULT_SP,
                                    "textview setTextSize(default sp) override applied");
                        } finally {
                            INTERNAL_UPDATE.set(Boolean.FALSE);
                            FeedbackDiagnosticRuntimeHotPathEvents.end(
                                    packageName,
                                    "textview_sp_rewrite",
                                    detail
                            );
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
            if (shouldInstallCurrentPxTextViewFallbacks(domainPlan)) {
                installTextAppearanceHooks(
                        xposed,
                        textViewClass,
                        factor,
                        targetPercent,
                        packageName,
                        domainPlan,
                        apiCapabilities);
                installTextViewAttachHook(
                        xposed,
                        textViewClass,
                        factor,
                        targetPercent,
                        packageName,
                        domainPlan,
                        apiCapabilities);
            } else {
                logIfChanged(buildFontLogKey(packageName, "textview-current-px-fallback-suppressed"),
                        "DPIS_FONT TextView current-px fallbacks suppressed: reason="
                                + domainPlan.reason);
            }
            if (domainPlan == null || domainPlan.paintFallbackEnabled) {
                installPaintTextSizeHooks(
                        xposed,
                        factor,
                        targetPercent,
                        packageName,
                        apiCapabilities);
            } else {
                // Paint/TextPaint cannot reliably tell whether incoming sizes were already
                // handled by Resources, WebView, or TextView domains, so keep it as a last fallback.
                logIfChanged(buildFontLogKey(packageName, "paint-fallback-suppressed"),
                        "DPIS_FONT Paint/TextPaint fallback suppressed: reason="
                                + domainPlan.reason);
            }
            if (shouldInstallCurrentPxTextViewFallbacks(domainPlan)) {
                installExpressionTextSetTextHook(
                        xposed,
                        textViewClass,
                        factor,
                        targetPercent,
                        packageName,
                        domainPlan,
                        apiCapabilities);
            }
            installedPid = ProcessScopedInstallGate.currentPid();
            DpisLog.i("ForceTextSize hook ready"
                    + ", paintFallback=" + (domainPlan == null
                            || domainPlan.paintFallbackEnabled));
            bridgeLog(xposed, "DPIS_FONT ForceTextSize hook ready: package=" + packageName
                    + ", hookIds="
                    + HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_WITH_UNIT + ","
                    + HOOK_ID_TEXTVIEW_SET_TEXT_SIZE_DEFAULT_SP + ","
                    + HOOK_ID_TEXTVIEW_SET_TEXT + ","
                    + HOOK_ID_TEXTVIEW_ATTACH + ","
                    + HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_CONTEXT + ","
                    + HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_RES + ","
                    + HOOK_ID_PAINT_SET_TEXT_SIZE + ","
                    + HOOK_ID_TEXTPAINT_SET_TEXT_SIZE);
        }
    }

    private static void installPaintTextSizeHooks(XposedInterface xposed,
                                                  float factor,
                                                  Integer targetPercent,
                                                  String packageName,
                                                  ModernApiCapabilities apiCapabilities)
            throws ReflectiveOperationException {
        Method paintSetTextSize = Paint.class.getDeclaredMethod("setTextSize", float.class);
        apiCapabilities.applyStableHookId(
                        xposed.hook(paintSetTextSize)
                                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                        HOOK_ID_PAINT_SET_TEXT_SIZE)
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
                    float currentPx = paint.getTextSize();
                    PaintFallbackContext context = paintFallbackContextProvisional();
                    PaintFallbackDecision decision = resolvePaintFallbackDecision(
                            paint,
                            incoming,
                            currentPx,
                            factor,
                            context);
                    if (decision.action == PaintFallbackAction.WRITE) {
                        // Defer the stack snapshot to the write gate: most
                        // Paint.setTextSize calls resolve to a non-WRITE decision
                        // (already-applied, no-op delta, etc.) for reasons unrelated
                        // to text-layout ownership, so they never need
                        // Thread.currentThread().getStackTrace(). Only when we are
                        // about to write do we re-check whether a stronger domain
                        // (span processing inside a text layout) already owns this
                        // paint size, which may flip the decision back to skip.
                        context = paintFallbackContext(paint, incoming);
                        decision = resolvePaintFallbackDecision(
                                paint,
                                incoming,
                                currentPx,
                                factor,
                                context);
                    }
                    if (decision.action != PaintFallbackAction.WRITE) {
                        if (decision.action == PaintFallbackAction.KEEP) {
                            FeedbackDiagnosticRuntimeHotPathEvents.kept(
                                    packageName,
                                    "paint_text_size_fallback",
                                    "reason=current_target, paint="
                                            + paint.getClass().getName()
                                            + ", factor=" + factor
                                            + ", percent=" + targetPercent);
                            return null;
                        }
                        return chain.proceed();
                    }
                    String detail = "paint=" + paint.getClass().getName()
                            + ", in=" + incoming
                            + ", out=" + decision.adjustedPx
                            + ", factor=" + factor
                            + ", percent=" + targetPercent
                            + context.detailSuffix();
                    FeedbackDiagnosticRuntimeHotPathEvents.begin(
                            packageName,
                            "paint_text_size_fallback",
                            detail
                    );
                    Object result;
                    try {
                        result = chain.proceed(new Object[] {decision.adjustedPx});
                        PaintProvenanceTracker.recordApplied(paint, decision.adjustedPx, factor);
                        FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                packageName,
                                "paint_text_size_fallback",
                                detail
                        );
                        bridgeMutationAppliedIfChanged(
                                xposed,
                                packageName,
                                HOOK_ID_PAINT_SET_TEXT_SIZE,
                                "Paint.setTextSize fallback applied");
                    } finally {
                        FeedbackDiagnosticRuntimeHotPathEvents.end(
                                packageName,
                                "paint_text_size_fallback",
                                detail
                        );
                    }
                    if (verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                        logSampled(buildHotFontLogKey(packageName, "paint-size"),
                                "DPIS_FONT Paint.setTextSize override: in=" + incoming
                                        + ", out=" + decision.adjustedPx
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
            if (textPaintSetTextSize.equals(paintSetTextSize)) {
                return;
            }
            apiCapabilities.applyStableHookId(
                            xposed.hook(textPaintSetTextSize)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_TEXTPAINT_SET_TEXT_SIZE)
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
                        float currentPx = textPaint.getTextSize();
                        PaintFallbackContext context = paintFallbackContextProvisional();
                        PaintFallbackDecision decision = resolvePaintFallbackDecision(
                                textPaint,
                                incoming,
                                currentPx,
                                factor,
                                context);
                        if (decision.action == PaintFallbackAction.WRITE) {
                            // Defer the stack snapshot to the write gate (see the
                            // Paint.setTextSize hook above for the rationale).
                            context = paintFallbackContext(textPaint, incoming);
                            decision = resolvePaintFallbackDecision(
                                    textPaint,
                                    incoming,
                                    currentPx,
                                    factor,
                                    context);
                        }
                        if (decision.action != PaintFallbackAction.WRITE) {
                            if (decision.action == PaintFallbackAction.KEEP) {
                                FeedbackDiagnosticRuntimeHotPathEvents.kept(
                                        packageName,
                                        "textpaint_text_size_fallback",
                                        "reason=current_target, paint="
                                                + textPaint.getClass().getName()
                                                + ", factor=" + factor
                                                + ", percent=" + targetPercent);
                                return null;
                            }
                            return chain.proceed();
                        }
                        String detail = "paint=" + textPaint.getClass().getName()
                                + ", in=" + incoming
                                + ", out=" + decision.adjustedPx
                                + ", factor=" + factor
                                + ", percent=" + targetPercent
                                + context.detailSuffix();
                        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                                packageName,
                                "textpaint_text_size_fallback",
                                detail
                        );
                        Object result;
                        try {
                            result = chain.proceed(new Object[] {decision.adjustedPx});
                            PaintProvenanceTracker.recordApplied(textPaint, decision.adjustedPx, factor);
                            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                    packageName,
                                    "textpaint_text_size_fallback",
                                    detail
                            );
                            bridgeMutationAppliedIfChanged(
                                    xposed,
                                    packageName,
                                    HOOK_ID_TEXTPAINT_SET_TEXT_SIZE,
                                    "TextPaint.setTextSize fallback applied");
                        } finally {
                            FeedbackDiagnosticRuntimeHotPathEvents.end(
                                    packageName,
                                    "textpaint_text_size_fallback",
                                    detail
                            );
                        }
                        if (verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                            logSampled(buildHotFontLogKey(packageName, "textpaint-size"),
                                    "DPIS_FONT TextPaint.setTextSize override: in=" + incoming
                                            + ", out=" + decision.adjustedPx
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

    private static PaintFallbackDecision resolvePaintFallbackDecision(Object paint,
                                                                      float incomingPx,
                                                                      float currentPx,
                                                                      float factor) {
        return resolvePaintFallbackDecision(
                paint,
                incomingPx,
                currentPx,
                factor,
                paintFallbackContext());
    }

    private static PaintFallbackDecision resolvePaintFallbackDecision(Object paint,
                                                                      float incomingPx,
                                                                      float currentPx,
                                                                      float factor,
                                                                      PaintFallbackContext context) {
        return resolvePaintFallbackDecision(
                paint,
                incomingPx,
                currentPx,
                factor,
                context.strongerDomainOwns);
    }

    private static PaintFallbackDecision resolvePaintFallbackDecision(Object paint,
                                                                      float incomingPx,
                                                                      float currentPx,
                                                                      float factor,
                                                                      boolean strongerDomainOwns) {
        PaintProvenanceTracker.Resolution resolution = PaintProvenanceTracker.resolveFallback(
                paint,
                incomingPx,
                currentPx,
                factor,
                strongerDomainOwns);
        if (resolution.action() == PaintProvenanceTracker.Action.WRITE) {
            return PaintFallbackDecision.write(resolution.adjustedPx());
        }
        if (resolution.action() == PaintProvenanceTracker.Action.SKIP) {
            return PaintFallbackDecision.skip(resolution.adjustedPx());
        }
        if (resolution.action() == PaintProvenanceTracker.Action.KEEP) {
            return PaintFallbackDecision.keep(resolution.adjustedPx());
        }
        return PaintFallbackDecision.observe(resolution.adjustedPx());
    }

    public static PaintFallbackDecision resolvePaintFallbackDecisionForTest(Object paint,
                                                                     float incomingPx,
                                                                     float currentPx,
                                                                     float factor,
                                                                     boolean strongerDomainOwns) {
        return resolvePaintFallbackDecision(
                paint,
                incomingPx,
                currentPx,
                factor,
                strongerDomainOwns);
    }

    private static PaintFallbackContext paintFallbackContextProvisional() {
        // Cheap, allocation-free ownership hint used on the fast path. The
        // expensive stack snapshot is deferred to paintFallbackContext(), which
        // is only consulted when the provisional decision is WRITE.
        if (isInsideTextViewSetTextSize()) {
            return new PaintFallbackContext(true, "");
        }
        return new PaintFallbackContext(false, "");
    }

    private static PaintFallbackContext paintFallbackContext() {
        return paintFallbackContext(null, 0f);
    }

    private static PaintFallbackContext paintFallbackContext(Object paint, float incomingPx) {
        if (isInsideTextViewSetTextSize()) {
            return new PaintFallbackContext(true, "");
        }
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        boolean strongerDomainOwns = isPaintSizeOwnedByTextLayout(trace);
        boolean includeCaller = FeedbackDiagnosticRuntimeTransport.isCaptureActive()
                && PaintDiagnosticCallerSampler.shouldCapture(
                        paint != null ? paint.getClass().getName() : "unknown",
                        incomingPx);
        String caller = includeCaller
                ? summarizePaintFallbackStack(trace)
                : "";
        return new PaintFallbackContext(strongerDomainOwns, caller);
    }

    private static void installTextViewAttachHook(XposedInterface xposed,
                                                  Class<?> textViewClass,
                                                  float factor,
                                                  Integer targetPercent,
                                                  String packageName,
                                                  FontHookArbitration.FontDomainPlan domainPlan,
                                                  ModernApiCapabilities apiCapabilities) {
        try {
            Method onAttachedToWindowMethod = findOnAttachedToWindowMethod(textViewClass);
            apiCapabilities.applyStableHookId(
                            xposed.hook(onAttachedToWindowMethod)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_TEXTVIEW_ATTACH)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!isTargetPercentActive(targetPercent)) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof TextView textView)) {
                            return result;
                        }
                        if (shouldKeepCurrentTextViewFallback(textView, factor)) {
                            recordCurrentPxKeptHotPath(
                                    packageName,
                                    textView,
                                    factor,
                                    targetPercent
                            );
                            return result;
                        }
                        String detail = "view=" + textView.getClass().getName()
                                + ", factor=" + factor
                                + ", percent=" + targetPercent;
                        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                                packageName,
                                "textview_current_px_fallback",
                                detail
                        );
                        if (applyTextViewSizeOverride(textView, factor, domainPlan)) {
                            FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                    packageName,
                                    "textview_current_px_fallback",
                                    detail
                            );
                            bridgeMutationAppliedIfChanged(
                                    xposed,
                                    packageName,
                                    HOOK_ID_TEXTVIEW_ATTACH,
                                    "TextView attach fallback applied");
                            if (verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                                logSampled(buildHotFontLogKey(
                                                packageName,
                                                "textview-attach-" + textView.getClass().getName()),
                                        "DPIS_FONT TextView attach override: view="
                                                + textView.getClass().getName()
                                                + ", factor=" + factor
                                                + ", percent=" + targetPercent,
                                        HOT_LOG_INTERVAL);
                            }
                            FontDebugStatsReporter.record(
                                    "textview-attach",
                                    textView.getClass().getName(),
                                    textView.getContext());
                        } else {
                            FeedbackDiagnosticRuntimeHotPathEvents.skipped(
                                    packageName,
                                    "textview_current_px_fallback",
                                    "reason=no_change_or_stronger_provenance, " + detail
                            );
                        }
                        FeedbackDiagnosticRuntimeHotPathEvents.end(
                                packageName,
                                "textview_current_px_fallback",
                                detail
                        );
                        return result;
                    });
            logIfChanged(buildFontLogKey(packageName, "textview-attach-hook"),
                    "DPIS_FONT TextView attach hook ready");
        } catch (Throwable t) {
            logIfChanged(buildFontLogKey(packageName, "textview-attach-hook-skip"),
                    "DPIS_FONT TextView attach hook skipped: "
                            + t.getClass().getSimpleName());
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

    private static void installExpressionTextSetTextHook(XposedInterface xposed,
                                                         Class<?> textViewClass,
                                                         float factor,
                                                         Integer targetPercent,
                                                         String packageName,
                                                         FontHookArbitration.FontDomainPlan domainPlan,
                                                         ModernApiCapabilities apiCapabilities)
            throws ReflectiveOperationException {
        Method setTextMethod = textViewClass.getDeclaredMethod(
                "setText", CharSequence.class, TextView.BufferType.class);
        apiCapabilities.applyStableHookId(
                        xposed.hook(setTextMethod)
                                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                        HOOK_ID_TEXTVIEW_SET_TEXT)
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
                        if (reinforceTextViewTarget(textView, factor, domainPlan)) {
                            recordCurrentPxReinforceHotPath(
                                    packageName,
                                    textView,
                                    factor,
                                    targetPercent
                            );
                            FontDebugStatsReporter.record(
                                    "textview-settext-reinforce",
                                    textView.getClass().getName(),
                                    textView.getContext());
                        } else if (shouldKeepCurrentTextViewFallback(textView, factor)) {
                            recordCurrentPxKeptHotPath(
                                    packageName, textView, factor, targetPercent);
                        }
                        return result;
                    }
                    CharSequence patched = scaleSpans(spanned, factor);
                    if (patched == sourceText) {
                        Object result = chain.proceed();
                        if (reinforceTextViewTarget(textView, factor, domainPlan)) {
                            recordCurrentPxReinforceHotPath(
                                    packageName,
                                    textView,
                                    factor,
                                    targetPercent
                            );
                            FontDebugStatsReporter.record(
                                    "textview-settext-reinforce",
                                    textView.getClass().getName(),
                                    textView.getContext());
                        } else if (shouldKeepCurrentTextViewFallback(textView, factor)) {
                            recordCurrentPxKeptHotPath(
                                    packageName, textView, factor, targetPercent);
                        }
                        return result;
                    }
                    TextView.BufferType bufferType = (TextView.BufferType) chain.getArg(1);
                    String detail = "view=" + textView.getClass().getName()
                            + ", factor=" + factor
                            + ", percent=" + targetPercent
                            + ", length=" + patched.length();
                    FeedbackDiagnosticRuntimeHotPathEvents.begin(
                            packageName,
                            "textview_span_rewrite",
                            detail
                    );
                    INTERNAL_TEXT_UPDATE.set(Boolean.TRUE);
                    try {
                        textView.setText(patched, bufferType);
                        FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                packageName,
                                "textview_span_rewrite",
                                detail
                        );
                        bridgeMutationAppliedIfChanged(
                                xposed,
                                packageName,
                                HOOK_ID_TEXTVIEW_SET_TEXT,
                                "TextView span rewrite applied");
                    } finally {
                        INTERNAL_TEXT_UPDATE.set(Boolean.FALSE);
                        FeedbackDiagnosticRuntimeHotPathEvents.end(
                                packageName,
                                "textview_span_rewrite",
                                detail
                        );
                    }
                    if (reinforceTextViewTarget(textView, factor, domainPlan)) {
                        recordCurrentPxReinforceHotPath(
                                packageName,
                                textView,
                                factor,
                                targetPercent
                        );
                        FontDebugStatsReporter.record(
                                "textview-settext-reinforce",
                                textView.getClass().getName(),
                                textView.getContext());
                    } else if (shouldKeepCurrentTextViewFallback(textView, factor)) {
                        recordCurrentPxKeptHotPath(
                                packageName, textView, factor, targetPercent);
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

    private static boolean reinforceTextViewTarget(
            TextView textView,
            float factor,
            FontHookArbitration.FontDomainPlan domainPlan) {
        if (textView == null) {
            return false;
        }
        TargetTextSize target = LAST_TARGET_TEXT_SIZES.get(textView);
        Float desiredPx = target != null && target.matchesFactor(factor)
                ? target.px
                : null;
        if (desiredPx == null || desiredPx <= 0f) {
            if (!isCommentLikeNode(textView) || !isScaleFactorActive(factor)) {
                return false;
            }
            if (TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                    textView, factor)) {
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
            markAppliedTargetSize(textView, desiredPx, factor);
            return true;
        } finally {
            INTERNAL_UPDATE.set(Boolean.FALSE);
        }
    }

    private static void recordCurrentPxReinforceHotPath(
            String packageName,
            TextView textView,
            float factor,
            Integer targetPercent
    ) {
        String detail = "reason=set_text_reinforce"
                + ", view=" + textView.getClass().getName()
                + ", factor=" + factor
                + ", percent=" + targetPercent;
        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                packageName,
                "textview_current_px_fallback",
                detail
        );
        FeedbackDiagnosticRuntimeHotPathEvents.applied(
                packageName,
                "textview_current_px_fallback",
                detail
        );
        FeedbackDiagnosticRuntimeHotPathEvents.end(
                packageName,
                "textview_current_px_fallback",
                detail
        );
    }

    private static void recordSlowTextMutationEvidence(
            String packageName,
            String routeName,
            String detail,
            long frameworkDurationNs,
            long bookkeepingDurationNs,
            boolean diagnosticCaptureActive
    ) {
        long totalDurationNs = frameworkDurationNs + bookkeepingDurationNs;
        if (totalDurationNs < SLOW_TEXT_MUTATION_THRESHOLD_NS
                || !diagnosticCaptureActive) {
            return;
        }
        FeedbackDiagnosticRuntimeHotPathEvents.event(
                packageName,
                "font",
                routeName,
                "slow_mutation_breakdown",
                detail
                        + ",frameworkUs=" + (frameworkDurationNs / 1_000L)
                        + ",bookkeepingUs=" + (bookkeepingDurationNs / 1_000L)
                        + ",totalUs=" + (totalDurationNs / 1_000L)
        );
    }

    private static void recordCurrentPxKeptHotPath(
            String packageName,
            TextView textView,
            float factor,
            Integer targetPercent
    ) {
        String detail = "reason=current_target"
                + ", view=" + textView.getClass().getName()
                + ", factor=" + factor
                + ", percent=" + targetPercent;
        // Kept is an aggregate-only outcome; do not create begin/end rows for
        // a callback that performed no mutation.
        FeedbackDiagnosticRuntimeHotPathEvents.kept(
                packageName,
                "textview_current_px_fallback",
                detail
        );
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
                                                   String packageName,
                                                   FontHookArbitration.FontDomainPlan domainPlan,
                                                   ModernApiCapabilities apiCapabilities) {
        try {
            Method setTextAppearanceCtx = textViewClass.getDeclaredMethod(
                    "setTextAppearance", android.content.Context.class, int.class);
            apiCapabilities.applyStableHookId(
                            xposed.hook(setTextAppearanceCtx)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_CONTEXT)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!isTargetPercentActive(targetPercent)) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof TextView textView)) {
                            return result;
                        }
                        String detail = "view=" + textView.getClass().getName()
                                + ", factor=" + factor
                                + ", percent=" + targetPercent;
                        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                                packageName,
                                "text_appearance",
                                detail
                        );
                        try {
                            if (applyTextViewSizeOverride(textView, factor, domainPlan)) {
                                FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                        packageName,
                                        "text_appearance",
                                        detail
                                );
                                bridgeMutationAppliedIfChanged(
                                        xposed,
                                        packageName,
                                        HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_CONTEXT,
                                        "TextAppearance(Context,int) fallback applied");
                            }
                        } finally {
                            FeedbackDiagnosticRuntimeHotPathEvents.end(
                                    packageName,
                                    "text_appearance",
                                    detail
                            );
                        }
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
            apiCapabilities.applyStableHookId(
                            xposed.hook(setTextAppearanceRes)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_RES)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!isTargetPercentActive(targetPercent)) {
                            return result;
                        }
                        Object thisObject = chain.getThisObject();
                        if (!(thisObject instanceof TextView textView)) {
                            return result;
                        }
                        String detail = "view=" + textView.getClass().getName()
                                + ", factor=" + factor
                                + ", percent=" + targetPercent;
                        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                                packageName,
                                "text_appearance_int",
                                detail
                        );
                        try {
                            if (applyTextViewSizeOverride(textView, factor, domainPlan)) {
                                FeedbackDiagnosticRuntimeHotPathEvents.applied(
                                        packageName,
                                        "text_appearance_int",
                                        detail
                                );
                                bridgeMutationAppliedIfChanged(
                                        xposed,
                                        packageName,
                                        HOOK_ID_TEXTVIEW_SET_TEXT_APPEARANCE_RES,
                                        "TextAppearance(int) fallback applied");
                            }
                        } finally {
                            FeedbackDiagnosticRuntimeHotPathEvents.end(
                                    packageName,
                                    "text_appearance_int",
                                    detail
                            );
                        }
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
                                                String packageName,
                                                FontHookArbitration.FontDomainPlan domainPlan) {
        try {
            Method onDrawMethod = textViewClass.getDeclaredMethod("onDraw", android.graphics.Canvas.class);
            xposed.hook(onDrawMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (isTargetPercentActive(targetPercent)) {
                            Object thisObject = chain.getThisObject();
                            if (thisObject instanceof TextView textView
                                    && !Boolean.TRUE.equals(INTERNAL_UPDATE.get())) {
                                applyTextViewSizeOverride(textView, factor, domainPlan);
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
            markAppliedTargetSize(textView, desiredPx, factor);
        } finally {
            INTERNAL_UPDATE.set(Boolean.FALSE);
        }
    }

    private static boolean applyTextViewSizeOverride(TextView textView,
                                                     float factor,
                                                     FontHookArbitration.FontDomainPlan domainPlan) {
        float currentPx = textView.getTextSize();
        if (isKnownAppliedTextSize(textView, currentPx, factor)) {
            return false;
        }
        if (TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                textView, factor)) {
            return false;
        }
        float expectedPx = FontFieldRewriteMath.resolveScaledTextSize(
                currentPx, factor, TEXT_VIEW_BASE_TEXT_SIZES, textView);
        if (!shouldApplyTargetSize(textView, expectedPx)) {
            return false;
        }
        INTERNAL_UPDATE.set(Boolean.TRUE);
        try {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, expectedPx);
            markAppliedTargetSize(textView, expectedPx, factor);
            TextViewFontProvenanceTracker.recordApplied(
                    textView,
                    currentPx,
                    expectedPx,
                    factor,
                    TextViewFontProvenanceTracker.Source.TEXTVIEW_CURRENT_PX_FALLBACK,
                    TextViewFontProvenanceTracker.UnitKind.UNKNOWN);
            return true;
        } finally {
            INTERNAL_UPDATE.set(Boolean.FALSE);
        }
    }

    /**
     * Distinguishes a no-op owned by this fallback route from an observation
     * that was intentionally left to a stronger font domain.
     */
    private static boolean shouldKeepCurrentTextViewFallback(
            TextView textView,
            float factor) {
        float currentPx = textView.getTextSize();
        if (isKnownAppliedTextSize(textView, currentPx, factor)) {
            return true;
        }
        if (TextViewFontProvenanceTracker.hasStrongerProvenanceForCurrentPxFallback(
                textView, factor)) {
            return false;
        }
        float expectedPx = FontFieldRewriteMath.resolveScaledTextSize(
                currentPx, factor, TEXT_VIEW_BASE_TEXT_SIZES, textView);
        return expectedPx > 0f && !shouldApplyTargetSize(textView, expectedPx);
    }

    private static boolean shouldApplyTargetSize(TextView textView, float targetPx) {
        if (textView == null || targetPx <= 0f) {
            return false;
        }
        float currentPx = textView.getTextSize();
        return Math.abs(currentPx - targetPx) >= SIZE_EPSILON_PX;
    }

    private static boolean shouldKeepCurrentTextViewTarget(TextView textView,
                                                            float incomingPx,
                                                            float factor) {
        if (textView == null || incomingPx <= 0f) {
            return false;
        }
        float currentPx = textView.getTextSize();
        TargetTextSize target = LAST_TARGET_TEXT_SIZES.get(textView);
        Float targetPx = target != null && target.matchesFactor(factor)
                ? target.px
                : null;
        if (targetPx == null) {
            targetPx = TextViewFontProvenanceTracker.appliedTargetForFactor(textView, factor);
        }
        if (targetPx == null) {
            return false;
        }
        Float basePx = TEXT_VIEW_BASE_TEXT_SIZES.get(textView);
        if (basePx == null
                || (!FontFieldRewriteMath.approximatelyEqual(incomingPx, basePx)
                && !FontFieldRewriteMath.approximatelyEqual(incomingPx, targetPx))) {
            return false;
        }
        return FontMutationScheduler.decide(
                incomingPx,
                currentPx,
                targetPx,
                factor,
                false).action() == FontMutationScheduler.Action.KEEP_CURRENT;
    }

    private static void markAppliedTargetSize(TextView textView, float targetPx, float factor) {
        if (textView == null || targetPx <= 0f) {
            return;
        }
        LAST_TARGET_TEXT_SIZES.put(textView, new TargetTextSize(targetPx, factor));
    }

    private static boolean isKnownAppliedTextSize(TextView textView, float currentPx, float factor) {
        if (textView == null || currentPx <= 0f) {
            return false;
        }
        TargetTextSize target = LAST_TARGET_TEXT_SIZES.get(textView);
        return target != null
                && target.matchesFactor(factor)
                && FontFieldRewriteMath.isKnownScaledTextSize(
                currentPx,
                factor,
                target.px);
    }

    private static void recordTextViewBase(TextView textView, float basePx, float factor) {
        if (textView == null || basePx <= 0f) {
            return;
        }
        if (FontFieldRewriteMath.shouldRecordTextBase(
                basePx,
                factor,
                TEXT_VIEW_BASE_TEXT_SIZES.get(textView),
                targetPx(textView, factor))) {
            TEXT_VIEW_BASE_TEXT_SIZES.put(textView, basePx);
        }
    }

    private static Float targetPx(TextView textView, float factor) {
        TargetTextSize target = LAST_TARGET_TEXT_SIZES.get(textView);
        return target != null && target.matchesFactor(factor) ? target.px : null;
    }

    private static void recordResourcesHandledTextSize(TextView textView, float currentPx, float factor) {
        if (textView == null || currentPx <= 0f || !isScaleFactorActive(factor)) {
            return;
        }
        float inferredBasePx = currentPx / factor;
        if (inferredBasePx > 0f) {
            TEXT_VIEW_BASE_TEXT_SIZES.put(textView, inferredBasePx);
        }
        markAppliedTargetSize(textView, currentPx, factor);
        TextViewFontProvenanceTracker.recordResourcesHandled(textView, currentPx, factor);
    }

    private static void recordTextViewRewrite(TextView textView,
                                              float originalPx,
                                              float forcedPx,
                                              float factor,
                                              int unit) {
        TextViewFontProvenanceTracker.Source source;
        TextViewFontProvenanceTracker.UnitKind unitKind;
        if (unit == TypedValue.COMPLEX_UNIT_SP) {
            source = TextViewFontProvenanceTracker.Source.TEXTVIEW_SP_REWRITE;
            unitKind = TextViewFontProvenanceTracker.UnitKind.SP;
        } else {
            source = TextViewFontProvenanceTracker.Source.TEXTVIEW_ABSOLUTE_REWRITE;
            unitKind = TextViewFontProvenanceTracker.UnitKind.ABSOLUTE;
        }
        TextViewFontProvenanceTracker.recordApplied(
                textView,
                originalPx,
                forcedPx,
                factor,
                source,
                unitKind);
    }

    private static CharSequence scaleSpans(Spanned source, float factor) {
        if (!isScaleFactorActive(factor)) {
            return source;
        }
        FontScaledMarker[] markers = source.getSpans(0, source.length(), FontScaledMarker.class);
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
                FontScaledMarker marker = findMarker(
                        source, markers, start, end, FontScaledMarker.KIND_ABSOLUTE);
                if (marker != null && marker.matchesFactor(factor)
                        && marker.appliedAbsoluteSize == originalSize) {
                    continue;
                }
                if (marker != null && !marker.matchesFactor(factor)) {
                    originalSize = marker.originalAbsoluteSize;
                }
                int scaledSize = FontFieldRewriteMath.scaleAbsoluteSize(originalSize, factor);
                if (scaledSize == originalSize) {
                    continue;
                }
                if (builder == null) {
                    builder = new SpannableStringBuilder(source);
                }
                builder.removeSpan(span);
                if (marker != null) {
                    builder.removeSpan(marker);
                }
                builder.setSpan(new AbsoluteSizeSpan(scaledSize, span.getDip()), start, end, flags);
                builder.setSpan(new FontScaledMarker(
                        FontScaledMarker.KIND_ABSOLUTE,
                        factor,
                        originalSize,
                        scaledSize,
                        0f,
                        0f),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
                FontScaledMarker marker = findMarker(
                        source, markers, start, end, FontScaledMarker.KIND_RELATIVE);
                if (marker != null && marker.matchesFactor(factor)
                        && Math.abs(marker.appliedRelativeSize - originalSize) < 0.0001f) {
                    continue;
                }
                if (marker != null && !marker.matchesFactor(factor)) {
                    originalSize = marker.originalRelativeSize;
                }
                float scaledSize = FontFieldRewriteMath.scaleRelativeSize(originalSize, factor);
                if (Math.abs(scaledSize - originalSize) < 0.0001f) {
                    continue;
                }
                if (builder == null) {
                    builder = new SpannableStringBuilder(source);
                }
                builder.removeSpan(span);
                if (marker != null) {
                    builder.removeSpan(marker);
                }
                builder.setSpan(new RelativeSizeSpan(scaledSize), start, end, flags);
                builder.setSpan(new FontScaledMarker(
                        FontScaledMarker.KIND_RELATIVE,
                        factor,
                        0,
                        0,
                        originalSize,
                        scaledSize),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                changed = true;
            }
        }
        return changed ? builder : source;
    }

    private static FontScaledMarker findMarker(Spanned source,
                                               FontScaledMarker[] markers,
                                               int start,
                                               int end,
                                               int kind) {
        if (markers == null) {
            return null;
        }
        for (FontScaledMarker marker : markers) {
            if (source.getSpanStart(marker) == start
                    && source.getSpanEnd(marker) == end
                    && marker.kind == kind) {
                return marker;
            }
        }
        return null;
    }

    // This bookkeeping span must survive Spannable copies so copied text is not rescaled.
    private static final class FontScaledMarker {
        static final int KIND_ABSOLUTE = 1;
        static final int KIND_RELATIVE = 2;

        final int kind;
        final float factor;
        final int originalAbsoluteSize;
        final int appliedAbsoluteSize;
        final float originalRelativeSize;
        final float appliedRelativeSize;

        FontScaledMarker(int kind,
                         float factor,
                         int originalAbsoluteSize,
                         int appliedAbsoluteSize,
                         float originalRelativeSize,
                         float appliedRelativeSize) {
            this.kind = kind;
            this.factor = factor;
            this.originalAbsoluteSize = originalAbsoluteSize;
            this.appliedAbsoluteSize = appliedAbsoluteSize;
            this.originalRelativeSize = originalRelativeSize;
            this.appliedRelativeSize = appliedRelativeSize;
        }

        boolean matchesFactor(float candidate) {
            return Math.abs(factor - candidate) <= 0.001f;
        }
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }

    private static void bridgeMutationAppliedIfChanged(XposedInterface xposed,
                                                       String packageName,
                                                       String hookId,
                                                       String eventName) {
        bridgeLogIfChanged(
                xposed,
                buildBridgeLogKey(packageName, hookId),
                "DPIS_FONT " + eventName + ": package=" + packageName + ", hookId=" + hookId);
    }

    private static void bridgeLogIfChanged(XposedInterface xposed, String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            bridgeLog(xposed, message);
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

    private static String buildBridgeLogKey(String packageName, String hookId) {
        String pkg = packageName == null ? "unknown" : packageName;
        String id = hookId == null ? "unknown" : hookId;
        return pkg + ":" + BRIDGE_LOG_KEY_PREFIX + ":" + id;
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

    private static boolean isInsideTextViewSetTextSize() {
        return TEXT_VIEW_SET_TEXT_SIZE_DEPTH.get() > 0;
    }

    private static boolean isPaintSizeOwnedByTextLayout(StackTraceElement[] trace) {
        if (trace == null) {
            return false;
        }
        boolean fromSpan = false;
        boolean fromTextLayout = false;
        for (StackTraceElement element : trace) {
            if (element == null) {
                continue;
            }
            String className = element.getClassName();
            if (className == null) {
                continue;
            }
            if (className.startsWith("android.text.style.")) {
                fromSpan = true;
            }
            if (className.startsWith("android.text.MeasuredParagraph")
                    || className.startsWith("android.text.StaticLayout")
                    || className.startsWith("android.text.TextLine")) {
                fromTextLayout = true;
            }
        }
        return fromSpan && fromTextLayout;
    }

    private static String summarizePaintFallbackStack(StackTraceElement[] trace) {
        if (trace == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int added = 0;
        for (StackTraceElement element : trace) {
            if (element == null) {
                continue;
            }
            String className = element.getClassName();
            if (className == null
                    || className.startsWith("java.lang.Thread")
                    || className.startsWith("de.robv.android.xposed")
                    || className.startsWith("io.github.libxposed")
                    || className.startsWith("com.dpis.module.ForceTextSizeHookInstaller")
                    || "android.graphics.Paint".equals(className)
                    || "android.text.TextPaint".equals(className)) {
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

    public static boolean shouldForceTextUnitForTest(int unit,
                                              FontHookArbitration.FontDomainPlan domainPlan) {
        return shouldForceTextUnit(unit, domainPlan);
    }

    private static String routeNameForTextViewSetTextSize(int unit) {
        return unit == TypedValue.COMPLEX_UNIT_SP
                ? "textview_sp_rewrite"
                : "textview_absolute_rewrite";
    }

    private static boolean shouldForceTextUnit(int unit,
                                               FontHookArbitration.FontDomainPlan domainPlan) {
        if (unit == TypedValue.COMPLEX_UNIT_SP) {
            return shouldRewriteDefaultSpTextSize(domainPlan);
        }
        return shouldRewriteAbsoluteTextSize(domainPlan)
                && (unit == TypedValue.COMPLEX_UNIT_PX
                || unit == TypedValue.COMPLEX_UNIT_DIP
                || unit == TypedValue.COMPLEX_UNIT_PT
                || unit == TypedValue.COMPLEX_UNIT_IN
                || unit == TypedValue.COMPLEX_UNIT_MM);
    }

    private static boolean shouldRewriteDefaultSpTextSize(
            FontHookArbitration.FontDomainPlan domainPlan) {
        return domainPlan == null || domainPlan.textViewSpRewriteEnabled;
    }

    public enum PaintFallbackAction {
        WRITE,
        SKIP,
        KEEP,
        OBSERVE
    }

    public static final class PaintFallbackDecision {
        public final PaintFallbackAction action;
        public final float adjustedPx;

        private PaintFallbackDecision(PaintFallbackAction action, float adjustedPx) {
            this.action = action;
            this.adjustedPx = adjustedPx;
        }

        static PaintFallbackDecision write(float adjustedPx) {
            return new PaintFallbackDecision(PaintFallbackAction.WRITE, adjustedPx);
        }

        static PaintFallbackDecision skip(float incomingPx) {
            return new PaintFallbackDecision(PaintFallbackAction.SKIP, incomingPx);
        }

        static PaintFallbackDecision keep(float incomingPx) {
            return new PaintFallbackDecision(PaintFallbackAction.KEEP, incomingPx);
        }

        static PaintFallbackDecision observe(float incomingPx) {
            return new PaintFallbackDecision(PaintFallbackAction.OBSERVE, incomingPx);
        }
    }

    private static final class PaintFallbackContext {
        final boolean strongerDomainOwns;
        final String callerSummary;

        PaintFallbackContext(boolean strongerDomainOwns, String callerSummary) {
            this.strongerDomainOwns = strongerDomainOwns;
            this.callerSummary = callerSummary != null ? callerSummary : "";
        }

        String detailSuffix() {
            return callerSummary.isEmpty() ? "" : ", caller=" + callerSummary;
        }
    }

    private static final class TargetTextSize {
        final float px;
        final float factor;

        TargetTextSize(float px, float factor) {
            this.px = px;
            this.factor = factor;
        }

        boolean matchesFactor(float candidate) {
            return isScaleFactorActive(factor)
                    && isScaleFactorActive(candidate)
                    && Math.abs(factor - candidate) <= 0.001f;
        }
    }

    private static boolean isSpTextHandledByResources(TextView textView,
                                                      float factor,
                                                      FontHookArbitration.FontDomainPlan domainPlan) {
        if (textView == null || domainPlan == null || !domainPlan.resourcesFontEnabled) {
            return false;
        }
        DisplayMetrics metrics = textView.getResources() != null
                ? textView.getResources().getDisplayMetrics()
                : null;
        return metrics != null && FontFieldRewriteMath.isResourcesScaledDensityApplied(
                metrics.density,
                metrics.scaledDensity,
                factor);
    }

    private static boolean shouldRewriteAbsoluteTextSize(
            FontHookArbitration.FontDomainPlan domainPlan) {
        return domainPlan == null || domainPlan.textViewAbsoluteRewriteEnabled;
    }

    private static boolean shouldInstallCurrentPxTextViewFallbacks(
            FontHookArbitration.FontDomainPlan domainPlan) {
        return domainPlan == null || domainPlan.textViewCurrentPxFallbackEnabled;
    }

    private static boolean isTargetPercentActive(Integer targetPercent) {
        return targetPercent != null && targetPercent > 0 && targetPercent != 100;
    }

    private static boolean isScaleFactorActive(float factor) {
        return factor > 0f && factor != 1.0f;
    }

    private static boolean isVerboseFontLogsEnabled(DpisConfigStore store) {
        return store != null && store.isFontDebugOverlayEnabled();
    }

    private static void bridgeLog(XposedInterface xposed, String message) {
        if (xposed == null || (!BuildConfig.DEBUG && !DpisLog.isLoggingEnabled())) {
            return;
        }
        try {
            xposed.log(android.util.Log.INFO, DpisLog.TAG, BRIDGE_LOG_PREFIX + message);
        } catch (Throwable ignored) {
            // Bridge evidence must not affect target app behavior.
        }
    }
}
