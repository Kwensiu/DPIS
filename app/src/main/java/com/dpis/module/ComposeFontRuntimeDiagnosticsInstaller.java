package com.dpis.module;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;

import com.dpis.module.fonts.ComposeFontRuntimeClassifier;
import com.dpis.module.fonts.FontDebugStatsReporter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.github.libxposed.api.XposedInterface;

/**
 * Observes Compose-heavy roots to feed the resources_font event gate; it does
 * NOT scale fonts itself. There is no setTextSize / fontScale / scaledDensity
 * write anywhere in this class -- it only calls {@link ResourcesFontScheduler}
 * observe / suppression checks so the read path can avoid double scaling.
 *
 * Common misconception: because this installer is gated on
 * {@code resourcesFontEnabled}, resources_font looks like the only route that
 * scales Compose text. It is not. Compose draws through android.graphics.Paint
 * (AndroidParagraph -> TextPaint, which does not override setTextSize), so the
 * Paint/TextView draw-rewrite routes scale Compose text independently of
 * resources_font. With resources_font OFF in compat mode, Compose still scales
 * via those routes; resources_font only adds value-rewrite (the
 * Configuration.fontScale / scaledDensity values an app may read directly).
 */
final class ComposeFontRuntimeDiagnosticsInstaller {
    static final long LAYOUT_EVALUATE_THROTTLE_MS = 500L;
    private static final String FONT_LOG_KEY_PREFIX = "font";
    private static final ConcurrentMap<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();
    private static final Object LOCK = new Object();
    private static final Map<Activity, ActivityState> ACTIVITY_STATES = new WeakHashMap<>();
    private static volatile boolean callbacksRegistered;
    private static volatile boolean applicationRetryHookInstalled;
    private static volatile boolean activityFallbackHooksInstalled;

    private ComposeFontRuntimeDiagnosticsInstaller() {
    }

    static void install(XposedInterface xposed,
                        String packageName,
                        DpisConfigStore store,
                        FontHookArbitration.FontDomainPlan domainPlan,
                        String hookDomains,
                        String hookDomainSource) {
        if (store == null || domainPlan == null || !domainPlan.resourcesFontEnabled) {
            return;
        }
        Integer targetPercent = store.getTargetFontScalePercent(packageName);
        if (targetPercent == null || targetPercent <= 0) {
            return;
        }
        installActivityFallbackHooks(xposed, packageName, store, domainPlan,
                hookDomains, hookDomainSource);
        Application application = currentApplication();
        if (shouldDeferRegistration(application, callbacksRegistered)) {
            logIfChanged(buildFontLogKey(packageName, "compose-runtime-current-app-missing"),
                    "DPIS_FONT Compose runtime diagnostics deferred: currentApplication unavailable"
                            + ", package=" + packageName
                            + ", hookDomains=" + safeValue(hookDomains)
                            + ", hookDomainSource=" + safeValue(hookDomainSource));
            installApplicationRetryHook(xposed, packageName, store, domainPlan,
                    hookDomains, hookDomainSource);
            return;
        }
        registerCallbacks(application, packageName, store, domainPlan,
                hookDomains, hookDomainSource);
    }

    static boolean shouldInstall(HookExecutionPlan plan) {
        return plan != null
                && plan.fontDomainPlan != null
                && plan.fontDomainPlan.resourcesFontEnabled;
    }

    static boolean shouldDeferRegistration(Application application, boolean alreadyRegistered) {
        return application == null && !alreadyRegistered;
    }

    static boolean shouldEvaluateFromLayout(long nowMs,
                                            long lastLayoutEvaluationAtMs) {
        return lastLayoutEvaluationAtMs <= 0
                || nowMs - lastLayoutEvaluationAtMs >= LAYOUT_EVALUATE_THROTTLE_MS;
    }

    static Float resolveTargetFactor(Integer targetPercent) {
        return targetPercent == null || targetPercent <= 0 ? null : targetPercent / 100f;
    }

    static Float resolveCurrentTargetFactorForTest(DpisConfigStore store, String packageName) {
        return resolveCurrentTargetFactor(store, packageName);
    }

    static boolean shouldSkipForTargetSuppression(String packageName, float targetFactor) {
        return ResourcesFontScheduler.isPackageTargetSuppressed(packageName, targetFactor);
    }

    private static void registerCallbacks(Application application,
                                          String packageName,
                                          DpisConfigStore store,
                                          FontHookArbitration.FontDomainPlan domainPlan,
                                          String hookDomains,
                                          String hookDomainSource) {
        if (application == null) {
            return;
        }
        synchronized (LOCK) {
            if (callbacksRegistered) {
                return;
            }
            application.registerActivityLifecycleCallbacks(new DiagnosticsCallbacks(
                    packageName,
                    store,
                    domainPlan,
                    hookDomains,
                    hookDomainSource));
            callbacksRegistered = true;
        }
        logIfChanged(buildFontLogKey(packageName, "compose-runtime-ready"),
                "DPIS_FONT Compose runtime diagnostics ready: package=" + packageName
                        + ", hookDomains=" + safeValue(hookDomains)
                        + ", hookDomainSource=" + safeValue(hookDomainSource));
    }

    private static void evaluate(Activity activity,
                                 String packageName,
                                 DpisConfigStore store,
                                 FontHookArbitration.FontDomainPlan domainPlan,
                                 String hookDomains,
                                 String hookDomainSource) {
        if (activity == null) {
            return;
        }
        Float targetFactor = resolveCurrentTargetFactor(store, packageName);
        if (targetFactor == null) {
            detachLayoutListener(activity);
            return;
        }
        if (shouldSkipForTargetSuppression(packageName, targetFactor)) {
            detachLayoutListener(activity);
            return;
        }
        View root = decorRoot(activity);
        if (root == null) {
            return;
        }
        Resources resources = root.getResources();
        if (resources == null) {
            return;
        }
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        if (configuration == null || metrics == null) {
            return;
        }

        boolean composeHeavy = ComposeFontRuntimeClassifier.isComposeHeavy(new AndroidViewTreeNode(root));
        String scopeKey = buildScopeKey(activity, root);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                domainPlan,
                configuration.fontScale,
                metrics.density,
                metrics.scaledDensity,
                targetFactor,
                composeHeavy);
        ResourcesFontScheduler.observe(
                packageName,
                scopeKey,
                resources,
                evidence,
                configuration.fontScale,
                targetFactor,
                System.currentTimeMillis());
        String activityClass = activity.getClass().getName();
        String rootClass = root.getClass().getName();
        String stateKey = buildStateKey(
                rootClass,
                composeHeavy,
                evidence.resourcesHandled,
                configuration.fontScale,
                metrics.density,
                metrics.scaledDensity);
        ActivityState state = stateFor(activity);
        if (stateKey.equals(state.lastStateKey)) {
            return;
        }
        state.lastStateKey = stateKey;

        DpisLog.i("DPIS_FONT Compose runtime observed: package=" + packageName
                + ", activity=" + activityClass
                + ", root=" + rootClass
                + ", scope=" + scopeKey
                + ", composeHeavy=" + composeHeavy
                + ", resourcesHandled=" + evidence.resourcesHandled
                + ", resourcesFontDomainEnabled=" + evidence.resourcesFontDomainEnabled
                + ", fontScaleMatches=" + evidence.fontScaleMatches
                + ", scaledDensityRatioMatches=" + evidence.scaledDensityRatioMatches
                + ", scaledDensityRatio=" + evidence.scaledDensityRatio
                + ", fontScale=" + configuration.fontScale
                + ", density=" + metrics.density
                + ", scaledDensity=" + metrics.scaledDensity
                + ", targetFactor=" + targetFactor
                + ", hookDomains=" + safeValue(hookDomains)
                + ", hookDomainSource=" + safeValue(hookDomainSource));
        FontDebugStatsReporter.record(
                evidence.resourcesHandled ? "compose-resources-handled" : "compose-resources-observed",
                rootClass,
                activity);
    }

    private static void attachLayoutListener(Activity activity,
                                             String packageName,
                                             DpisConfigStore store,
                                             FontHookArbitration.FontDomainPlan domainPlan,
                                             String hookDomains,
                                             String hookDomainSource) {
        Float targetFactor = resolveCurrentTargetFactor(store, packageName);
        if (targetFactor == null) {
            cleanup(activity);
            return;
        }
        if (shouldSkipForTargetSuppression(packageName, targetFactor)) {
            detachLayoutListener(activity);
            return;
        }
        View root = decorRoot(activity);
        if (root == null) {
            return;
        }
        ActivityState state = stateFor(activity);
        if (state.listener != null && state.listenerRoot == root) {
            return;
        }
        removeLayoutListener(state);
        ViewTreeObserver.OnGlobalLayoutListener listener = () -> {
            long nowMs = System.currentTimeMillis();
            if (!shouldEvaluateFromLayout(nowMs, state.lastLayoutEvaluationAtMs)) {
                return;
            }
            state.lastLayoutEvaluationAtMs = nowMs;
            evaluate(
                    activity,
                    packageName,
                    store,
                    domainPlan,
                    hookDomains,
                    hookDomainSource);
        };
        root.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        state.listener = listener;
        state.listenerRoot = root;
    }

    private static void detachLayoutListener(Activity activity) {
        synchronized (LOCK) {
            ActivityState state = ACTIVITY_STATES.get(activity);
            if (state != null) {
                removeLayoutListener(state);
            }
        }
    }

    private static void cleanup(Activity activity) {
        synchronized (LOCK) {
            ActivityState state = ACTIVITY_STATES.remove(activity);
            if (state != null) {
                removeLayoutListener(state);
            }
        }
    }

    private static ActivityState stateFor(Activity activity) {
        synchronized (LOCK) {
            ActivityState state = ACTIVITY_STATES.get(activity);
            if (state == null) {
                state = new ActivityState();
                ACTIVITY_STATES.put(activity, state);
            }
            return state;
        }
    }

    private static void removeLayoutListener(ActivityState state) {
        if (state == null || state.listener == null || state.listenerRoot == null) {
            return;
        }
        ViewTreeObserver observer = state.listenerRoot.getViewTreeObserver();
        if (observer != null && observer.isAlive()) {
            observer.removeOnGlobalLayoutListener(state.listener);
        }
        state.listener = null;
        state.listenerRoot = null;
    }

    private static View decorRoot(Activity activity) {
        Window window = activity.getWindow();
        return window == null ? null : window.getDecorView();
    }

    private static Application currentApplication() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentApplication = activityThread.getDeclaredMethod("currentApplication");
            Object app = currentApplication.invoke(null);
            return app instanceof Application ? (Application) app : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Float resolveCurrentTargetFactor(DpisConfigStore store, String packageName) {
        if (store == null) {
            return null;
        }
        return resolveTargetFactor(store.getTargetFontScalePercent(packageName));
    }

    private static void installApplicationRetryHook(XposedInterface xposed,
                                                    String packageName,
                                                    DpisConfigStore store,
                                                    FontHookArbitration.FontDomainPlan domainPlan,
                                                    String hookDomains,
                                                    String hookDomainSource) {
        if (xposed == null || applicationRetryHookInstalled) {
            return;
        }
        synchronized (LOCK) {
            if (applicationRetryHookInstalled) {
                return;
            }
            applicationRetryHookInstalled = true;
        }
        try {
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            xposed.hook(attach)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof Application application) {
                            registerCallbacks(application, packageName, store, domainPlan,
                                    hookDomains, hookDomainSource);
                        }
                        return result;
                    });
            Method onCreate = Application.class.getDeclaredMethod("onCreate");
            xposed.hook(onCreate)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof Application application) {
                            registerCallbacks(application, packageName, store, domainPlan,
                                    hookDomains, hookDomainSource);
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Compose runtime diagnostics retry hook ready for " + packageName);
        } catch (Throwable throwable) {
            synchronized (LOCK) {
                applicationRetryHookInstalled = false;
            }
            DpisLog.e("DPIS_FONT Compose runtime diagnostics retry hook failed for "
                    + packageName, throwable);
        }
    }

    private static void installActivityFallbackHooks(XposedInterface xposed,
                                                     String packageName,
                                                     DpisConfigStore store,
                                                     FontHookArbitration.FontDomainPlan domainPlan,
                                                     String hookDomains,
                                                     String hookDomainSource) {
        if (xposed == null || activityFallbackHooksInstalled) {
            return;
        }
        synchronized (LOCK) {
            if (activityFallbackHooksInstalled) {
                return;
            }
            activityFallbackHooksInstalled = true;
        }
        try {
            Method onResume = Activity.class.getDeclaredMethod("onResume");
            xposed.hook(onResume)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof Activity activity) {
                            evaluate(activity, packageName, store, domainPlan,
                                    hookDomains, hookDomainSource);
                            attachLayoutListener(activity, packageName, store, domainPlan,
                                    hookDomains, hookDomainSource);
                        }
                        return result;
                    });

            Method onPause = Activity.class.getDeclaredMethod("onPause");
            xposed.hook(onPause)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof Activity activity) {
                            detachLayoutListener(activity);
                        }
                        return result;
                    });

            Method onStop = Activity.class.getDeclaredMethod("onStop");
            xposed.hook(onStop)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof Activity activity) {
                            detachLayoutListener(activity);
                        }
                        return result;
                    });

            Method onDestroy = Activity.class.getDeclaredMethod("onDestroy");
            xposed.hook(onDestroy)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof Activity activity) {
                            cleanup(activity);
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Compose runtime diagnostics activity fallback hooks ready for "
                    + packageName);
        } catch (Throwable throwable) {
            synchronized (LOCK) {
                activityFallbackHooksInstalled = false;
            }
            DpisLog.e("DPIS_FONT Compose runtime diagnostics activity fallback hooks failed for "
                    + packageName, throwable);
        }
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

    private static String buildStateKey(String rootClass,
                                        boolean composeHeavy,
                                        boolean resourcesHandled,
                                        float fontScale,
                                        float density,
                                        float scaledDensity) {
        return rootClass
                + "|" + composeHeavy
                + "|" + resourcesHandled
                + "|" + fontScale
                + "|" + density
                + "|" + scaledDensity;
    }

    private static String buildScopeKey(Activity activity, View root) {
        String activityClass = activity == null ? "unknown" : activity.getClass().getName();
        int rootId = root == null ? 0 : System.identityHashCode(root);
        return activityClass + "#" + Integer.toHexString(rootId);
    }

    private static String safeValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private static final class DiagnosticsCallbacks
            implements Application.ActivityLifecycleCallbacks {
        private final String packageName;
        private final DpisConfigStore store;
        private final FontHookArbitration.FontDomainPlan domainPlan;
        private final String hookDomains;
        private final String hookDomainSource;

        DiagnosticsCallbacks(String packageName,
                             DpisConfigStore store,
                             FontHookArbitration.FontDomainPlan domainPlan,
                             String hookDomains,
                             String hookDomainSource) {
            this.packageName = packageName;
            this.store = store;
            this.domainPlan = domainPlan;
            this.hookDomains = hookDomains;
            this.hookDomainSource = hookDomainSource;
        }

        @Override
        public void onActivityResumed(Activity activity) {
            evaluate(activity, packageName, store, domainPlan, hookDomains, hookDomainSource);
            attachLayoutListener(activity, packageName, store, domainPlan,
                    hookDomains, hookDomainSource);
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            cleanup(activity);
        }

        @Override
        public void onActivityCreated(Activity activity, android.os.Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
            detachLayoutListener(activity);
        }

        @Override
        public void onActivityStopped(Activity activity) {
            detachLayoutListener(activity);
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, android.os.Bundle outState) {
        }
    }

    private static final class ActivityState {
        String lastStateKey;
        long lastLayoutEvaluationAtMs;
        View listenerRoot;
        ViewTreeObserver.OnGlobalLayoutListener listener;
    }

    private static final class AndroidViewTreeNode
            implements ComposeFontRuntimeClassifier.ViewTreeNode {
        private final View view;

        AndroidViewTreeNode(View view) {
            this.view = view;
        }

        @Override
        public String className() {
            return view == null ? null : view.getClass().getName();
        }

        @Override
        public List<? extends ComposeFontRuntimeClassifier.ViewTreeNode> children() {
            if (!(view instanceof ViewGroup group)) {
                return List.of();
            }
            int childCount = group.getChildCount();
            if (childCount <= 0) {
                return List.of();
            }
            List<AndroidViewTreeNode> children = new ArrayList<>(childCount);
            for (int i = 0; i < childCount; i++) {
                View child = group.getChildAt(i);
                if (child != null) {
                    children.add(new AndroidViewTreeNode(child));
                }
            }
            return children;
        }
    }
}
