package com.dpis.module;

import java.util.LinkedHashSet;
import java.util.Set;

final class HookDomainOverrideStore {
    private final DpisConfigStore configStore;

    HookDomainOverrideStore(DpisConfigStore configStore) {
        this.configStore = configStore;
    }

    HookDomainOverride read(String packageName) {
        if (configStore == null || packageName == null || packageName.isBlank()) {
            return HookDomainOverride.automatic();
        }
        String raw = configStore.getPackageFontHookDomainsRaw(packageName);
        return fromRaw(raw);
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
        String raw = rawValueForSelection(enabledKnownDomains, automaticKnownDomains, unknownDomains);
        if (raw == null) {
            return restoreRecommended(packageName);
        }
        return save(packageName, enabledKnownDomains, unknownDomains);
    }

    boolean restoreRecommended(String packageName) {
        if (configStore == null || packageName == null || packageName.isBlank()) {
            return false;
        }
        // "Recommended" means no custom override for the compat hook-chain
        // template. System-mode font routes are scheduled separately.
        return configStore.clearPackageFontHookDomainsRaw(packageName);
    }

    static HookDomainOverride fromRaw(String raw) {
        if (raw == null) {
            return HookDomainOverride.automatic();
        }
        LinkedHashSet<String> known = new LinkedHashSet<>();
        LinkedHashSet<String> unknown = new LinkedHashSet<>();
        parseCsv(raw, known, unknown);
        return new HookDomainOverride(true,
                FontHookDomainRegistry.orderedCustomizableSubset(known),
                unknown);
    }

    static String rawValueForSelection(Set<String> enabledKnownDomains,
            Set<String> automaticKnownDomains,
            Set<String> unknownDomains) {
        LinkedHashSet<String> normalizedSaved = normalizedCustomizableDomains(
                enabledKnownDomains);
        if (selectionMatchesAutomatic(normalizedSaved, automaticKnownDomains, unknownDomains)) {
            return null;
        }
        return formatCsv(normalizedSaved, unknownDomains);
    }

    static HookDomainOverride automaticIfSelectionMatchesAutomatic(
            HookDomainOverride override,
            Set<String> automaticKnownDomains) {
        if (override == null || !override.customPathEnabled) {
            return override != null ? override : HookDomainOverride.automatic();
        }
        if (selectionMatchesAutomatic(
                override.enabledKnownDomains,
                automaticKnownDomains,
                override.unknownDomains)) {
            return HookDomainOverride.automatic();
        }
        return override;
    }

    private static boolean selectionMatchesAutomatic(Set<String> enabledKnownDomains,
            Set<String> automaticKnownDomains,
            Set<String> unknownDomains) {
        return normalizedCustomizableDomains(enabledKnownDomains).equals(
                normalizedCustomizableDomains(automaticKnownDomains))
                && (unknownDomains == null || unknownDomains.isEmpty());
    }

    private static LinkedHashSet<String> normalizedCustomizableDomains(Set<String> domains) {
        return new LinkedHashSet<>(FontHookDomainRegistry.orderedCustomizableSubset(domains));
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

    static String formatCsv(Set<String> enabledKnownDomains, Set<String> unknownDomains) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>(
                FontHookDomainRegistry.orderedCustomizableSubset(enabledKnownDomains));
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
