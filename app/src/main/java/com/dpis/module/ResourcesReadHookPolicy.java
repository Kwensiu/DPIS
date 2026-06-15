package com.dpis.module;

final class ResourcesReadHookPolicy {
    static final ResourcesReadHookPolicy FULL = new ResourcesReadHookPolicy(
            true,
            true,
            false);

    final boolean viewportHandlingEnabled;
    final boolean configurationFontOverrideEnabled;
    final boolean metricsTargetFontOverrideEnabled;

    ResourcesReadHookPolicy(boolean viewportHandlingEnabled,
                            boolean configurationFontOverrideEnabled,
                            boolean metricsTargetFontOverrideEnabled) {
        this.viewportHandlingEnabled = viewportHandlingEnabled;
        this.configurationFontOverrideEnabled = configurationFontOverrideEnabled;
        this.metricsTargetFontOverrideEnabled = metricsTargetFontOverrideEnabled;
    }
}
