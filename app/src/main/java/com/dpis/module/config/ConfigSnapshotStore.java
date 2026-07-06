package com.dpis.module.config;

import com.dpis.module.viewport.ViewportTargetSpec;

import java.util.Set;

public interface ConfigSnapshotStore {
    Set<String> getConfiguredPackages();

    boolean isTargetDpisEnabled(String packageName);

    ViewportTargetSpec getTargetViewportSpec(String packageName);

    String getTargetViewportApplyMode(String packageName);

    Integer getTargetFontScalePercent(String packageName);

    String getTargetFontApplyMode(String packageName);

    String getTargetTypefaceId(String packageName);

    String getPackageFontHookDomainsRaw(String packageName);

    boolean isSystemServerHooksEnabled();

    boolean isSystemServerSafeModeEnabled();

    boolean isGlobalLogEnabled();

    boolean hasSystemServerHooksEnabled();

    boolean hasSystemServerSafeModeEnabled();

    boolean hasGlobalLogEnabled();
}
