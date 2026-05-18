package com.dpis.module;

import java.util.LinkedHashSet;
import java.util.Set;

final class HookDomainOverrideStore {
    private final DpiConfigStore configStore;

    HookDomainOverrideStore(DpiConfigStore configStore) {
        this.configStore = configStore;
    }

    HookDomainOverride read(String packageName) {
        if (configStore == null || packageName == null || packageName.isBlank()) {
            return HookDomainOverride.automatic();
        }
        String raw = configStore.getPackageFontHookDomainsRaw(packageName);
        if (raw == null) {
            return HookDomainOverride.automatic();
        }
        LinkedHashSet<String> known = new LinkedHashSet<>();
        LinkedHashSet<String> unknown = new LinkedHashSet<>();
        parseCsv(raw, known, unknown);
        return new HookDomainOverride(true, known, unknown);
    }

    boolean save(String packageName, Set<String> enabledKnownDomains, Set<String> unknownDomains) {
        if (configStore == null || packageName == null || packageName.isBlank()) {
            return false;
        }
        String raw = formatCsv(
                FontHookDomainRegistry.orderedCustomizableSubset(enabledKnownDomains),
                unknownDomains);
        return configStore.setPackageFontHookDomainsRaw(packageName, raw);
    }

    boolean saveCustomIfDifferentFromAutomatic(String packageName,
                                               Set<String> enabledKnownDomains,
                                               Set<String> automaticKnownDomains,
                                               Set<String> unknownDomains) {
        LinkedHashSet<String> normalizedSaved = new LinkedHashSet<>(
                FontHookDomainRegistry.orderedCustomizableSubset(enabledKnownDomains));
        LinkedHashSet<String> normalizedAuto = new LinkedHashSet<>(
                FontHookDomainRegistry.orderedCustomizableSubset(automaticKnownDomains));
        if (normalizedSaved.equals(normalizedAuto)
                && (unknownDomains == null || unknownDomains.isEmpty())) {
            return restoreRecommended(packageName);
        }
        return save(packageName, normalizedSaved, unknownDomains);
    }

    boolean restoreRecommended(String packageName) {
        if (configStore == null || packageName == null || packageName.isBlank()) {
            return false;
        }
        return configStore.clearPackageFontHookDomainsRaw(packageName);
    }

    private static void parseCsv(String raw, Set<String> known, Set<String> unknown) {
        if (raw.isEmpty()) {
            return;
        }
        String[] parts = raw.split(",");
        for (String part : parts) {
            String id = part == null ? "" : part.trim();
            if (id.isEmpty()) {
                continue;
            }
            if (FontHookDomainRegistry.isKnown(id)) {
                known.add(id);
            } else {
                unknown.add(id);
            }
        }
    }

    private static String formatCsv(Set<String> enabledKnownDomains, Set<String> unknownDomains) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>(
                FontHookDomainRegistry.orderedKnownSubset(enabledKnownDomains));
        if (unknownDomains != null) {
            for (String unknown : unknownDomains) {
                String id = unknown == null ? "" : unknown.trim();
                if (!id.isEmpty() && !FontHookDomainRegistry.isKnown(id)) {
                    ordered.add(id);
                }
            }
        }
        if (ordered.isEmpty()) {
            return "";
        }
        return String.join(",", ordered);
    }
}
