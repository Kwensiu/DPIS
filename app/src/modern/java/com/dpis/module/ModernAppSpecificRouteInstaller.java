package com.dpis.module;

import com.dpis.module.appconfig.WechatDpiConfig;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

final class ModernAppSpecificRouteInstaller {
    private static final AtomicBoolean WECHAT_APPLICATION_ATTACH_HOOK_INSTALLED =
            new AtomicBoolean(false);

    private ModernAppSpecificRouteInstaller() {
    }

    static void handleModuleLoaded(XposedModule xposed, String processName) {
        if (xposed == null || !WechatDpiConfig.appliesTo(processName)) {
            return;
        }
        installWechatApplicationAttachHook(xposed);
    }

    static boolean handlePackageReady(XposedModule xposed,
            XposedModule.PackageReadyParam param,
            String processName) {
        if (param == null || !WechatDpiConfig.appliesTo(param.getPackageName())) {
            return false;
        }
        if (WechatDpiConfig.appliesTo(processName)) {
            installWechatPackageReadyRoute(
                    xposed,
                    param.getPackageName(),
                    param.getClassLoader(),
                    param.getApplicationInfo(),
                    processName,
                    "package_ready");
        }
        DpisLog.i("modern app-specific route installed alongside generic hooks: package="
                + WechatDpiConfig.PACKAGE_NAME + ", process=" + processName);
        return false;
    }

    static void handlePackageReadyReplay(XposedModule xposed,
            String packageName,
            ClassLoader classLoader,
            ApplicationInfo applicationInfo,
            String processName) {
        if (xposed == null || !WechatDpiConfig.appliesTo(packageName)
                || !WechatDpiConfig.appliesTo(processName)) {
            return;
        }
        installWechatPackageReadyRoute(
                xposed,
                packageName,
                classLoader,
                applicationInfo,
                processName,
                "hot_reload_package_ready");
    }

    private static void installWechatPackageReadyRoute(XposedModule xposed,
            String packageName,
            ClassLoader classLoader,
            ApplicationInfo applicationInfo,
            String processName,
            String source) {
        DpisLog.i("modern WeChat DPI route enter: package="
                + packageName + ", process=" + processName + ", source=" + source);
        xposed.log(android.util.Log.INFO, DpisLog.TAG,
                "DPIS modern WeChat DPI route enter: package="
                        + packageName + ", process=" + processName + ", source=" + source);
        FeedbackDiagnosticRuntimeHotPathEvents.event(
                packageName,
                "wechat_dpi",
                source,
                "route_callback_entered",
                "source=" + source + ", process=" + processName);
        try {
            boolean installed = WechatDpiModernHookInstaller.install(
                    xposed,
                    classLoader,
                    applicationInfo,
                    packageName);
            DpisLog.i("modern WeChat DPI route install attempted: package="
                    + packageName + ", process=" + processName
                    + ", source=" + source
                    + ", installed=" + installed
                    + ", classLoader="
                    + WechatDpiModernHookInstaller.describeClassLoaderForLog(classLoader));
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    packageName,
                    "wechat_dpi",
                    source,
                    installed ? "mutation_candidate" : "skipped",
                    "installAttempted=true, installed=" + installed
                            + ", process=" + processName);
        } catch (Throwable throwable) {
            DpisLog.e("modern WeChat DPI route install failed: package="
                    + packageName + ", process=" + processName + ", source=" + source
                    + ", " + throwable.getClass().getName() + ": " + throwable.getMessage(),
                    throwable);
            FeedbackDiagnosticRuntimeHotPathEvents.event(
                    packageName,
                    "wechat_dpi",
                    source,
                    "skipped",
                    "installFailed=true, process=" + processName
                            + ", error=" + throwable.getClass().getSimpleName());
        }
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
                        if (contextObject instanceof Context context
                                && WechatDpiConfig.appliesTo(context.getPackageName())) {
                            ClassLoader classLoader = context.getClassLoader();
                            DpisLog.i("modern WeChat DPI application-attach route enter: package="
                                    + context.getPackageName()
                                    + ", classLoader="
                                    + WechatDpiModernHookInstaller.describeClassLoaderForLog(
                                            classLoader));
                            FeedbackDiagnosticRuntimeHotPathEvents.event(
                                    context.getPackageName(),
                                    "wechat_dpi",
                                    "application_attach",
                                    "route_callback_entered",
                                    "classLoader="
                                            + WechatDpiModernHookInstaller.describeClassLoaderForLog(
                                                    classLoader));
                            boolean installed = WechatDpiModernHookInstaller.install(
                                    xposed,
                                    classLoader,
                                    context.getApplicationInfo(),
                                    context.getPackageName());
                            DpisLog.i("modern WeChat DPI application-attach retry result: package="
                                    + context.getPackageName()
                                    + ", installed=" + installed
                                    + ", classLoader="
                                    + WechatDpiModernHookInstaller.describeClassLoaderForLog(
                                            classLoader));
                            FeedbackDiagnosticRuntimeHotPathEvents.event(
                                    context.getPackageName(),
                                    "wechat_dpi",
                                    "application_attach",
                                    installed ? "mutation_candidate" : "skipped",
                                    "retryInstallAttempted=true, installed=" + installed
                                            + ", classLoader="
                                            + WechatDpiModernHookInstaller.describeClassLoaderForLog(
                                                    classLoader));
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
            DpisLog.e("modern WeChat DPI application-attach hook failed: "
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
