package com.dpis.module.fonts.hookdomain;

import com.dpis.module.hyperos.HyperOsNativeRoutePolicy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class PackageFontHookDomainDefaults {
    private static final Map<String, Set<String>> EXACT_DEFAULTS = createDefaults();

    private PackageFontHookDomainDefaults() {
    }

    public static Set<String> resolveExactDefaults(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> defaults = EXACT_DEFAULTS.get(packageName);
        return defaults == null ? Collections.emptySet() : defaults;
    }

    private static Map<String, Set<String>> createDefaults() {
        // Last-resort compatibility supplements for runtimes with a known
        // extra domain owner. Do not use this table to hide lifecycle risk in
        // a target app; scheduler policy owns those fallbacks globally.
        LinkedHashMap<String, Set<String>> table = new LinkedHashMap<>();
        Set<String> hyperOsNativeFlutter = Set.of(FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER);
        for (String packageName : HyperOsNativeRoutePolicy.knownPackages()) {
            table.put(packageName, hyperOsNativeFlutter);
        }
        return Collections.unmodifiableMap(table);
    }

}
