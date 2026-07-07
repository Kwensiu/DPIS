package com.dpis.module.hooks;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class HookDomainOverride {
    public final boolean customPathEnabled;
    public final Set<String> enabledKnownDomains;
    public final Set<String> unknownDomains;

    public HookDomainOverride(boolean customPathEnabled,
                       Set<String> enabledKnownDomains,
                       Set<String> unknownDomains) {
        this.customPathEnabled = customPathEnabled;
        this.enabledKnownDomains = Collections.unmodifiableSet(new LinkedHashSet<>(
                enabledKnownDomains != null ? enabledKnownDomains : Collections.emptySet()));
        this.unknownDomains = Collections.unmodifiableSet(new LinkedHashSet<>(
                unknownDomains != null ? unknownDomains : Collections.emptySet()));
    }

    public static HookDomainOverride automatic() {
        return new HookDomainOverride(false, Collections.emptySet(), Collections.emptySet());
    }
}
