package com.dpis.module;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

final class ModernAppSpecificRouteInstaller {
    private static final AtomicBoolean WECHAT_MODULE_LOADED_CLASS_HOOK_INSTALLED =
            new AtomicBoolean(false);
    private static final AtomicBoolean WECHAT_APPLICATION_ATTACH_HOOK_INSTALLED =
            new AtomicBoolean(false);

    private ModernAppSpecificRouteInstaller() {
    }

    static void handleModuleLoaded(XposedModule xposed, String processName) {
        if (xposed == null || !WechatDpiConfig.appliesTo(processName)) {
            return;
        }
        installWechatModuleLoadedClassHook(xposed);
        installWechatApplicationAttachHook(xposed);
    }

    @SuppressLint("NewApi")
    static boolean handlePackageLoaded(XposedModule xposed,
            XposedModule.PackageLoadedParam param,
            String processName) {
        if (param == null || !WechatDpiConfig.appliesTo(param.getPackageName())) {
            return false;
        }
        if (WechatDpiConfig.appliesTo(processName)) {
            DpisLog.i("modern WeChat DPI package-loaded route enter: package="
                    + param.getPackageName() + ", process=" + processName);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    param.getPackageName(),
                    "wechat_dpi",
                    "package_loaded",
                    "route_callback_entered",
                    "source=package_loaded, process=" + processName);
            try {
                WechatDpiModernHookInstaller.install(
                        xposed,
                        param.getDefaultClassLoader(),
                        param.getApplicationInfo(),
                        param.getPackageName());
                DpisLog.i("modern WeChat DPI package-loaded route install attempted: package="
                        + param.getPackageName() + ", process=" + processName
                        + ", defaultClassLoader="
                        + WechatDpiModernHookInstaller.describeClassLoaderForLog(
                                param.getDefaultClassLoader()));
                FeedbackDiagnosticRuntimeHotPathEvents.event(
                        param.getPackageName(),
                        "wechat_dpi",
                        "package_loaded",
                        "mutation_candidate",
                        "installAttempted=true, process=" + processName);
            } catch (Throwable throwable) {
                DpisLog.e("modern WeChat DPI package-loaded route install failed: package="
                        + param.getPackageName() + ", process=" + processName + ", "
                        + throwable.getClass().getName() + ": " + throwable.getMessage(),
                        throwable);
                FeedbackDiagnosticRuntimeHotPathEvents.event(
                        param.getPackageName(),
                        "wechat_dpi",
                        "package_loaded",
                        "skipped",
                        "installFailed=true, process=" + processName
                                + ", error=" + throwable.getClass().getSimpleName());
            }
        }
        return false;
    }

    static boolean handlePackageReady(XposedModule xposed,
            XposedModule.PackageReadyParam param,
            String processName) {
        if (param == null || !WechatDpiConfig.appliesTo(param.getPackageName())) {
            return false;
        }
        if (WechatDpiConfig.appliesTo(processName)) {
            DpisLog.i("modern WeChat DPI route enter: package="
                    + param.getPackageName() + ", process=" + processName);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    param.getPackageName(),
                    "wechat_dpi",
                    "package_ready",
                    "route_callback_entered",
                    "source=package_ready, process=" + processName);
            try {
                WechatDpiModernHookInstaller.install(
                        xposed,
                        param.getClassLoader(),
                        param.getApplicationInfo(),
                        param.getPackageName());
                DpisLog.i("modern WeChat DPI route install attempted: package="
                        + param.getPackageName() + ", process=" + processName
                        + ", classLoader="
                        + WechatDpiModernHookInstaller.describeClassLoaderForLog(
                                param.getClassLoader()));
                FeedbackDiagnosticRuntimeHotPathEvents.event(
                        param.getPackageName(),
                        "wechat_dpi",
                        "package_ready",
                        "mutation_candidate",
                        "installAttempted=true, process=" + processName);
            } catch (Throwable throwable) {
                DpisLog.e("modern WeChat DPI route install failed: package="
                        + param.getPackageName() + ", process=" + processName + ", "
                        + throwable.getClass().getName() + ": " + throwable.getMessage(),
                        throwable);
                FeedbackDiagnosticRuntimeHotPathEvents.event(
                        param.getPackageName(),
                        "wechat_dpi",
                        "package_ready",
                        "skipped",
                        "installFailed=true, process=" + processName
                                + ", error=" + throwable.getClass().getSimpleName());
            }
        }
        DpisLog.i("modern app-specific route installed alongside generic hooks: package="
                + WechatDpiConfig.PACKAGE_NAME + ", process=" + processName);
        return false;
    }

    private static void installWechatModuleLoadedClassHook(XposedInterface xposed) {
        if (!WECHAT_MODULE_LOADED_CLASS_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method method = ClassLoader.class.getDeclaredMethod(
                    "loadClass", String.class, boolean.class);
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        maybeInstallWechatFromLoadedClass(xposed, result, "loadClass");
                        return result;
                    });
            DpisLog.i("modern WeChat DPI module-loaded class hook ready: process="
                    + WechatDpiConfig.PACKAGE_NAME);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "module_loaded_class",
                    "hook_ready",
                    "process=" + WechatDpiConfig.PACKAGE_NAME);
        } catch (Throwable throwable) {
            WECHAT_MODULE_LOADED_CLASS_HOOK_INSTALLED.set(false);
            DpisLog.e("modern WeChat DPI module-loaded class hook failed: process="
                    + WechatDpiConfig.PACKAGE_NAME + ", "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(),
                    throwable);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "module_loaded_class",
                    "skipped",
                    "hookFailed=true, error=" + throwable.getClass().getSimpleName());
        }
    }

    private static void maybeInstallWechatFromLoadedClass(XposedInterface xposed, Object result,
            String source) {
        if (!(result instanceof Class<?> loadedClass)
                || !WechatDpiRoutes.matchesClassName(loadedClass.getName())) {
            return;
        }
        DpisLog.i("modern WeChat DPI module-loaded class hit: source=" + source
                + ", class=" + loadedClass.getName());
        FeedbackDiagnosticRuntimeHotPathEvents.event(
                WechatDpiConfig.PACKAGE_NAME,
                "wechat_dpi",
                "module_loaded_class",
                "route_callback_entered",
                "source=" + source + ", class=" + loadedClass.getName());
        WechatDpiModernHookInstaller.installFromLoadedClass(
                xposed, loadedClass, WechatDpiConfig.PACKAGE_NAME);
    }

    private static void installWechatApplicationAttachHook(XposedInterface xposed) {
        if (!WECHAT_APPLICATION_ATTACH_HOOK_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method method = Application.class.getDeclaredMethod("attach", Context.class);
            xposed.hook(method)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object contextObject = chain.getArgs() != null && !chain.getArgs().isEmpty()
                                ? chain.getArgs().get(0)
                                : null;
                        if (contextObject instanceof Context context) {
                            ClassLoader classLoader = context.getClassLoader();
                            DpisLog.i("modern WeChat DPI application-attach route enter: package="
                                    + WechatDpiConfig.PACKAGE_NAME
                                    + ", classLoader="
                                    + WechatDpiModernHookInstaller.describeClassLoaderForLog(
                                            classLoader));
                            FeedbackDiagnosticRuntimeHotPathEvents.event(
                                    WechatDpiConfig.PACKAGE_NAME,
                                    "wechat_dpi",
                                    "application_attach",
                                    "route_callback_entered",
                                    "classLoader="
                                            + WechatDpiModernHookInstaller.describeClassLoaderForLog(
                                                    classLoader));
                            WechatDpiModernHookInstaller.install(
                                    xposed,
                                    classLoader,
                                    context.getApplicationInfo(),
                                    context.getPackageName());
                        }
                        return result;
                    });
            DpisLog.i("modern WeChat DPI application-attach hook ready: process="
                    + WechatDpiConfig.PACKAGE_NAME);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "application_attach",
                    "hook_ready",
                    "process=" + WechatDpiConfig.PACKAGE_NAME);
        } catch (Throwable throwable) {
            WECHAT_APPLICATION_ATTACH_HOOK_INSTALLED.set(false);
            DpisLog.e("modern WeChat DPI application-attach hook failed: process="
                    + WechatDpiConfig.PACKAGE_NAME + ", "
                    + throwable.getClass().getName() + ": " + throwable.getMessage(),
                    throwable);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    WechatDpiConfig.PACKAGE_NAME,
                    "wechat_dpi",
                    "application_attach",
                    "skipped",
                    "hookFailed=true, error=" + throwable.getClass().getSimpleName());
        }
    }

    static boolean shouldSuppressModuleLoadedGenericHooks(String packageName, String processName) {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return false;
        }
        DpisLog.i("modern app-specific route allowing generic hooks alongside: package="
                + WechatDpiConfig.PACKAGE_NAME + ", process=" + processName);
        return false;
    }
}
