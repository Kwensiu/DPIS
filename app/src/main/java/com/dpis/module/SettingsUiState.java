package com.dpis.module;

/** Immutable presentation snapshot; persistence and workflow execution stay Java-owned. */
public final class SettingsUiState {
    public final boolean storeAvailable;
    public final boolean systemHooksEnabled;
    public final boolean safeModeEnabled;
    public final boolean globalLogEnabled;
    public final boolean launcherIconHidden;
    public final int interfaceScalePercent;
    public final boolean cacheClearInProgress;
    public final String cacheUsage;
    public final String languageLabel;

    SettingsUiState(boolean storeAvailable, boolean systemHooksEnabled, boolean safeModeEnabled,
            boolean globalLogEnabled, boolean launcherIconHidden, int interfaceScalePercent,
            boolean cacheClearInProgress, String cacheUsage, String languageLabel) {
        this.storeAvailable = storeAvailable;
        this.systemHooksEnabled = systemHooksEnabled;
        this.safeModeEnabled = safeModeEnabled;
        this.globalLogEnabled = globalLogEnabled;
        this.launcherIconHidden = launcherIconHidden;
        this.interfaceScalePercent = interfaceScalePercent;
        this.cacheClearInProgress = cacheClearInProgress;
        this.cacheUsage = cacheUsage != null ? cacheUsage : "";
        this.languageLabel = languageLabel != null ? languageLabel : "";
    }
}
