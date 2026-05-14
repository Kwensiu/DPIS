package com.dpis.displaytool;

public final class CompanionContract {
    public static final String STAGE = "phase1";
    public static final String CONTROL_ACTION = "io.github.kwensiu.dpis.displaytool.CONTROL";

    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_SCENE = "scene";
    public static final String EXTRA_VARIANT = "variant";
    public static final String EXTRA_TRIGGER = "trigger";
    public static final String EXTRA_GROUP = "group";
    public static final String EXTRA_FROM_CONTROL_RECEIVER = "from_control_receiver";

    public static final String ACTION_RUN_ALL = "run_all";
    public static final String ACTION_RUN_SCENE = "run_scene";
    public static final String ACTION_SHOW_SCENE = "show_scene";
    public static final String ACTION_DUMP_SUMMARY = "dump_summary";
    public static final String ACTION_RESET_STATE = "reset_state";

    public static final String TRIGGER_COLD_START = "cold_start";
    public static final String TRIGGER_ADB = "adb";

    public static final String GROUP_NATIVE = "native";
    public static final String GROUP_COMPOSE = "compose";

    public static final String VARIANT_NORMAL = "normal";
    public static final String VARIANT_FRAGILE = "fragile";
    public static final String VARIANT_MODE_NORMAL_ONLY = "normal_only";
    public static final String VARIANT_MODE_SINGLE = "single";

    private CompanionContract() {
    }
}
