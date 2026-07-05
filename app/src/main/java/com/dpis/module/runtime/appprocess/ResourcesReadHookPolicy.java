package com.dpis.module.runtime.appprocess;

import com.dpis.module.*;

public final class ResourcesReadHookPolicy {
    public static final ResourcesReadHookPolicy FULL = new ResourcesReadHookPolicy(
            true,
            true,
            false);

    public final boolean viewportHandlingEnabled;
    public final boolean configurationFontOverrideEnabled;
    public final boolean metricsTargetFontOverrideEnabled;

    public ResourcesReadHookPolicy(boolean viewportHandlingEnabled,
                            boolean configurationFontOverrideEnabled,
                            boolean metricsTargetFontOverrideEnabled) {
        this.viewportHandlingEnabled = viewportHandlingEnabled;
        this.configurationFontOverrideEnabled = configurationFontOverrideEnabled;
        this.metricsTargetFontOverrideEnabled = metricsTargetFontOverrideEnabled;
    }
}
