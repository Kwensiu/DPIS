package com.dpis.module;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContentProvider;
import android.content.pm.ProviderInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.libxposed.api.XposedInterface;

final class FlutterSettingsFontHookInstaller {
    private static final String BRIDGE_LOG_PREFIX = "DPIS ";
    private static final String MESSAGE_BUILDER_CLASS = "io.flutter.embedding.engine.systemchannels.SettingsChannel$MessageBuilder";
    private static final String SETTINGS_CHANNEL_CLASS = "io.flutter.embedding.engine.systemchannels.SettingsChannel";
    private static final String FLUTTER_VIEW_CLASS = "io.flutter.embedding.android.FlutterView";
    private static final String FLUTTER_FRAGMENT_CLASS = "io.flutter.embedding.android.FlutterFragment";
    private static final String FLUTTER_JNI_CLASS = "io.flutter.embedding.engine.FlutterJNI";
    private static final String SETTINGS_CHANNEL_NAME = "flutter/settings";
    private static final String TEXT_SCALE_FACTOR_KEY = "textScaleFactor";
    private static final Pattern TEXT_SCALE_FACTOR_PATTERN = Pattern.compile(
            "(\"" + TEXT_SCALE_FACTOR_KEY + "\"\\s*:\\s*)(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)");
    private static final AtomicBoolean CLASS_LOADER_HOOK_INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean BASE_DEX_FIND_CLASS_HOOK_INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean APPLICATION_ATTACH_HOOK_INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean APPLICATION_CREATE_HOOK_INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean CONTENT_PROVIDER_ATTACH_HOOK_INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean LOADED_APK_CLASS_LOADER_HOOK_INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean FLUTTER_VIEW_ATTACH_BRIDGE_INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean ACTIVITY_RESUME_SCAN_INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean VIEW_ROOT_SCAN_INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean ACTIVE_ACTIVITY_SCAN_SCHEDULED = new AtomicBoolean();
    private static final AtomicBoolean ACTIVE_ACTIVITY_SCAN_THREAD_STARTED = new AtomicBoolean();
    private static final Set<String> HOOKED_CLASSES = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<Integer> RESENT_SETTINGS_VIEW_IDS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final float EPSILON = 0.0001f;

    private static final AtomicBoolean APP_CLASSLOADER_RETRY_ATTEMPTED = new AtomicBoolean();

    private static volatile XposedInterface storedXposed;
    private static volatile DpiConfigStore storedStore;
    private static volatile FontHookArbitration.FontDomainPlan storedDomainPlan;
    private static volatile String storedPackageName;

    private FlutterSettingsFontHookInstaller() {
    }

    static void retryWithAppClassLoader(XposedInterface xposed,
            String packageName,
            DpiConfigStore store,
            FontHookArbitration.FontDomainPlan domainPlan,
            ClassLoader appClassLoader) {
        bridgeProbe("DPIS_FONT flutter-retry-inner enter: package=" + packageName
                + ", xposed=" + (xposed != null)
                + ", store=" + (store != null)
                + ", classLoader=" + appClassLoader
                + ", domainPlan=" + (domainPlan != null)
                + ", flutterSettings=" + (domainPlan != null && domainPlan.flutterSettingsEnabled));
        if (xposed == null || store == null || appClassLoader == null
                || domainPlan == null || !domainPlan.flutterSettingsEnabled) {
            bridgeProbe("DPIS_FONT flutter-retry-inner exit: null guard failed");
            return;
        }
        if (!APP_CLASSLOADER_RETRY_ATTEMPTED.compareAndSet(false, true)) {
            bridgeProbe("DPIS_FONT flutter-retry-inner exit: already attempted");
            return;
        }
        FontScaleOverride.Result fontScale = FontScaleOverride.resolve(store, packageName, 1.0f);
        if (fontScale.targetPercent == null || fontScale.targetPercent <= 0
                || Math.abs(fontScale.effective - 1.0f) < EPSILON) {
            bridgeProbe("DPIS_FONT flutter-retry-inner exit: inactive scale"
                    + ", targetPercent=" + fontScale.targetPercent
                    + ", effective=" + fontScale.effective);
            return;
        }
        bridgeProbe("DPIS_FONT Flutter semantic app-classloader retry: package=" + packageName
                + ", classloader=" + appClassLoader
                + ", percent=" + fontScale.targetPercent
                + ", targetScale=" + fontScale.effective);
        probeClassLoaderCapability(appClassLoader, packageName);
        tryHookMessageBuilder(xposed, packageName, fontScale.effective, fontScale.targetPercent,
                appClassLoader);
        tryHookSettingsChannel(packageName, fontScale.effective, fontScale.targetPercent,
                appClassLoader);
        tryHookFlutterView(xposed, packageName, fontScale.effective, fontScale.targetPercent,
                appClassLoader);
        tryHookFlutterFragment(xposed, packageName, fontScale.effective, fontScale.targetPercent,
                appClassLoader);
        tryHookFlutterJni(xposed, packageName, fontScale.effective, fontScale.targetPercent,
                appClassLoader);
    }

    private static void probeClassLoaderCapability(ClassLoader classLoader, String packageName) {
        String[] targets = {
                FLUTTER_VIEW_CLASS,
                MESSAGE_BUILDER_CLASS,
                SETTINGS_CHANNEL_CLASS,
                FLUTTER_JNI_CLASS
        };
        for (String target : targets) {
            try {
                Class<?> found = Class.forName(target, false, classLoader);
                bridgeProbe("DPIS_FONT classloader-probe FOUND: " + target
                        + ", loader=" + found.getClassLoader()
                        + ", package=" + packageName);
            } catch (ClassNotFoundException e) {
                bridgeProbe("DPIS_FONT classloader-probe NOT_FOUND: " + target
                        + ", classLoader=" + classLoader
                        + ", package=" + packageName);
            } catch (Throwable t) {
                bridgeProbe("DPIS_FONT classloader-probe ERROR: " + target
                        + ", error=" + t.getClass().getName() + ": " + t.getMessage()
                        + ", package=" + packageName);
            }
        }
    }

    private static void tryExtractFlutterViewClassLoader(View root, String source) {
        if (root == null) {
            return;
        }
        try {
            View flutterView = findFlutterViewInTree(root);
            if (flutterView != null) {
                ClassLoader cl = flutterView.getClass().getClassLoader();
                bridgeProbe("DPIS_FONT app-classloader-source: visible-flutter-view"
                        + ", source=" + source
                        + ", viewClass=" + flutterView.getClass().getName()
                        + ", classloader=" + cl);
                callRetryWithClassLoader(cl, "visible-flutter-view-" + source);
            }
        } catch (Throwable t) {
            bridgeProbe("DPIS_FONT app-classloader-source: visible-flutter-view ERROR"
                    + ", source=" + source
                    + ", error=" + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    private static View findFlutterViewInTree(View root) {
        if (root == null) {
            return null;
        }
        String className = root.getClass().getName();
        if (className.contains("Flutter") || className.contains("flutter")) {
            return root;
        }
        if (root instanceof android.view.ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findFlutterViewInTree(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void callRetryWithClassLoader(ClassLoader classLoader, String source) {
        XposedInterface xposed = storedXposed;
        DpiConfigStore store = storedStore;
        FontHookArbitration.FontDomainPlan domainPlan = storedDomainPlan;
        String packageName = storedPackageName;
        if (xposed == null || store == null || domainPlan == null || packageName == null) {
            bridgeProbe("DPIS_FONT app-classloader-source: " + source
                    + " retry SKIPPED: stored refs null"
                    + ", xposed=" + (xposed != null)
                    + ", store=" + (store != null)
                    + ", domainPlan=" + (domainPlan != null)
                    + ", packageName=" + packageName);
            return;
        }
        bridgeProbe("DPIS_FONT app-classloader-source: " + source
                + " calling retryWithAppClassLoader"
                + ", classloader=" + classLoader
                + ", package=" + packageName);
        retryWithAppClassLoader(xposed, packageName, store, domainPlan, classLoader);
    }

    static void install(XposedInterface xposed,
            String packageName,
            DpiConfigStore store,
            FontHookArbitration.FontDomainPlan domainPlan) {
        DpisLog.i("DPIS_FONT Flutter settings install entry: package=" + packageName
                + ", hasXposed=" + (xposed != null)
                + ", hasStore=" + (store != null)
                + ", hasDomainPlan=" + (domainPlan != null)
                + ", flutterSettings="
                + (domainPlan != null && domainPlan.flutterSettingsEnabled));
        if (xposed == null) {
            DpisLog.i("DPIS_FONT Flutter settings install skipped: missing xposed for "
                    + packageName);
            return;
        }
        if (store == null) {
            DpisLog.i("DPIS_FONT Flutter settings install skipped: missing store for "
                    + packageName);
            return;
        }
        if (domainPlan == null || !domainPlan.flutterSettingsEnabled) {
            DpisLog.i("DPIS_FONT Flutter settings install skipped: disabled domain for "
                    + packageName);
            return;
        }
        FontScaleOverride.Result fontScale = FontScaleOverride.resolve(store, packageName, 1.0f);
        if (fontScale.targetPercent == null || fontScale.targetPercent <= 0
                || Math.abs(fontScale.effective - 1.0f) < EPSILON) {
            DpisLog.i("DPIS_FONT Flutter settings install skipped: inactive target for "
                    + packageName + ", targetPercent=" + fontScale.targetPercent
                    + ", effective=" + fontScale.effective);
            return;
        }
        DpisLog.i("DPIS_FONT Flutter settings install active: package=" + packageName
                + ", percent=" + fontScale.targetPercent
                + ", targetScale=" + fontScale.effective);
        bridgeProbe("DPIS_FONT Flutter semantic install active: package=" + packageName
                + ", percent=" + fontScale.targetPercent
                + ", targetScale=" + fontScale.effective);
        storedXposed = xposed;
        storedStore = store;
        storedDomainPlan = domainPlan;
        storedPackageName = packageName;
        installLoadedApkClassLoaderHook(xposed, packageName, fontScale.effective,
                fontScale.targetPercent);
        installContentProviderAttachClassLoaderHook(xposed, packageName, fontScale.effective,
                fontScale.targetPercent);
        installApplicationAttachClassLoaderHook(xposed, packageName, fontScale.effective,
                fontScale.targetPercent);
        installApplicationCreateClassLoaderHook(xposed, packageName, fontScale.effective,
                fontScale.targetPercent);
        installLoadedClassHook(xposed, packageName, fontScale.effective, fontScale.targetPercent);
        installBaseDexFindClassHook(xposed, packageName, fontScale.effective,
                fontScale.targetPercent);
        tryHookMessageBuilder(xposed, packageName, fontScale.effective, fontScale.targetPercent,
                Thread.currentThread().getContextClassLoader());
        tryHookMessageBuilder(xposed, packageName, fontScale.effective, fontScale.targetPercent,
                ClassLoader.getSystemClassLoader());
        tryHookSettingsChannel(packageName, fontScale.effective, fontScale.targetPercent,
                Thread.currentThread().getContextClassLoader());
        tryHookSettingsChannel(packageName, fontScale.effective, fontScale.targetPercent,
                ClassLoader.getSystemClassLoader());
        tryHookFlutterView(xposed, packageName, fontScale.effective, fontScale.targetPercent,
                Thread.currentThread().getContextClassLoader());
        tryHookFlutterView(xposed, packageName, fontScale.effective, fontScale.targetPercent,
                ClassLoader.getSystemClassLoader());
        tryHookFlutterFragment(xposed, packageName, fontScale.effective,
                fontScale.targetPercent, Thread.currentThread().getContextClassLoader());
        tryHookFlutterFragment(xposed, packageName, fontScale.effective,
                fontScale.targetPercent, ClassLoader.getSystemClassLoader());
        tryHookFlutterJni(xposed, packageName, fontScale.effective, fontScale.targetPercent,
                Thread.currentThread().getContextClassLoader());
        tryHookFlutterJni(xposed, packageName, fontScale.effective, fontScale.targetPercent,
                ClassLoader.getSystemClassLoader());
        installFlutterViewAttachBridge(xposed, packageName, fontScale.effective,
                fontScale.targetPercent);
    }

    private static void installLoadedClassHook(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (!CLASS_LOADER_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method method = ClassLoader.class.getDeclaredMethod(
                    "loadClass", String.class, boolean.class);
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (result instanceof Class<?> loadedClass
                                && isMessageBuilderClass(loadedClass.getName())) {
                            hookMessageBuilderClass(xposed, packageName, targetFontScale,
                                    targetPercent, loadedClass);
                        } else if (result instanceof Class<?> loadedSettingsClass
                                && isSettingsChannelClass(loadedSettingsClass.getName())) {
                            hookSettingsChannelClass(packageName, targetFontScale,
                                    targetPercent, loadedSettingsClass);
                        } else if (result instanceof Class<?> loadedFlutterViewClass
                                && isFlutterViewClass(loadedFlutterViewClass.getName())) {
                            hookFlutterViewClass(xposed, packageName, targetFontScale,
                                    targetPercent, loadedFlutterViewClass);
                        } else if (result instanceof Class<?> loadedFlutterJniClass
                                && isFlutterJniClass(loadedFlutterJniClass.getName())) {
                            hookFlutterJniClass(xposed, packageName, targetFontScale,
                                    targetPercent, loadedFlutterJniClass);
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter settings class-loader hook ready for " + packageName);
            bridgeProbe("DPIS_FONT Flutter semantic class-loader hook ready for " + packageName);
        } catch (Throwable throwable) {
            CLASS_LOADER_HOOK_INSTALLED.set(false);
            DpisLog.e("DPIS_FONT Flutter settings class-loader hook failed for "
                    + packageName, throwable);
        }
    }

    private static void installBaseDexFindClassHook(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (xposed == null || !BASE_DEX_FIND_CLASS_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> baseDexClassLoader = Class.forName("dalvik.system.BaseDexClassLoader",
                    false, ClassLoader.getSystemClassLoader());
            Method method = baseDexClassLoader.getDeclaredMethod("findClass", String.class);
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        List<Object> args = chain.getArgs();
                        String requestedName = args != null && !args.isEmpty()
                                && args.get(0) instanceof String name ? name : null;
                        if (result instanceof Class<?> loadedClass
                                && isFlutterSemanticClass(loadedClass.getName())) {
                            bridgeProbe("DPIS_FONT Flutter semantic BaseDex findClass hit: package="
                                    + packageName + ", requested=" + requestedName
                                    + ", class=" + loadedClass.getName()
                                    + ", loader=" + loadedClass.getClassLoader());
                            hookFlutterSemanticClass(xposed, packageName, targetFontScale,
                                    targetPercent, loadedClass);
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter BaseDex findClass hook ready for " + packageName);
            bridgeProbe("DPIS_FONT Flutter semantic BaseDex findClass hook ready for "
                    + packageName);
        } catch (Throwable throwable) {
            BASE_DEX_FIND_CLASS_HOOK_INSTALLED.set(false);
            DpisLog.e("DPIS_FONT Flutter BaseDex findClass hook failed for "
                    + packageName, throwable);
        }
    }

    private static void installApplicationAttachClassLoaderHook(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (xposed == null || !APPLICATION_ATTACH_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method method = Application.class.getDeclaredMethod("attach", Context.class);
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        bridgeProbe("DPIS_FONT app-classloader-source: application-attach FIRED"
                                + ", package=" + packageName);
                        Object result = chain.proceed();
                        Object contextObject = chain.getArgs() != null && !chain.getArgs().isEmpty()
                                ? chain.getArgs().get(0)
                                : null;
                        if (contextObject instanceof Context context) {
                            ClassLoader classLoader = context.getClassLoader();
                            bridgeProbe("DPIS_FONT app-classloader-source: application-attach"
                                    + ", classloader=" + classLoader
                                    + ", package=" + packageName);
                            installSemanticHooksFromClassLoader(xposed, packageName,
                                    targetFontScale, targetPercent, classLoader,
                                    "application-attach");
                            tryHookFlutterView(xposed, packageName, targetFontScale,
                                    targetPercent, classLoader);
                            tryHookFlutterFragment(xposed, packageName, targetFontScale,
                                    targetPercent, classLoader);
                            callRetryWithClassLoader(classLoader, "application-attach");
                        } else {
                            bridgeProbe("DPIS_FONT app-classloader-source: application-attach"
                                    + " context NOT instanceof Context"
                                    + ", contextObject=" + contextObject
                                    + ", args=" + chain.getArgs());
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter application classloader hook ready for " + packageName);
            bridgeProbe("DPIS_FONT Flutter semantic application classloader hook ready for "
                    + packageName);
        } catch (Throwable throwable) {
            APPLICATION_ATTACH_HOOK_INSTALLED.set(false);
            DpisLog.e("DPIS_FONT Flutter application classloader hook failed for "
                    + packageName, throwable);
        }
    }

    private static void installApplicationCreateClassLoaderHook(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (xposed == null || !APPLICATION_CREATE_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method method = Application.class.getDeclaredMethod("onCreate");
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        bridgeProbe("DPIS_FONT app-classloader-source: application-onCreate FIRED"
                                + ", package=" + packageName);
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof Application application) {
                            Context context = application.getApplicationContext();
                            ClassLoader classLoader = context != null
                                    ? context.getClassLoader()
                                    : application.getClassLoader();
                            bridgeProbe("DPIS_FONT app-classloader-source: application-onCreate"
                                    + ", classloader=" + classLoader
                                    + ", package=" + packageName);
                            installSemanticHooksFromClassLoader(xposed, packageName,
                                    targetFontScale, targetPercent, classLoader,
                                    "application-onCreate");
                            tryHookFlutterView(xposed, packageName, targetFontScale,
                                    targetPercent, classLoader);
                            tryHookFlutterFragment(xposed, packageName, targetFontScale,
                                    targetPercent, classLoader);
                            tryHookFlutterJni(xposed, packageName, targetFontScale,
                                    targetPercent, classLoader);
                            callRetryWithClassLoader(classLoader, "application-onCreate");
                        } else {
                            bridgeProbe("DPIS_FONT app-classloader-source: application-onCreate"
                                    + " thisObject NOT Application"
                                    + ", thisObject=" + thisObject);
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter application onCreate classloader hook ready for "
                    + packageName);
            bridgeProbe("DPIS_FONT Flutter semantic application onCreate classloader hook ready for "
                    + packageName);
        } catch (Throwable throwable) {
            APPLICATION_CREATE_HOOK_INSTALLED.set(false);
            DpisLog.e("DPIS_FONT Flutter application onCreate classloader hook failed for "
                    + packageName, throwable);
        }
    }

    private static void installContentProviderAttachClassLoaderHook(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (xposed == null
                || !CONTENT_PROVIDER_ATTACH_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method method = ContentProvider.class.getDeclaredMethod("attachInfo",
                    Context.class, ProviderInfo.class);
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        bridgeProbe("DPIS_FONT app-classloader-source: content-provider-attach FIRED"
                                + ", package=" + packageName);
                        Object result = chain.proceed();
                        Object contextObject = chain.getArgs() != null && !chain.getArgs().isEmpty()
                                ? chain.getArgs().get(0)
                                : null;
                        if (contextObject instanceof Context context) {
                            ClassLoader classLoader = context.getClassLoader();
                            bridgeProbe("DPIS_FONT app-classloader-source: content-provider-attach"
                                    + ", classloader=" + classLoader
                                    + ", package=" + packageName);
                            installSemanticHooksFromClassLoader(xposed, packageName,
                                    targetFontScale, targetPercent, classLoader,
                                    "content-provider-attach");
                            tryHookFlutterView(xposed, packageName, targetFontScale,
                                    targetPercent, classLoader);
                            tryHookFlutterFragment(xposed, packageName, targetFontScale,
                                    targetPercent, classLoader);
                            tryHookFlutterJni(xposed, packageName, targetFontScale,
                                    targetPercent, classLoader);
                            callRetryWithClassLoader(classLoader, "content-provider-attach");
                        } else {
                            bridgeProbe("DPIS_FONT app-classloader-source: content-provider-attach"
                                    + " context NOT instanceof Context"
                                    + ", contextObject=" + contextObject
                                    + ", args=" + chain.getArgs());
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter content provider classloader hook ready for "
                    + packageName);
            bridgeProbe("DPIS_FONT Flutter semantic content provider classloader hook ready for "
                    + packageName);
        } catch (Throwable throwable) {
            CONTENT_PROVIDER_ATTACH_HOOK_INSTALLED.set(false);
            DpisLog.e("DPIS_FONT Flutter content provider classloader hook failed for "
                    + packageName, throwable);
        }
    }

    private static void installLoadedApkClassLoaderHook(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (xposed == null || !LOADED_APK_CLASS_LOADER_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> loadedApkClass = Class.forName("android.app.LoadedApk", false,
                    ClassLoader.getSystemClassLoader());
            Method method = loadedApkClass.getDeclaredMethod("getClassLoader");
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        bridgeProbe("DPIS_FONT app-classloader-source: loaded-apk-getClassLoader FIRED"
                                + ", package=" + packageName);
                        Object result = chain.proceed();
                        if (result instanceof ClassLoader classLoader) {
                            bridgeProbe("DPIS_FONT app-classloader-source: loaded-apk-getClassLoader"
                                    + ", classloader=" + classLoader
                                    + ", package=" + packageName);
                            installSemanticHooksFromClassLoader(xposed, packageName,
                                    targetFontScale, targetPercent, classLoader,
                                    "loaded-apk-getClassLoader");
                            tryHookFlutterView(xposed, packageName, targetFontScale,
                                    targetPercent, classLoader);
                            tryHookFlutterFragment(xposed, packageName, targetFontScale,
                                    targetPercent, classLoader);
                            tryHookFlutterJni(xposed, packageName, targetFontScale,
                                    targetPercent, classLoader);
                            callRetryWithClassLoader(classLoader, "loaded-apk-getClassLoader");
                        } else {
                            bridgeProbe("DPIS_FONT app-classloader-source: loaded-apk-getClassLoader"
                                    + " result NOT ClassLoader"
                                    + ", result=" + result
                                    + ", package=" + packageName);
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter loadedApk classloader hook ready for " + packageName);
            bridgeProbe("DPIS_FONT Flutter semantic loadedApk classloader hook ready for "
                    + packageName);
        } catch (Throwable throwable) {
            LOADED_APK_CLASS_LOADER_HOOK_INSTALLED.set(false);
            DpisLog.e("DPIS_FONT Flutter loadedApk classloader hook failed for "
                    + packageName, throwable);
        }
    }

    private static void tryHookMessageBuilder(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(MESSAGE_BUILDER_CLASS, false, classLoader);
            hookMessageBuilderClass(xposed, packageName, targetFontScale, targetPercent, clazz);
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter settings immediate hook failed for "
                    + packageName, throwable);
        }
    }

    private static void tryHookFlutterView(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(FLUTTER_VIEW_CLASS, false, classLoader);
            hookFlutterViewClass(xposed, packageName, targetFontScale, targetPercent, clazz);
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter view immediate hook failed for "
                    + packageName, throwable);
        }
    }

    private static void tryHookFlutterFragment(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(FLUTTER_FRAGMENT_CLASS, false, classLoader);
            hookFlutterFragmentClass(xposed, packageName, targetFontScale, targetPercent, clazz);
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter fragment immediate hook failed for "
                    + packageName, throwable);
        }
    }

    private static void tryHookSettingsChannel(String packageName,
            float targetFontScale,
            int targetPercent,
            ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(SETTINGS_CHANNEL_CLASS, false, classLoader);
            hookSettingsChannelClass(packageName, targetFontScale, targetPercent, clazz);
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter SettingsChannel immediate observe failed for "
                    + packageName, throwable);
        }
    }

    private static void tryHookFlutterJni(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            Class<?> clazz = Class.forName(FLUTTER_JNI_CLASS, false, classLoader);
            hookFlutterJniClass(xposed, packageName, targetFontScale, targetPercent, clazz);
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT FlutterJNI immediate hook failed for "
                    + packageName, throwable);
        }
    }

    private static void hookFlutterSemanticClass(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        if (isMessageBuilderClass(clazz.getName())) {
            hookMessageBuilderClass(xposed, packageName, targetFontScale, targetPercent, clazz);
        } else if (isSettingsChannelClass(clazz.getName())) {
            hookSettingsChannelClass(packageName, targetFontScale, targetPercent, clazz);
        } else if (isFlutterViewClass(clazz.getName())) {
            hookFlutterViewClass(xposed, packageName, targetFontScale, targetPercent, clazz);
        } else if (isFlutterFragmentClass(clazz.getName())) {
            hookFlutterFragmentClass(xposed, packageName, targetFontScale, targetPercent, clazz);
        } else if (isFlutterJniClass(clazz.getName())) {
            hookFlutterJniClass(xposed, packageName, targetFontScale, targetPercent, clazz);
        }
    }

    private static void hookMessageBuilderClass(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            Class<?> clazz) {
        if (xposed == null || clazz == null || !isMessageBuilderClass(clazz.getName())) {
            return;
        }
        String key = clazz.getName() + "@"
                + System.identityHashCode(clazz.getClassLoader()) + "@"
                + System.identityHashCode(clazz);
        if (!HOOKED_CLASSES.add(key)) {
            return;
        }
        boolean hookedAny = false;
        for (Method method : clazz.getDeclaredMethods()) {
            if (isSetTextScaleFactorMethod(method)) {
                hookSetTextScaleFactor(xposed, packageName, targetFontScale, targetPercent, method);
                hookedAny = true;
            } else if (isSetDisplayMetricsMethod(method)) {
                hookSetDisplayMetrics(xposed, packageName, targetFontScale, targetPercent, method);
                hookedAny = true;
            }
        }
        if (hookedAny) {
            DpisLog.i("DPIS_FONT Flutter settings hook ready: class="
                    + clazz.getName() + ", percent=" + targetPercent);
        } else {
            DpisLog.i("DPIS_FONT Flutter settings hook skipped: no supported methods in "
                    + clazz.getName());
        }
    }

    private static void hookFlutterViewClass(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            Class<?> clazz) {
        if (xposed == null || clazz == null || !isFlutterViewClass(clazz.getName())) {
            return;
        }
        String key = clazz.getName() + "@"
                + System.identityHashCode(clazz.getClassLoader()) + "@"
                + System.identityHashCode(clazz);
        if (!HOOKED_CLASSES.add(key)) {
            return;
        }
        try {
            Method attachMethod = clazz.getDeclaredMethod("attachToFlutterEngine",
                    Class.forName("io.flutter.embedding.engine.FlutterEngine",
                            false, clazz.getClassLoader()));
            xposed.hook(attachMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        List<Object> args = chain.getArgs();
                        if (args != null && !args.isEmpty()) {
                            installSemanticHooksFromFlutterObject(xposed, packageName,
                                    targetFontScale, targetPercent, args.get(0),
                                    "flutter-view-engine-arg");
                        }
                        installSemanticHooksFromFlutterObject(xposed, packageName,
                                targetFontScale, targetPercent, chain.getThisObject(),
                                "flutter-view-attach-method");
                        resendFlutterUserSettings(chain.getThisObject(), packageName,
                                targetFontScale, targetPercent);
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter view attach hook ready for " + packageName);
            bridgeProbe("DPIS_FONT Flutter semantic view attach hook ready for " + packageName);
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter view attach hook failed for " + packageName,
                    throwable);
        }
    }

    private static void hookFlutterFragmentClass(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            Class<?> clazz) {
        if (xposed == null || clazz == null || !isFlutterFragmentClass(clazz.getName())) {
            return;
        }
        String key = clazz.getName() + "@"
                + System.identityHashCode(clazz.getClassLoader()) + "@"
                + System.identityHashCode(clazz);
        if (!HOOKED_CLASSES.add(key)) {
            return;
        }
        try {
            Method method = clazz.getDeclaredMethod("onViewCreated", View.class,
                    Class.forName("android.os.Bundle", false, clazz.getClassLoader()));
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object viewObject = chain.getArgs() != null && !chain.getArgs().isEmpty()
                                ? chain.getArgs().get(0)
                                : null;
                        if (viewObject instanceof View view) {
                            bridgeProbe("DPIS_FONT Flutter semantic fragment view created: package="
                                    + packageName + ", source=flutter-fragment-onViewCreated"
                                    + ", view=" + view.getClass().getName());
                            scanFlutterViews(view, xposed, packageName, targetFontScale,
                                    targetPercent, "flutter-fragment-onViewCreated");
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter fragment semantic hook ready for " + packageName);
            bridgeProbe("DPIS_FONT Flutter semantic fragment view hook ready for " + packageName);
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter fragment semantic hook failed for "
                    + packageName, throwable);
        }
    }

    private static void installFlutterViewAttachBridge(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (xposed == null || !FLUTTER_VIEW_ATTACH_BRIDGE_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method method = View.class.getDeclaredMethod("onAttachedToWindow");
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object view = chain.getThisObject();
                        if (view != null && isFlutterViewClass(view.getClass().getName())) {
                            installSemanticHooksFromFlutterObject(xposed, packageName,
                                    targetFontScale, targetPercent, view,
                                    "flutter-view-attached");
                            resendFlutterUserSettings(view, packageName, targetFontScale,
                                    targetPercent);
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter view semantic attach bridge ready for " + packageName);
            bridgeProbe("DPIS_FONT Flutter semantic attach bridge ready for " + packageName);
        } catch (Throwable throwable) {
            FLUTTER_VIEW_ATTACH_BRIDGE_INSTALLED.set(false);
            DpisLog.e("DPIS_FONT Flutter view semantic attach bridge failed for "
                    + packageName, throwable);
        }
    }

    private static void installActivityResumeFlutterViewScan(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (xposed == null || !ACTIVITY_RESUME_SCAN_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method method = Activity.class.getDeclaredMethod("onResume");
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        bridgeProbe("DPIS_HOOK_CALLBACK_PROBE fired: method=Activity.onResume"
                                + ", package=" + packageName
                                + ", thisObject=" + chain.getThisObject());
                        Object result = chain.proceed();
                        Object thisObject = chain.getThisObject();
                        if (thisObject instanceof Activity activity) {
                            View decorView = activity.getWindow() != null
                                    ? activity.getWindow().getDecorView()
                                    : null;
                            scanFlutterViews(decorView,
                                    xposed, packageName, targetFontScale, targetPercent,
                                    "activity-resume");
                            tryExtractFlutterViewClassLoader(decorView, "activity-resume");
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter activity resume semantic scan ready for " + packageName);
            bridgeProbe("DPIS_FONT Flutter semantic activity scan ready for " + packageName);
        } catch (Throwable throwable) {
            ACTIVITY_RESUME_SCAN_INSTALLED.set(false);
            DpisLog.e("DPIS_FONT Flutter activity resume semantic scan failed for "
                    + packageName, throwable);
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    private static void installViewRootFlutterViewScan(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (xposed == null || !VIEW_ROOT_SCAN_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> viewRootImplClass = Class.forName("android.view.ViewRootImpl", false,
                    ClassLoader.getSystemClassLoader());
            Method method = viewRootImplClass.getDeclaredMethod("performTraversals");
            final AtomicBoolean viewRootProbeLogged = new AtomicBoolean();
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (viewRootProbeLogged.compareAndSet(false, true)) {
                            bridgeProbe("DPIS_HOOK_CALLBACK_PROBE fired: method=ViewRootImpl.performTraversals"
                                    + ", package=" + packageName);
                        }
                        Object result = chain.proceed();
                        Object root = readField(chain.getThisObject(), "mView");
                        if (root instanceof View view) {
                            scanFlutterViews(view, xposed, packageName, targetFontScale,
                                    targetPercent, "view-root");
                            tryExtractFlutterViewClassLoader(view, "view-root");
                        }
                        return result;
                    });
            DpisLog.i("DPIS_FONT Flutter view-root semantic scan ready for " + packageName);
            bridgeProbe("DPIS_FONT Flutter semantic view-root scan ready for " + packageName);
        } catch (Throwable throwable) {
            VIEW_ROOT_SCAN_INSTALLED.set(false);
            DpisLog.e("DPIS_FONT Flutter view-root semantic scan failed for "
                    + packageName, throwable);
        }
    }

    private static void scheduleActiveActivityFlutterViewScans(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (!ACTIVE_ACTIVITY_SCAN_SCHEDULED.compareAndSet(false, true)) {
            return;
        }
        try {
            Handler handler = new Handler(Looper.getMainLooper());
            long[] delays = { 500L, 1500L, 3500L, 7000L, 12000L };
            for (long delay : delays) {
                handler.postDelayed(() -> {
                    bridgeProbe("DPIS_FONT Flutter semantic active activity scan runnable entered: package="
                            + packageName + ", source=active-activity-" + delay + "ms");
                    scanActiveActivities(xposed, packageName,
                            targetFontScale, targetPercent,
                            "active-activity-" + delay + "ms");
                },
                        delay);
            }
            bridgeProbe("DPIS_FONT Flutter semantic active activity scans scheduled: package="
                    + packageName);
        } catch (Throwable throwable) {
            ACTIVE_ACTIVITY_SCAN_SCHEDULED.set(false);
            bridgeProbe("DPIS_FONT Flutter semantic active activity scans schedule failed: package="
                    + packageName + ", error=" + throwable.getClass().getName()
                    + ": " + throwable.getMessage());
        }
    }

    private static void startActiveActivityFlutterViewProbeThread(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (!ACTIVE_ACTIVITY_SCAN_THREAD_STARTED.compareAndSet(false, true)) {
            return;
        }
        try {
            Thread thread = new Thread(() -> {
                bridgeProbe("DPIS_FONT Flutter semantic active activity probe thread entered: package="
                        + packageName);
                long[] delays = { 750L, 2000L, 4000L, 8000L, 14000L };
                for (long delay : delays) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        bridgeProbe("DPIS_FONT Flutter semantic active activity probe thread interrupted: package="
                                + packageName + ", error="
                                + interruptedException.getClass().getName());
                        return;
                    }
                    bridgeProbe("DPIS_FONT Flutter semantic active activity probe tick: package="
                            + packageName + ", delayMs=" + delay);
                    scanActiveActivities(xposed, packageName, targetFontScale, targetPercent,
                            "active-activity-thread-" + delay + "ms");
                }
            }, "dpis-flutter-active-activity-probe");
            thread.setDaemon(true);
            thread.start();
            bridgeProbe("DPIS_FONT Flutter semantic active activity probe thread scheduled: package="
                    + packageName);
        } catch (Throwable throwable) {
            ACTIVE_ACTIVITY_SCAN_THREAD_STARTED.set(false);
            bridgeProbe("DPIS_FONT Flutter semantic active activity probe thread failed: package="
                    + packageName + ", error=" + throwable.getClass().getName()
                    + ": " + throwable.getMessage());
        }
    }

    private static void scanActiveActivities(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            String source) {
        try {
            bridgeProbe("DPIS_FONT Flutter semantic active activity scan enter: package="
                    + packageName + ", source=" + source);
            Object activityThread = Class.forName("android.app.ActivityThread")
                    .getMethod("currentActivityThread")
                    .invoke(null);
            Object activitiesObject = readField(activityThread, "mActivities");
            if (!(activitiesObject instanceof Map<?, ?> activities)) {
                bridgeProbe("DPIS_FONT Flutter semantic active activity scan skipped: package="
                        + packageName + ", source=" + source
                        + ", activities=" + (activitiesObject == null
                                ? "null"
                                : activitiesObject.getClass().getName()));
                return;
            }
            int scanned = 0;
            for (Object record : activities.values()) {
                Object activityObject = readField(record, "activity");
                Object pausedObject = readField(record, "paused");
                if (!(activityObject instanceof Activity activity)) {
                    continue;
                }
                if (activity.isFinishing()) {
                    continue;
                }
                scanned++;
                scanFlutterViews(activity.getWindow() != null
                        ? activity.getWindow().getDecorView()
                        : null,
                        xposed, packageName, targetFontScale, targetPercent, source);
                bridgeProbe("DPIS_FONT Flutter semantic active activity scanned: package="
                        + packageName + ", source=" + source
                        + ", activity=" + activity.getClass().getName()
                        + ", paused=" + pausedObject
                        + ", scanned=" + scanned);
            }
            if (scanned == 0) {
                bridgeProbe("DPIS_FONT Flutter semantic active activity scan empty: package="
                        + packageName + ", source=" + source
                        + ", records=" + activities.size());
            }
        } catch (Throwable throwable) {
            bridgeProbe("DPIS_FONT Flutter semantic active activity scan failed: package="
                    + packageName + ", source=" + source
                    + ", error=" + throwable.getClass().getName()
                    + ": " + throwable.getMessage());
        }
    }

    private static void scanFlutterViews(View view,
            XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            String source) {
        if (view == null) {
            return;
        }
        if (isFlutterViewClass(view.getClass().getName())) {
            bridgeProbe("DPIS_FONT Flutter semantic view found: package=" + packageName
                    + ", source=" + source
                    + ", view=" + view.getClass().getName()
                    + ", percent=" + targetPercent);
            installSemanticHooksFromFlutterObject(xposed, packageName, targetFontScale,
                    targetPercent, view, source);
            resendFlutterUserSettings(view, packageName, targetFontScale, targetPercent);
            return;
        }
        if (!(view instanceof ViewGroup group)) {
            return;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            scanFlutterViews(group.getChildAt(i), xposed, packageName, targetFontScale,
                    targetPercent, source);
        }
    }

    private static void installSemanticHooksFromFlutterObject(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            Object flutterObject,
            String source) {
        if (flutterObject == null) {
            return;
        }
        ClassLoader classLoader = flutterObject.getClass().getClassLoader();
        installSemanticHooksFromClassLoader(xposed, packageName, targetFontScale,
                targetPercent, classLoader, source + "-classloader");
        Object engine = findFlutterEngine(flutterObject);
        if (engine == null || engine == flutterObject) {
            bridgeProbe("DPIS_FONT Flutter semantic engine not found: package=" + packageName
                    + ", source=" + source
                    + ", object=" + flutterObject.getClass().getName()
                    + ", percent=" + targetPercent);
            return;
        }
        installSemanticHooksFromClassLoader(xposed, packageName, targetFontScale,
                targetPercent, engine.getClass().getClassLoader(), source + "-engine");
        DpisLog.i("DPIS_FONT Flutter semantic hooks refreshed: source=" + source
                + ", object=" + flutterObject.getClass().getName()
                + ", engine=" + engine.getClass().getName()
                + ", package=" + packageName
                + ", percent=" + targetPercent);
        bridgeProbe("DPIS_FONT Flutter semantic hooks refreshed: source=" + source
                + ", object=" + flutterObject.getClass().getName()
                + ", engine=" + engine.getClass().getName()
                + ", package=" + packageName
                + ", percent=" + targetPercent);
    }

    private static void installSemanticHooksFromClassLoader(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            ClassLoader classLoader,
            String source) {
        if (classLoader == null) {
            return;
        }
        tryHookMessageBuilder(xposed, packageName, targetFontScale, targetPercent, classLoader);
        tryHookSettingsChannel(packageName, targetFontScale, targetPercent, classLoader);
        tryHookFlutterJni(xposed, packageName, targetFontScale, targetPercent, classLoader);
        DpisLog.i("DPIS_FONT Flutter semantic classloader refresh: source=" + source
                + ", package=" + packageName + ", percent=" + targetPercent);
        bridgeProbe("DPIS_FONT Flutter semantic classloader refresh: source=" + source
                + ", package=" + packageName + ", percent=" + targetPercent);
    }

    private static Object findFlutterEngine(Object flutterObject) {
        Object fromMethod = findFlutterEngineFromMethods(flutterObject);
        if (fromMethod != null) {
            return fromMethod;
        }
        return findFlutterEngineFromFields(flutterObject);
    }

    private static Object findFlutterEngineFromMethods(Object flutterObject) {
        Class<?> clazz = flutterObject.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getParameterTypes().length != 0
                        || !isFlutterEngineTypeName(method.getReturnType().getName())) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    return method.invoke(flutterObject);
                } catch (Throwable ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static Object findFlutterEngineFromFields(Object flutterObject) {
        Class<?> clazz = flutterObject.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!isFlutterEngineTypeName(field.getType().getName())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    return field.get(flutterObject);
                } catch (Throwable ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static Object readField(Object target, String fieldName) {
        if (target == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        Class<?> clazz = target.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private static void hookFlutterJniClass(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            Class<?> clazz) {
        if (xposed == null || clazz == null || !isFlutterJniClass(clazz.getName())) {
            return;
        }
        String key = clazz.getName() + "@"
                + System.identityHashCode(clazz.getClassLoader()) + "@"
                + System.identityHashCode(clazz);
        if (!HOOKED_CLASSES.add(key)) {
            return;
        }
        boolean hookedAny = false;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!isPlatformMessageDispatchMethod(method)) {
                continue;
            }
            hookPlatformMessageDispatch(xposed, packageName, targetFontScale,
                    targetPercent, method);
            hookedAny = true;
        }
        if (hookedAny) {
            DpisLog.i("DPIS_FONT FlutterJNI platform message hook ready: class="
                    + clazz.getName() + ", percent=" + targetPercent);
        } else {
            DpisLog.i("DPIS_FONT FlutterJNI hook skipped: no platform message methods in "
                    + clazz.getName());
        }
    }

    private static void hookSettingsChannelClass(String packageName,
            float targetFontScale,
            int targetPercent,
            Class<?> clazz) {
        if (clazz == null || !isSettingsChannelClass(clazz.getName())) {
            return;
        }
        String key = clazz.getName() + "@"
                + System.identityHashCode(clazz.getClassLoader()) + "@"
                + System.identityHashCode(clazz);
        if (!HOOKED_CLASSES.add(key)) {
            return;
        }
        DpisLog.i("DPIS_FONT Flutter class observed: class=" + clazz.getName()
                + ", package=" + packageName
                + ", percent=" + targetPercent
                + ", targetScale=" + targetFontScale);
        for (Method method : clazz.getDeclaredMethods()) {
            DpisLog.i("DPIS_FONT Flutter SettingsChannel method observed: "
                    + method.toGenericString() + ", package=" + packageName
                    + ", percent=" + targetPercent + ", targetScale=" + targetFontScale);
        }
    }

    private static void resendFlutterUserSettings(Object thisObject,
            String packageName,
            float targetFontScale,
            int targetPercent) {
        if (thisObject == null) {
            return;
        }
        int viewId = System.identityHashCode(thisObject);
        if (!RESENT_SETTINGS_VIEW_IDS.add(viewId)) {
            return;
        }
        try {
            Method method = thisObject.getClass().getDeclaredMethod("sendUserSettingsToFlutter");
            method.setAccessible(true);
            method.invoke(thisObject);
            DpisLog.i("DPIS_FONT Flutter view resend user settings: package="
                    + packageName + ", percent=" + targetPercent
                    + ", targetScale=" + targetFontScale);
            bridgeProbe("DPIS_FONT Flutter semantic resend user settings: package="
                    + packageName + ", percent=" + targetPercent
                    + ", targetScale=" + targetFontScale);
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter view resend user settings failed for "
                    + packageName, throwable);
            bridgeProbe("DPIS_FONT Flutter semantic resend failed: package=" + packageName
                    + ", object=" + thisObject.getClass().getName()
                    + ", error=" + throwable.getClass().getName()
                    + ": " + throwable.getMessage());
        }
    }

    private static void hookPlatformMessageDispatch(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            Method method) {
        try {
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        List<Object> args = chain.getArgs();
                        PlatformMessageIndexes indexes = findPlatformMessageIndexes(args);
                        if (indexes == null) {
                            return chain.proceed();
                        }
                        if (!SETTINGS_CHANNEL_NAME.equals(args.get(indexes.channelIndex))) {
                            return chain.proceed();
                        }
                        Object bufferObject = args.get(indexes.bufferIndex);
                        Object positionObject = args.get(indexes.positionIndex);
                        if (!(bufferObject instanceof ByteBuffer buffer)
                                || !(positionObject instanceof Integer position)) {
                            return chain.proceed();
                        }
                        ByteBuffer adjusted = replaceSettingsTextScaleFactor(
                                buffer, position, targetFontScale);
                        if (adjusted == null) {
                            return chain.proceed();
                        }
                        Object[] adjustedArgs = args.toArray();
                        adjustedArgs[indexes.bufferIndex] = adjusted;
                        adjustedArgs[indexes.positionIndex] = adjusted.remaining();
                        Object result = chain.proceed(adjustedArgs);
                        DpisLog.i("DPIS_FONT FlutterJNI settings message override: percent="
                                + targetPercent + ", targetScale=" + targetFontScale
                                + ", package=" + packageName
                                + ", method=" + method.getName());
                        bridgeProbe("DPIS_FONT Flutter semantic settings override: package="
                                + packageName
                                + ", percent=" + targetPercent
                                + ", targetScale=" + targetFontScale
                                + ", method=" + method.getName());
                        return result;
                    });
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT FlutterJNI platform message hook failed for "
                    + packageName + ": " + method.toGenericString(), throwable);
        }
    }

    private static void hookSetTextScaleFactor(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            Method method) {
        try {
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        float incoming = (Float) chain.getArg(0);
                        if (Math.abs(incoming - targetFontScale) < EPSILON) {
                            return chain.proceed();
                        }
                        Object result = chain.proceed(new Object[] { targetFontScale });
                        DpisLog.i("DPIS_FONT Flutter settings textScaleFactor override: in="
                                + incoming + ", out=" + targetFontScale
                                + ", percent=" + targetPercent
                                + ", package=" + packageName);
                        return result;
                    });
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter settings setTextScaleFactor hook failed for "
                    + packageName, throwable);
        }
    }

    private static void hookSetDisplayMetrics(XposedInterface xposed,
            String packageName,
            float targetFontScale,
            int targetPercent,
            Method method) {
        try {
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object metricsObject = chain.getArg(0);
                        if (!(metricsObject instanceof DisplayMetrics metrics)) {
                            return chain.proceed();
                        }
                        DisplayMetrics adjusted = cloneWithTargetScaledDensity(
                                metrics, targetFontScale);
                        if (Math.abs(adjusted.scaledDensity - metrics.scaledDensity) < EPSILON) {
                            return chain.proceed();
                        }
                        Object result = chain.proceed(new Object[] { adjusted });
                        DpisLog.i("DPIS_FONT Flutter settings DisplayMetrics override: scaledDensity="
                                + metrics.scaledDensity + " -> " + adjusted.scaledDensity
                                + ", density=" + adjusted.density
                                + ", percent=" + targetPercent
                                + ", package=" + packageName);
                        return result;
                    });
        } catch (Throwable throwable) {
            DpisLog.e("DPIS_FONT Flutter settings setDisplayMetrics hook failed for "
                    + packageName, throwable);
        }
    }

    static DisplayMetrics cloneWithTargetScaledDensityForTest(DisplayMetrics metrics,
            float targetFontScale) {
        return cloneWithTargetScaledDensity(metrics, targetFontScale);
    }

    private static DisplayMetrics cloneWithTargetScaledDensity(DisplayMetrics metrics,
            float targetFontScale) {
        DisplayMetrics adjusted = new DisplayMetrics();
        copyDisplayMetrics(metrics, adjusted);
        float density = adjusted.density > 0f
                ? adjusted.density
                : adjusted.densityDpi / (float) DisplayMetrics.DENSITY_DEFAULT;
        if (density <= 0f) {
            return adjusted;
        }
        adjusted.scaledDensity = density * targetFontScale;
        return adjusted;
    }

    private static void copyDisplayMetrics(DisplayMetrics source, DisplayMetrics target) {
        target.widthPixels = source.widthPixels;
        target.heightPixels = source.heightPixels;
        target.density = source.density;
        target.densityDpi = source.densityDpi;
        target.scaledDensity = source.scaledDensity;
        target.xdpi = source.xdpi;
        target.ydpi = source.ydpi;
    }

    static boolean isMessageBuilderClassForTest(String className) {
        return isMessageBuilderClass(className);
    }

    static boolean isSettingsChannelClassForTest(String className) {
        return isSettingsChannelClass(className);
    }

    static boolean isSettingsMessageBuilderClassForTest(String className) {
        return isMessageBuilderClass(className);
    }

    static boolean isFlutterViewClassForTest(String className) {
        return isFlutterViewClass(className);
    }

    static boolean isFlutterFragmentClassForTest(String className) {
        return isFlutterFragmentClass(className);
    }

    static boolean isFlutterJniClassForTest(String className) {
        return isFlutterJniClass(className);
    }

    static boolean isFlutterEngineTypeNameForTest(String className) {
        return isFlutterEngineTypeName(className);
    }

    private static boolean isMessageBuilderClass(String className) {
        return MESSAGE_BUILDER_CLASS.equals(className);
    }

    private static boolean isSettingsChannelClass(String className) {
        return SETTINGS_CHANNEL_CLASS.equals(className);
    }

    private static boolean isFlutterViewClass(String className) {
        return FLUTTER_VIEW_CLASS.equals(className);
    }

    private static boolean isFlutterFragmentClass(String className) {
        return FLUTTER_FRAGMENT_CLASS.equals(className);
    }

    private static boolean isFlutterJniClass(String className) {
        return FLUTTER_JNI_CLASS.equals(className);
    }

    private static boolean isFlutterSemanticClass(String className) {
        return isMessageBuilderClass(className)
                || isSettingsChannelClass(className)
                || isFlutterViewClass(className)
                || isFlutterFragmentClass(className)
                || isFlutterJniClass(className);
    }

    private static boolean isFlutterEngineTypeName(String className) {
        return "io.flutter.embedding.engine.FlutterEngine".equals(className);
    }

    static boolean isSetTextScaleFactorMethodForTest(Method method) {
        return isSetTextScaleFactorMethod(method);
    }

    private static boolean isSetTextScaleFactorMethod(Method method) {
        if (method == null || !"setTextScaleFactor".equals(method.getName())) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length == 1 && parameterTypes[0] == float.class;
    }

    static boolean isSetDisplayMetricsMethodForTest(Method method) {
        return isSetDisplayMetricsMethod(method);
    }

    private static boolean isSetDisplayMetricsMethod(Method method) {
        if (method == null || !"setDisplayMetrics".equals(method.getName())) {
            return false;
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length == 1 && parameterTypes[0] == DisplayMetrics.class;
    }

    static boolean isPlatformMessageDispatchMethodForTest(Method method) {
        return isPlatformMessageDispatchMethod(method);
    }

    private static boolean isPlatformMessageDispatchMethod(Method method) {
        if (method == null) {
            return false;
        }
        String name = method.getName();
        if (!"dispatchPlatformMessage".equals(name)
                && !"nativeDispatchPlatformMessage".equals(name)) {
            return false;
        }
        boolean hasChannel = false;
        boolean hasBuffer = false;
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (Class<?> parameterType : parameterTypes) {
            hasChannel |= parameterType == String.class;
            hasBuffer |= parameterType == ByteBuffer.class;
        }
        return hasChannel && hasBuffer;
    }

    static ByteBuffer replaceSettingsTextScaleFactorForTest(ByteBuffer buffer,
            int position,
            float targetFontScale) {
        return replaceSettingsTextScaleFactor(buffer, position, targetFontScale);
    }

    private static ByteBuffer replaceSettingsTextScaleFactor(ByteBuffer buffer,
            int position,
            float targetFontScale) {
        if (buffer == null || position <= 0) {
            return null;
        }
        int length = Math.min(position, buffer.capacity());
        if (length <= 0) {
            return null;
        }
        byte[] bytes = new byte[length];
        ByteBuffer duplicate = buffer.duplicate();
        duplicate.position(0);
        duplicate.limit(length);
        duplicate.get(bytes);
        String json = new String(bytes, StandardCharsets.UTF_8);
        String adjustedJson = rewriteTextScaleFactor(json, targetFontScale);
        if (adjustedJson == null) {
            return null;
        }
        byte[] adjustedBytes = adjustedJson.getBytes(StandardCharsets.UTF_8);
        ByteBuffer adjusted = ByteBuffer.allocateDirect(adjustedBytes.length);
        adjusted.put(adjustedBytes);
        adjusted.flip();
        return adjusted;
    }

    private static String rewriteTextScaleFactor(String json, float targetFontScale) {
        if (json == null) {
            return null;
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null;
        }
        Matcher matcher = TEXT_SCALE_FACTOR_PATTERN.matcher(json);
        if (matcher.find()) {
            try {
                double current = Double.parseDouble(matcher.group(2));
                if (Double.isFinite(current) && Math.abs(current - targetFontScale) < EPSILON) {
                    return null;
                }
            } catch (NumberFormatException ignored) {
                // Replace malformed numeric-looking values below.
            }
            return matcher.replaceFirst(Matcher.quoteReplacement(
                    matcher.group(1) + formatScale(targetFontScale)));
        }
        int insertAt = json.lastIndexOf('}');
        if (insertAt < 0) {
            return null;
        }
        String prefix = json.substring(0, insertAt).trim();
        String separator = prefix.length() > 1 ? "," : "";
        return json.substring(0, insertAt) + separator + "\""
                + TEXT_SCALE_FACTOR_KEY + "\":" + formatScale(targetFontScale)
                + json.substring(insertAt);
    }

    private static String formatScale(float targetFontScale) {
        if (Math.abs(targetFontScale - Math.round(targetFontScale)) < EPSILON) {
            return Integer.toString(Math.round(targetFontScale));
        }
        return Float.toString(targetFontScale);
    }

    private static PlatformMessageIndexes findPlatformMessageIndexes(List<Object> args) {
        if (args == null || args.isEmpty()) {
            return null;
        }
        int channelIndex = -1;
        int bufferIndex = -1;
        int positionIndex = -1;
        for (int i = 0; i < args.size(); i++) {
            Object arg = args.get(i);
            if (channelIndex < 0 && arg instanceof String) {
                channelIndex = i;
            } else if (channelIndex >= 0 && bufferIndex < 0 && arg instanceof ByteBuffer) {
                bufferIndex = i;
            } else if (bufferIndex >= 0 && positionIndex < 0 && arg instanceof Integer) {
                positionIndex = i;
                break;
            }
        }
        if (channelIndex < 0 || bufferIndex < 0 || positionIndex < 0) {
            return null;
        }
        return new PlatformMessageIndexes(channelIndex, bufferIndex, positionIndex);
    }

    private static final class PlatformMessageIndexes {
        final int channelIndex;
        final int bufferIndex;
        final int positionIndex;

        PlatformMessageIndexes(int channelIndex, int bufferIndex, int positionIndex) {
            this.channelIndex = channelIndex;
            this.bufferIndex = bufferIndex;
            this.positionIndex = positionIndex;
        }
    }

    private static void bridgeProbe(String message) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        try {
            android.util.Log.i("DPIS", BRIDGE_LOG_PREFIX + message);
        } catch (Throwable ignored) {
            // Probe logging must never affect target app behavior.
        }
    }
}
