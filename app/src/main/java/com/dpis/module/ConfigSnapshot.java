package com.dpis.module;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class ConfigSnapshot {
    private static final ConfigSnapshot EMPTY = new ConfigSnapshot(
            Collections.emptySet(),
            Collections.emptyMap(),
            true,
            true,
            false,
            false,
            false,
            false);

    private final Set<String> configuredPackages;
    private final Map<String, PackageConfigSnapshot> packages;
    private final boolean systemServerHooksEnabled;
    private final boolean systemServerSafeModeEnabled;
    private final boolean globalLogEnabled;
    private final boolean hasSystemServerHooksEnabled;
    private final boolean hasSystemServerSafeModeEnabled;
    private final boolean hasGlobalLogEnabled;

    ConfigSnapshot(Set<String> configuredPackages,
                   Map<String, PackageConfigSnapshot> packages,
                   boolean systemServerHooksEnabled,
                   boolean systemServerSafeModeEnabled,
                   boolean globalLogEnabled,
                   boolean hasSystemServerHooksEnabled,
                   boolean hasSystemServerSafeModeEnabled,
                   boolean hasGlobalLogEnabled) {
        this.configuredPackages = Collections.unmodifiableSet(
                new LinkedHashSet<>(configuredPackages != null
                        ? configuredPackages
                        : Collections.emptySet()));
        this.packages = Collections.unmodifiableMap(
                new LinkedHashMap<>(packages != null
                        ? packages
                        : Collections.emptyMap()));
        this.systemServerHooksEnabled = systemServerHooksEnabled;
        this.systemServerSafeModeEnabled = systemServerSafeModeEnabled;
        this.globalLogEnabled = globalLogEnabled;
        this.hasSystemServerHooksEnabled = hasSystemServerHooksEnabled;
        this.hasSystemServerSafeModeEnabled = hasSystemServerSafeModeEnabled;
        this.hasGlobalLogEnabled = hasGlobalLogEnabled;
    }

    static ConfigSnapshot empty() {
        return EMPTY;
    }

    Set<String> getConfiguredPackages() {
        return configuredPackages;
    }

    PackageConfigSnapshot getPackage(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return null;
        }
        return packages.get(packageName);
    }

    boolean isConfigured(String packageName) {
        return packageName != null && configuredPackages.contains(packageName);
    }

    boolean isSystemServerHooksEnabled() {
        return systemServerHooksEnabled;
    }

    boolean isSystemServerSafeModeEnabled() {
        return systemServerSafeModeEnabled;
    }

    boolean isGlobalLogEnabled() {
        return globalLogEnabled;
    }

    boolean hasSystemServerHooksEnabled() {
        return hasSystemServerHooksEnabled;
    }

    boolean hasSystemServerSafeModeEnabled() {
        return hasSystemServerSafeModeEnabled;
    }

    boolean hasGlobalLogEnabled() {
        return hasGlobalLogEnabled;
    }
}
