package com.dpis.module;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class Compat100SystemServerHookInstaller {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private Compat100SystemServerHookInstaller() {
    }

    static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            // API100 legacy modules do not have libxposed's system_server entry,
            // so keep this launch Configuration path wired explicitly for compat100.
            Class<?> launchActivityItemClass = Class.forName(
                    "android.app.servertransaction.LaunchActivityItem");
            PerAppDisplayConfigSource source = new PerAppDisplayConfigSource(
                    ConfigStoreFactory.createForCompat100SystemServerHost());
            int hookedCount = 0;
            for (Constructor<?> constructor : launchActivityItemClass.getDeclaredConstructors()) {
                XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        applyLaunchActivityItemArgs(source, param.args);
                    }
                });
                hookedCount++;
            }
            logDebug("compat100 system_server launch-activity-item hook ready: constructors=" + hookedCount);
        } catch (Throwable throwable) {
            INSTALLED.set(false);
            logError("compat100 system_server launch-activity-item hook failed: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
        }
    }

    static void applyLaunchActivityItemArgs(PerAppDisplayConfigSource source, Object[] args) {
        if (source == null || args == null || !source.isSystemServerHooksEnabled()) {
            return;
        }
        String packageName = findActivityInfoPackage(args);
        if (packageName == null) {
            return;
        }
        PerAppDisplayConfig config = source.get(packageName);
        if (config == null) {
            return;
        }
        Configuration baseConfiguration = findFirstConfiguration(args);
        if (baseConfiguration == null) {
            return;
        }
        PerAppDisplayEnvironment environment = null;
        if (config.hasViewportOverride()) {
            environment = PerAppDisplayOverrideCalculator.calculate(
                    baseConfiguration,
                    resolveWidthPx(baseConfiguration),
                    resolveHeightPx(baseConfiguration),
                    config.targetViewportWidthDp);
        }
        boolean changed = false;
        for (Object arg : args) {
            if (arg instanceof Configuration configuration) {
                if (environment != null) {
                    changed |= applyConfiguration(configuration, environment);
                }
                changed |= applyFontScale(configuration, config);
            }
        }
        if (changed) {
            logDebug("compat100 system_server launch-activity-item apply: package=" + packageName
                    + ", widthDp=" + baseConfiguration.screenWidthDp
                    + ", densityDpi=" + baseConfiguration.densityDpi
                    + ", fontScale=" + baseConfiguration.fontScale);
        }
    }

    private static String findActivityInfoPackage(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof ActivityInfo activityInfo
                    && activityInfo.packageName != null
                    && !activityInfo.packageName.isBlank()) {
                return activityInfo.packageName;
            }
        }
        return null;
    }

    private static Configuration findFirstConfiguration(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Configuration configuration) {
                return configuration;
            }
        }
        return null;
    }

    private static boolean applyConfiguration(Configuration configuration,
                                              PerAppDisplayEnvironment environment) {
        if (configuration == null || environment == null) {
            return false;
        }
        boolean changed = configuration.screenWidthDp != environment.widthDp
                || configuration.screenHeightDp != environment.heightDp
                || configuration.smallestScreenWidthDp != environment.smallestWidthDp
                || configuration.densityDpi != environment.densityDpi;
        if (changed) {
            ViewportOverride.apply(configuration, new ViewportOverride.Result(
                    environment.widthDp,
                    environment.heightDp,
                    environment.smallestWidthDp,
                    environment.densityDpi));
        }
        return changed;
    }

    private static boolean applyFontScale(Configuration configuration, PerAppDisplayConfig config) {
        if (configuration == null
                || config == null
                || !FontApplyMode.SYSTEM_EMULATION.equals(config.targetFontMode)
                || config.targetFontScalePercent == null
                || config.targetFontScalePercent <= 0) {
            return false;
        }
        float fontScale = config.targetFontScalePercent / 100.0f;
        if (Math.abs(configuration.fontScale - fontScale) < 0.0001f) {
            return false;
        }
        configuration.fontScale = fontScale;
        return true;
    }

    private static int resolveWidthPx(Configuration configuration) {
        if (configuration == null || configuration.screenWidthDp <= 0 || configuration.densityDpi <= 0) {
            return 0;
        }
        return Math.round(configuration.screenWidthDp * (configuration.densityDpi / 160.0f));
    }

    private static int resolveHeightPx(Configuration configuration) {
        if (configuration == null || configuration.screenHeightDp <= 0 || configuration.densityDpi <= 0) {
            return 0;
        }
        return Math.round(configuration.screenHeightDp * (configuration.densityDpi / 160.0f));
    }

    private static void logDebug(String message) {
        if (!DpisLog.isLoggingEnabled()) {
            return;
        }
        DpisLog.i(message);
    }

    private static void logError(String message) {
        try {
            XposedBridge.log("DPIS " + message);
        } catch (Throwable ignored) {
        }
        try {
            Log.e(DpisLog.TAG, message);
        } catch (Throwable ignored) {
        }
    }
}
