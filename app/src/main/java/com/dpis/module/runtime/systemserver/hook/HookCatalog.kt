package com.dpis.module.runtime.systemserver.hook

object HookCatalog {
    @JvmField
    val LAUNCH_ACTIVITY_ITEM: HookSpec = HookSpec.newConstructor(
        "launch-activity-item",
        "system_server_launch_activity_item",
        arrayOf("android.app.servertransaction.LaunchActivityItem"),
    )

    @JvmField
    val CONFIG_DISPATCH: HookSpec = HookSpec.method(
        "config-dispatch",
        "system_server_config_dispatch",
        arrayOf("com.android.server.wm.ActivityRecord"),
        arrayOf("updateReportedConfigurationAndSend"),
    )

    @JvmField
    val ACTIVITY_START: HookSpec = HookSpec.method(
        "activity-start",
        "system_server_activity_start",
        arrayOf(
            "com.android.server.wm.ActivityStarter",
            "com.android.server.am.ActivityStarter",
        ),
        arrayOf("execute", "startActivityMayWait"),
    )

    @JvmField
    val RELAYOUT_DISPATCH: HookSpec = HookSpec.method(
        "relayout-dispatch",
        "system_server_relayout_dispatch",
        arrayOf("com.android.server.wm.WindowManagerService"),
        arrayOf("relayoutWindow"),
    )

    @JvmField
    val DISPLAY_POLICY_LAYOUT: HookSpec = HookSpec.method(
        "display-policy-layout",
        "system_server_display_policy_layout",
        arrayOf("com.android.server.wm.DisplayPolicy"),
        arrayOf("layoutWindowLw"),
    )

    @JvmField
    val DISPLAY_CONTENT_CONFIG: HookSpec = HookSpec.method(
        "display-content-config",
        "system_server_display_content_config",
        arrayOf("com.android.server.wm.DisplayContent"),
        arrayOf(
            "computeScreenConfiguration",
            "updateDisplayAndOrientation",
            "getDisplayInfo",
        ),
    )

    @JvmField
    val DISPLAY_MANAGER_INFO: HookSpec = HookSpec.method(
        "display-manager-info",
        "system_server_display_manager_info",
        arrayOf(
            "com.android.server.display.DisplayManagerService\$BinderService",
            "com.android.server.display.DisplayManagerService",
        ),
        arrayOf("getDisplayInfo", "getDisplayInfoInternal"),
    )

    private val METHOD_HOOK_SPECS = arrayOf(
        CONFIG_DISPATCH,
        ACTIVITY_START,
        RELAYOUT_DISPATCH,
        DISPLAY_POLICY_LAYOUT,
        DISPLAY_CONTENT_CONFIG,
        DISPLAY_MANAGER_INFO,
    )

    @JvmStatic
    fun methodHookSpecs(): Array<HookSpec> = METHOD_HOOK_SPECS.clone()
}
