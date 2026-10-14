package com.dpis.module.runtime.hyperos

import android.content.Context
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.FontApplyMode

object HyperOsNativeProxyRefreshCoordinator {
    /*
     * Dormant helper: automatic startup/package-update proxy refresh is intentionally disabled.
     * Keep this entry point for a future explicit user action or opt-in flow only; do not wire it
     * back into app startup, service binding, or MY_PACKAGE_REPLACED without revisiting the native
     * file side-effect scope.
     */
    @JvmStatic
    fun refreshConfiguredTargetsAsync(context: Context?, store: DpisConfigStore?) {
        if (context == null || store == null) return
        val packages = collectRefreshPackages(store)
        if (packages.isEmpty()) return
        Thread(
            { refreshConfiguredTargets(context, packages) },
            "DPIS-HyperOsNativeProxyRefresh",
        ).apply { isDaemon = true }.start()
    }

    @JvmStatic
    fun collectRefreshPackagesForTest(store: DpisConfigStore?): LinkedHashSet<String> =
        collectRefreshPackages(store)

    private fun collectRefreshPackages(store: DpisConfigStore?): LinkedHashSet<String> {
        val packages = LinkedHashSet<String>()
        if (store == null || !store.isFlutterFontHookEnabled ||
            !store.isHyperOsFlutterFontHookEnabled
        ) return packages
        for (rawPackageName in store.getConfiguredPackages()) {
            val packageName = rawPackageName ?: continue
            val fontScalePercent = store.getTargetFontScalePercent(packageName)
            val fontMode = store.getTargetFontApplyMode(packageName)
            if (store.isTargetDpisEnabled(packageName) &&
                fontScalePercent != null && fontScalePercent > 0 &&
                FontApplyMode.isEnabled(fontMode)
            ) {
                packages.add(packageName)
            }
        }
        return packages
    }

    private fun refreshConfiguredTargets(context: Context, packages: LinkedHashSet<String>) {
        for (packageName in packages) {
            val plan = HyperOsNativeProxyBindMounter.createPlan(context, packageName)
            if (!plan.valid()) {
                DpisLog.i("HyperOS Native Proxy refresh skipped package=$packageName reason=${plan.reason()}")
                continue
            }
            val result = HyperOsNativeProxyBindMounter.apply(plan)
            DpisLog.i(
                "HyperOS Native Proxy refresh package=$packageName " +
                        "success=${result.success()} output=${result.output()}",
            )
        }
    }
}
