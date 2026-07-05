package com.dpis.module;

import android.content.Context;

import java.util.Set;

public final class FontHookDomainPresentation {
    private final HookDomainOverride override;

    private FontHookDomainPresentation(HookDomainOverride override) {
        this.override = override != null ? override : HookDomainOverride.automatic();
    }

    public static FontHookDomainPresentation forRecommendedTemplateRaw(String raw) {
        return forOverride(
                HookDomainOverrideStore.fromRaw(raw),
                FontHookDomainRegistry.recommendedTemplateKnownDomains());
    }

    public static FontHookDomainPresentation forOverride(HookDomainOverride override,
            Set<String> automaticKnownDomains) {
        return new FontHookDomainPresentation(
                HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
                        override,
                        automaticKnownDomains));
    }

    HookDomainOverride override() {
        return override;
    }

    boolean displaysAsAutomatic() {
        return !override.customPathEnabled;
    }

    public String normalizedRawOrNull() {
        if (displaysAsAutomatic()) {
            return null;
        }
        return HookDomainOverrideStore.formatCsv(
                override.enabledKnownDomains,
                override.unknownDomains);
    }

    public String buttonText(Context context) {
        if (displaysAsAutomatic()) {
            return context.getString(R.string.dialog_font_hook_domains_title);
        }
        return context.getString(
                R.string.dialog_font_hook_domains_title_with_count,
                selectedDisplayCount(),
                totalDisplayCount());
    }

    int selectedDisplayCount() {
        return FontHookDomainRegistry.orderedCustomizableDisplaySubset(
                override.enabledKnownDomains).size();
    }

    int totalDisplayCount() {
        return FontHookDomainRegistry.orderedCustomizableDisplayIdsList().size();
    }
}
