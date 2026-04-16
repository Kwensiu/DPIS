package com.dpis.module;

final class PerAppDisplayConfig {
    final String packageName;
    final int targetViewportWidthDp;

    PerAppDisplayConfig(String packageName, int targetViewportWidthDp) {
        this.packageName = packageName;
        this.targetViewportWidthDp = targetViewportWidthDp;
    }
}
