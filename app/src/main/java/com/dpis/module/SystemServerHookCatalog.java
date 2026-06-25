package com.dpis.module;

final class SystemServerHookCatalog {
    static final SystemServerHookSpec LAUNCH_ACTIVITY_ITEM =
            SystemServerHookSpec.constructor(
                    "launch-activity-item",
                    "system_server_launch_activity_item",
                    new String[]{
                            "android.app.servertransaction.LaunchActivityItem"
                    });
    static final SystemServerHookSpec CONFIG_DISPATCH =
            SystemServerHookSpec.method(
                    "config-dispatch",
                    "system_server_config_dispatch",
                    new String[]{
                            "com.android.server.wm.ActivityRecord"
                    },
                    new String[]{
                            "updateReportedConfigurationAndSend"
                    });
    static final SystemServerHookSpec ACTIVITY_START =
            SystemServerHookSpec.method(
                    "activity-start",
                    "system_server_activity_start",
                    new String[]{
                            "com.android.server.wm.ActivityStarter",
                            "com.android.server.am.ActivityStarter"
                    },
                    new String[]{
                            "execute",
                            "startActivityMayWait"
                    });
    static final SystemServerHookSpec RELAYOUT_DISPATCH =
            SystemServerHookSpec.method(
                    "relayout-dispatch",
                    "system_server_relayout_dispatch",
                    new String[]{
                            "com.android.server.wm.WindowManagerService"
                    },
                    new String[]{
                            "relayoutWindow"
                    });
    static final SystemServerHookSpec DISPLAY_POLICY_LAYOUT =
            SystemServerHookSpec.method(
                    "display-policy-layout",
                    "system_server_display_policy_layout",
                    new String[]{
                            "com.android.server.wm.DisplayPolicy"
                    },
                    new String[]{
                            "layoutWindowLw"
                    });
    static final SystemServerHookSpec DISPLAY_CONTENT_CONFIG =
            SystemServerHookSpec.method(
                    "display-content-config",
                    "system_server_display_content_config",
                    new String[]{
                            "com.android.server.wm.DisplayContent"
                    },
                    new String[]{
                            "computeScreenConfiguration",
                            "updateDisplayAndOrientation",
                            "getDisplayInfo"
                    });
    static final SystemServerHookSpec DISPLAY_MANAGER_INFO =
            SystemServerHookSpec.method(
                    "display-manager-info",
                    "system_server_display_manager_info",
                    new String[]{
                            "com.android.server.display.DisplayManagerService$BinderService",
                            "com.android.server.display.DisplayManagerService"
                    },
                    new String[]{
                            "getDisplayInfo",
                            "getDisplayInfoInternal"
                    });

    private static final SystemServerHookSpec[] METHOD_HOOK_SPECS = new SystemServerHookSpec[]{
            CONFIG_DISPATCH,
            ACTIVITY_START,
            RELAYOUT_DISPATCH,
            DISPLAY_POLICY_LAYOUT,
            DISPLAY_CONTENT_CONFIG,
            DISPLAY_MANAGER_INFO
    };

    private SystemServerHookCatalog() {
    }

    static SystemServerHookSpec[] methodHookSpecs() {
        return METHOD_HOOK_SPECS.clone();
    }
}
