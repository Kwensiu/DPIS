package com.dpis.module.fonts;

import com.dpis.module.DpisConfigStore;
import com.dpis.module.DpisLog;
import com.dpis.module.fonts.FontApplyMode;

import android.content.Context;

import java.util.LinkedHashSet;

public final class HyperOsNativeProxyRefreshCoordinator {
    private HyperOsNativeProxyRefreshCoordinator() {
    }

    /*
     * Dormant helper: automatic startup/package-update proxy refresh is intentionally disabled.
     * Keep this entry point for a future explicit user action or opt-in flow only; do not wire it
     * back into app startup, service binding, or MY_PACKAGE_REPLACED without revisiting the native
     * file side-effect scope.
     */
    public static void refreshConfiguredTargetsAsync(Context context, DpisConfigStore store) {
        if (context == null || store == null) {
            return;
        }
        LinkedHashSet<String> packages = collectRefreshPackages(store);
        if (packages.isEmpty()) {
            return;
        }
        Thread refreshThread = new Thread(() -> refreshConfiguredTargets(context, packages),
                "DPIS-HyperOsNativeProxyRefresh");
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    public static LinkedHashSet<String> collectRefreshPackagesForTest(DpisConfigStore store) {
        return collectRefreshPackages(store);
    }

    private static LinkedHashSet<String> collectRefreshPackages(DpisConfigStore store) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        if (store == null
                || !store.isFlutterFontHookEnabled()
                || !store.isHyperOsFlutterFontHookEnabled()) {
            return packages;
        }
        for (String packageName : store.getConfiguredPackages()) {
            Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
            String fontMode = store.getTargetFontApplyMode(packageName);
            if (store.isTargetDpisEnabled(packageName)
                    && fontScalePercent != null
                    && fontScalePercent > 0
                    && FontApplyMode.isEnabled(fontMode)) {
                packages.add(packageName);
            }
        }
        return packages;
    }

    private static void refreshConfiguredTargets(Context context, LinkedHashSet<String> packages) {
        for (String packageName : packages) {
            HyperOsNativeProxyBindMounter.MountPlan plan =
                    HyperOsNativeProxyBindMounter.createPlan(context, packageName);
            if (!plan.valid()) {
                DpisLog.i("HyperOS Native Proxy refresh skipped package=" + packageName
                        + " reason=" + plan.reason());
                continue;
            }
            HyperOsNativeProxyBindMounter.MountResult result =
                    HyperOsNativeProxyBindMounter.apply(plan);
            DpisLog.i("HyperOS Native Proxy refresh package=" + packageName
                    + " success=" + result.success()
                    + " output=" + result.output());
        }
    }
}
