package com.dpis.module;

enum MainWorkspaceMode {
    APP,
    TEMPLATE;

    static MainWorkspaceMode fromName(String name) {
        if (name == null) {
            return APP;
        }
        try {
            return MainWorkspaceMode.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return APP;
        }
    }
}
