package com.dpis.module.runtime.systemserver

import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.systemserver.hook.EntryRoute
import com.dpis.module.runtime.systemserver.mutation.MutationField

object MutationPolicy {
    @JvmStatic
    fun shouldApplyPreProceedMutations(entryName: String?): Boolean =
        EntryRoute.isPreProceedMutationEntry(entryName)

    @JvmStatic
    fun shouldApplyMutationField(entryName: String?, field: MutationField?): Boolean {
        if (field == null) {
            return false
        }
        // Keep this package-neutral. Hook domains express user intent; this
        // field policy decides where a requested mutation can safely write.
        //
        // FONT_SCALE is launch-only on purpose. Writing Configuration.fontScale
        // during config-dispatch (updateReportedConfigurationAndSend) flips
        // CONFIG_FONT_SCALE and triggers an Activity relaunch, which is worse
        // than leaving the running config untouched. The accepted tradeoff is a
        // runtime base/target split: scaledDensity reaches the target via the
        // app-process resources_font read path, while Configuration.fontScale
        // stays at the system base on later dispatches. Apps that need both
        // values consistent should use compat mode (it unifies them on the
        // app-process read path without going through config-dispatch). See
        // docs/font-routing.md "Why FONT_SCALE is launch-only".
        return when (field) {
            MutationField.VIEWPORT -> true
            MutationField.FONT_SCALE -> EntryRoute.isLaunchOnlyMutationEntry(entryName)
        }
    }

    @JvmStatic
    fun shouldApplyPostProceedMutations(entryName: String?): Boolean =
        EntryRoute.isPostProceedMutationEntry(entryName)

    @JvmStatic
    fun shouldInstallTarget(entryName: String?, safeModeEnabled: Boolean): Boolean {
        if (safeModeEnabled) {
            // Safe mode keeps only the lowest-risk system_server entry.
            return EntryRoute.isLowRiskSystemServerEntry(entryName)
        }
        return true
    }

    @JvmStatic
    fun shouldInstallSystemServerHooks(
        processName: String?,
        packageName: String?,
        policy: HookRuntimePolicy?,
    ): Boolean {
        if (!HostProcess.isSystemServer(processName, packageName)) {
            return false
        }
        return policy == null || policy.systemServerHooksEnabled
    }
}
