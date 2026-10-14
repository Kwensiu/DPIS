package com.dpis.module.runtime.systemserver.hook

object EntryRoute {
    private const val ENTRY_CONFIG_DISPATCH = "config-dispatch"
    private const val ENTRY_ACTIVITY_START = "activity-start"
    private const val ENTRY_LAUNCH_ACTIVITY_ITEM = "launch-activity-item"
    private const val ENTRY_HYPEROS_RUST_PROCESS = "hyperos-rust-process"
    private const val ENTRY_DISPLAY_MANAGER_INFO = "display-manager-info"
    private const val ENTRY_RELAYOUT_DISPATCH = "relayout-dispatch"
    private const val ENTRY_DISPLAY_POLICY_LAYOUT = "display-policy-layout"
    private const val ENTRY_DISPLAY_CONTENT_CONFIG = "display-content-config"

    @JvmStatic
    fun isConfigDispatch(entryName: String?): Boolean = entryName == ENTRY_CONFIG_DISPATCH

    @JvmStatic
    fun isActivityStart(entryName: String?): Boolean = entryName == ENTRY_ACTIVITY_START

    @JvmStatic
    fun isLaunchActivityItem(entryName: String?): Boolean = entryName == ENTRY_LAUNCH_ACTIVITY_ITEM

    @JvmStatic
    fun isHyperOsRustProcess(entryName: String?): Boolean = entryName == ENTRY_HYPEROS_RUST_PROCESS

    @JvmStatic
    fun isDisplayManagerInfo(entryName: String?): Boolean = entryName == ENTRY_DISPLAY_MANAGER_INFO

    @JvmStatic
    fun isRelayoutDispatch(entryName: String?): Boolean = entryName == ENTRY_RELAYOUT_DISPATCH

    @JvmStatic
    fun isDisplayPolicyLayout(entryName: String?): Boolean =
        entryName == ENTRY_DISPLAY_POLICY_LAYOUT

    @JvmStatic
    fun isDisplayContentConfig(entryName: String?): Boolean =
        entryName == ENTRY_DISPLAY_CONTENT_CONFIG

    @JvmStatic
    fun isPreProceedMutationEntry(entryName: String?): Boolean =
        isConfigDispatch(entryName) || isActivityStart(entryName)

    @JvmStatic
    fun isPostProceedMutationEntry(entryName: String?): Boolean = !isConfigDispatch(entryName)

    @JvmStatic
    fun isLaunchOnlyMutationEntry(entryName: String?): Boolean = isLaunchActivityItem(entryName)

    @JvmStatic
    fun isLowRiskSystemServerEntry(entryName: String?): Boolean =
        isActivityStart(entryName) ||
                isConfigDispatch(entryName) ||
                isRelayoutDispatch(entryName) ||
                isLaunchActivityItem(entryName) ||
                isDisplayManagerInfo(entryName) ||
                isHyperOsRustProcess(entryName)

    @JvmStatic
    fun isHotEntry(entryName: String?): Boolean =
        isDisplayPolicyLayout(entryName) || isRelayoutDispatch(entryName)

    @JvmStatic
    fun isCoreLogEntry(entryName: String?): Boolean =
        isActivityStart(entryName) ||
                isConfigDispatch(entryName) ||
                isDisplayContentConfig(entryName)
}
