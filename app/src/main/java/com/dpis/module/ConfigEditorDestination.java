package com.dpis.module;

/** Visible destination inside one app or template configuration editing session. */
public enum ConfigEditorDestination {
    MAIN,
    HOOK_CHAIN_INTERFACE,
    HOOK_CHAIN_FONT;

    public boolean isHookChain() {
        return this != MAIN;
    }

    public int hookChainTabIndex() {
        return this == HOOK_CHAIN_FONT ? 1 : 0;
    }

    /** A child destination always returns to the main content of the same editor session. */
    public ConfigEditorDestination backDestination() {
        return MAIN;
    }

    public static ConfigEditorDestination forHookChainTab(int index) {
        return index == 1 ? HOOK_CHAIN_FONT : HOOK_CHAIN_INTERFACE;
    }

    public static ConfigEditorDestination fromName(String name) {
        if (name == null) return MAIN;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return MAIN;
        }
    }
}
