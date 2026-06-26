package com.dpis.module;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.libxposed.api.XposedInterface;

final class WebApkRuntimeOwnerBridge {
    static final String CHROME_PACKAGE = "com.android.chrome";
    private static final String ACTIVITY_THREAD_CLASS = "android.app.ActivityThread";
    private static final String WEBAPK_ACTIVITY_CLASS =
            "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity";
    private static final String HOOK_ID_ACTIVITY_ON_CREATE = "webapk_owner_activity_on_create";
    private static final String HOOK_ID_ACTIVITY_ON_RESUME = "webapk_owner_activity_on_resume";
    private static final String HOOK_ID_ACTIVITY_ON_NEW_INTENT = "webapk_owner_activity_on_new_intent";
    private static final String HOOK_ID_ACTIVITY_THREAD_LAUNCH_PREFIX =
            "webapk_owner_activity_thread_launch";
    private static final Map<String, String> LAST_MESSAGES = new ConcurrentHashMap<>();

    private static volatile Method currentActivityThreadMethod;
    private static volatile Method getActivitiesMethod;
    private static volatile boolean lifecycleHooksInstalled;
    private static volatile String activeOwnerPackage;

    private WebApkRuntimeOwnerBridge() {
    }

    static String resolveEffectivePackage(DpiConfigStore store, String carrierPackage) {
        if (!CHROME_PACKAGE.equals(carrierPackage) || store == null) {
            return carrierPackage;
        }
        String owner = currentWebApkOwner();
        if (owner == null) {
            recordUnresolved();
            return carrierPackage;
        }
        DpiConfigStore ownerStore = ownerStore(owner, store);
        if (!hasActiveOwnerConfig(ownerStore, owner)) {
            logIfChanged("unconfigured:" + owner,
                    "DPIS_WEBAPK Chrome carrier owner ignored: carrier="
                            + carrierPackage + ", owner=" + owner + ", reason=owner_not_configured");
            return carrierPackage;
        }
        logIfChanged("owner:" + owner,
                "DPIS_WEBAPK Chrome carrier using WebAPK owner: carrier="
                        + carrierPackage + ", owner=" + owner);
        return owner;
    }

    static DpiConfigStore resolveEffectiveStore(DpiConfigStore store, String effectivePackage) {
        if (WebApkCarrierResolver.isWebApkOwnerPackage(effectivePackage)) {
            return ownerStore(effectivePackage, store);
        }
        return store;
    }

    private static boolean hasActiveOwnerConfig(DpiConfigStore store, String owner) {
        if (store == null || !store.isTargetDpisEnabled(owner)) {
            return false;
        }
        Integer fontScalePercent = store.getTargetFontScalePercent(owner);
        // Empty owner configs intentionally fall back to Chrome instead of shadowing it.
        return store.getTargetViewportSpec(owner).isEnabled()
                || (fontScalePercent != null && fontScalePercent > 0 && fontScalePercent != 100)
                || (store.getTargetTypefaceId(owner) != null
                && !store.getTargetTypefaceId(owner).isBlank());
    }

    private static DpiConfigStore ownerStore(String owner, DpiConfigStore fallbackStore) {
        // WebAPK owner sync runs inside Chrome's app process, so an owner saved as
        // viewport auto must resolve like the normal app-process fallback route.
        return new DpiConfigStore(new RuntimePropertyConfigPreferences(
                owner,
                RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET));
    }

    static void installLifecycleHooks(XposedInterface xposed,
                                      String carrierPackage,
                                      ModernApiCapabilities apiCapabilities) {
        if (WebApkCarrierResolver.isWebApkOwnerPackage(carrierPackage)) {
            return;
        }
        if (!CHROME_PACKAGE.equals(carrierPackage) || xposed == null || lifecycleHooksInstalled) {
            return;
        }
        synchronized (WebApkRuntimeOwnerBridge.class) {
            if (lifecycleHooksInstalled) {
                return;
            }
            try {
                Method onCreate = Activity.class.getDeclaredMethod("onCreate", Bundle.class);
                        apiCapabilities.applyStableHookId(
                                xposed.hook(onCreate)
                                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                                HOOK_ID_ACTIVITY_ON_CREATE)
                        .intercept(chain -> {
                            observeActivity(chain.getThisObject(), null, "onCreate");
                            return chain.proceed();
                        });

                Method onResume = Activity.class.getDeclaredMethod("onResume");
                        apiCapabilities.applyStableHookId(
                                xposed.hook(onResume)
                                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                                HOOK_ID_ACTIVITY_ON_RESUME)
                        .intercept(chain -> {
                            observeActivity(chain.getThisObject(), null, "onResume");
                            return chain.proceed();
                        });

                Method onNewIntent = Activity.class.getDeclaredMethod("onNewIntent", Intent.class);
                apiCapabilities.applyStableHookId(
                                xposed.hook(onNewIntent)
                                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                                HOOK_ID_ACTIVITY_ON_NEW_INTENT)
                        .intercept(chain -> {
                            observeActivity(chain.getThisObject(), chain.getArg(0), "onNewIntent");
                            return chain.proceed();
                        });
                installActivityThreadLaunchHooks(xposed, apiCapabilities);
                lifecycleHooksInstalled = true;
                DpisLog.i("DPIS_WEBAPK lifecycle bridge ready: carrier=" + carrierPackage);
            } catch (Throwable throwable) {
                DpisLog.e("DPIS_WEBAPK lifecycle bridge failed: carrier=" + carrierPackage, throwable);
            }
        }
    }

    static String currentWebApkOwnerForTest(String activityText) {
        if (activityText == null || !activityText.contains(WEBAPK_ACTIVITY_CLASS)) {
            return null;
        }
        return WebApkCarrierResolver.ownerPackageFromText(activityText);
    }

    static String observeActivityForTest(String activityClassName,
                                         String activityIntentText,
                                         String methodIntentText,
                                         String sourceTag) {
        observeActivity(activityClassName, activityIntentText, methodIntentText, sourceTag);
        return activeOwnerPackage;
    }

    static void resetForTest() {
        activeOwnerPackage = null;
        lifecycleHooksInstalled = false;
        LAST_MESSAGES.clear();
    }

    static boolean hasActiveOwnerConfigForTest(DpiConfigStore store, String owner) {
        return hasActiveOwnerConfig(store, owner);
    }

    private static String currentWebApkOwner() {
        String cachedOwner = activeOwnerPackage;
        if (WebApkCarrierResolver.isWebApkOwnerPackage(cachedOwner)) {
            return cachedOwner;
        }
        Object activityThread = currentActivityThread();
        if (activityThread == null) {
            return null;
        }
        Object activities = invokeNoArg(activityThread, getActivitiesMethod(activityThread));
        if (activities == null) {
            activities = readField(activityThread, "mActivities");
        }
        if (activities == null) {
            return null;
        }
        String owner = currentWebApkOwnerFromActivities(activities);
        if (owner != null) {
            return owner;
        }
        owner = currentWebApkOwnerForTest(String.valueOf(activities));
        if (owner != null) {
            return owner;
        }
        return currentWebApkOwnerForTest(String.valueOf(activityThread));
    }

    private static String currentWebApkOwnerFromActivities(Object activities) {
        if (!(activities instanceof Map<?, ?> map)) {
            return null;
        }
        for (Object record : map.values()) {
            String owner = currentWebApkOwnerFromActivityRecord(record);
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }

    private static String currentWebApkOwnerFromActivityRecord(Object record) {
        if (isStopped(record)) {
            return null;
        }
        Object intent = readField(record, "intent");
        String owner = WebApkCarrierResolver.ownerPackageFromText(String.valueOf(intent));
        if (owner != null && isWebApkActivityRecord(record, intent)) {
            return owner;
        }
        Object activity = readField(record, "activity");
        if (activity == null || !WEBAPK_ACTIVITY_CLASS.equals(activity.getClass().getName())) {
            return null;
        }
        intent = invokeNoArg(activity, findNoArgMethod(activity.getClass(), "getIntent"));
        owner = WebApkCarrierResolver.ownerPackageFromText(String.valueOf(intent));
        if (owner != null) {
            return owner;
        }
        return WebApkCarrierResolver.ownerPackageFromText(String.valueOf(record));
    }

    private static void observeActivity(Object activityObject, Object methodIntent, String sourceTag) {
        if (!(activityObject instanceof Activity activity)) {
            return;
        }
        boolean webApkOwnerObserved = observeActivity(
                activity.getClass().getName(),
                String.valueOf(invokeNoArg(activity, findNoArgMethod(activity.getClass(), "getIntent"))),
                String.valueOf(methodIntent),
                sourceTag);
        if (webApkOwnerObserved) {
            syncActivityResources(activity, sourceTag);
        }
    }

    private static void installActivityThreadLaunchHooks(XposedInterface xposed,
                                                         ModernApiCapabilities apiCapabilities)
            throws ClassNotFoundException {
        Class<?> activityThreadClass = Class.forName(ACTIVITY_THREAD_CLASS);
        for (Method method : activityThreadClass.getDeclaredMethods()) {
            String methodName = method.getName();
            if (!"performLaunchActivity".equals(methodName)
                    && !"handleLaunchActivity".equals(methodName)) {
                continue;
            }
            apiCapabilities.applyStableHookId(
                            xposed.hook(method)
                                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                            HOOK_ID_ACTIVITY_THREAD_LAUNCH_PREFIX + "#" + method.toGenericString())
                    .intercept(chain -> {
                        observeLaunchArgs(methodName, chain.getArgs());
                        return chain.proceed();
                    });
        }
    }

    private static void observeLaunchArgs(String methodName, Iterable<?> args) {
        if (args == null) {
            return;
        }
        for (Object arg : args) {
            String owner = currentWebApkOwnerFromActivityRecord(arg);
            if (owner != null) {
                cacheOwner(owner, "launch owner cached: source=" + methodName);
                return;
            }
        }
    }

    private static boolean observeActivity(String activityClassName,
                                        String activityIntentText,
                                        String methodIntentText,
                                        String sourceTag) {
        if (activityClassName == null) {
            return false;
        }
        String owner = WebApkCarrierResolver.ownerPackageFromText(methodIntentText);
        if (owner == null) {
            owner = WebApkCarrierResolver.ownerPackageFromText(activityIntentText);
        }
        if (WebApkCarrierResolver.isWebApkOwnerPackage(owner)
                && activityClassName.contains(".webapps.")) {
            return cacheOwner(owner, "lifecycle owner cached: source=" + sourceTag
                    + ", activity=" + activityClassName);
        }
        if (!WEBAPK_ACTIVITY_CLASS.equals(activityClassName)) {
            clearActiveOwnerIfNeeded(activityClassName, sourceTag);
            return false;
        }
        if (!WebApkCarrierResolver.isWebApkOwnerPackage(owner)) {
            logIfChanged("lifecycle-missing-owner:" + sourceTag,
                    "DPIS_WEBAPK lifecycle owner missing: source=" + sourceTag
                            + ", activity=" + activityClassName);
            return false;
        }
        return cacheOwner(owner, "lifecycle owner cached: source=" + sourceTag);
    }

    private static boolean cacheOwner(String owner, String detail) {
        if (!WebApkCarrierResolver.isWebApkOwnerPackage(owner)) {
            return false;
        }
        activeOwnerPackage = owner;
        logIfChanged("owner-cache:" + owner + ":" + detail,
                "DPIS_WEBAPK " + detail + ", owner=" + owner);
        return true;
    }

    private static void syncActivityResources(Activity activity, String sourceTag) {
        String owner = activeOwnerPackage;
        if (activity == null || !WebApkCarrierResolver.isWebApkOwnerPackage(owner)) {
            return;
        }
        try {
            Resources resources = activity.getResources();
            if (resources == null) {
                return;
            }
            DpiConfigStore ownerStore = ownerStore(owner, null);
            Configuration config = resources.getConfiguration();
            ResourcesManagerHookInstaller.applyResourceOverrides(
                    config, ownerStore, owner, "WebApkOwnerBridge(" + sourceTag + ")");
            DisplayMetrics metrics = resources.getDisplayMetrics();
            ResourcesImplHookInstaller.applyDensityOverride(owner, config, metrics, ownerStore);
            logIfChanged("activity-sync:" + owner + ":" + sourceTag + ":"
                            + config.screenWidthDp + ":" + config.densityDpi,
                    "DPIS_WEBAPK activity resources synced: owner=" + owner
                            + ", source=" + sourceTag
                            + ", widthDp=" + config.screenWidthDp
                            + ", densityDpi=" + config.densityDpi);
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_WEBAPK activity resources sync failed: owner=" + owner
                    + ", source=" + sourceTag, throwable);
        }
    }

    private static void clearActiveOwnerIfNeeded(String activityClassName, String sourceTag) {
        String owner = activeOwnerPackage;
        if (owner == null) {
            return;
        }
        activeOwnerPackage = null;
        logIfChanged("lifecycle-clear:" + activityClassName,
                "DPIS_WEBAPK lifecycle owner cleared: source=" + sourceTag
                        + ", owner=" + owner + ", activity=" + activityClassName);
    }

    private static boolean isWebApkActivityRecord(Object record, Object intent) {
        String recordText = String.valueOf(record);
        String intentText = String.valueOf(intent);
        return recordText.contains(WEBAPK_ACTIVITY_CLASS)
                || intentText.contains("webapp://webapk-")
                || intentText.contains(WebApkCarrierResolver.WEBAPK_PACKAGE_EXTRA);
    }

    private static boolean isStopped(Object record) {
        Object stopped = readField(record, "stopped");
        return stopped instanceof Boolean && (Boolean) stopped;
    }

    private static Object currentActivityThread() {
        try {
            Method method = currentActivityThreadMethod;
            if (method == null) {
                synchronized (WebApkRuntimeOwnerBridge.class) {
                    method = currentActivityThreadMethod;
                    if (method == null) {
                        method = Class.forName(ACTIVITY_THREAD_CLASS)
                                .getDeclaredMethod("currentActivityThread");
                        method.setAccessible(true);
                        currentActivityThreadMethod = method;
                    }
                }
            }
            return method.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method getActivitiesMethod(Object activityThread) {
        if (activityThread == null) {
            return null;
        }
        try {
            Method method = getActivitiesMethod;
            if (method == null) {
                synchronized (WebApkRuntimeOwnerBridge.class) {
                    method = getActivitiesMethod;
                    if (method == null) {
                        method = activityThread.getClass().getDeclaredMethod("getActivities");
                        method.setAccessible(true);
                        getActivitiesMethod = method;
                    }
                }
            }
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeNoArg(Object target, Method method) {
        if (target == null || method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method findNoArgMethod(Class<?> type, String methodName) {
        if (type == null || methodName == null) {
            return null;
        }
        try {
            Method method = type.getMethod(methodName);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object readField(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return null;
        }
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void logIfChanged(String key, String message) {
        String previous = LAST_MESSAGES.put(key, message);
        if (!message.equals(previous)) {
            DpisLog.i(message);
        }
    }

    private static void recordUnresolved() {
        logIfChanged("chrome-owner-unresolved",
                "DPIS_WEBAPK Chrome carrier owner unresolved: activeOwner=" + activeOwnerPackage
                        + ", activityThreadAvailable=" + (currentActivityThread() != null));
    }

}
