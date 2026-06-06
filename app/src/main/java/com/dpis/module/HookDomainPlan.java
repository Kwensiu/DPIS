package com.dpis.module;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

final class HookDomainPlan {
    final Set<String> enabledDomains;
    final Set<String> builtinDomains;
    final Set<String> unknownCustomDomains;
    final String source;
    final String reason;

    HookDomainPlan(Set<String> enabledDomains,
                   Set<String> builtinDomains,
                   Set<String> unknownCustomDomains,
                   String source,
                   String reason) {
        this.enabledDomains = Collections.unmodifiableSet(enabledDomains != null
                ? FontHookDomainRegistry.orderedKnownSubset(enabledDomains)
                : new LinkedHashSet<>());
        this.builtinDomains = Collections.unmodifiableSet(builtinDomains != null
                ? FontHookDomainRegistry.orderedKnownSubset(builtinDomains)
                : new LinkedHashSet<>());
        this.unknownCustomDomains = unknownCustomDomains != null
                ? Collections.unmodifiableSet(normalizeUnknownDomains(unknownCustomDomains))
                : Collections.emptySet();
        this.source = source != null ? source : "auto";
        this.reason = reason != null ? reason : "none";
    }

    boolean hasResourcesFont() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_RESOURCES_FONT);
    }

    boolean hasSystemServerFont() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT);
    }

    boolean hasActivityThreadFont() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT);
    }

    boolean hasTextViewHooks() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE)
                || enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE)
                || enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK)
                || enabledDomains.contains(FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK);
    }

    boolean hasTextViewSpRewrite() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE);
    }

    boolean hasTextViewAbsoluteRewrite() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE);
    }

    boolean hasTextViewCurrentPxFallback() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK);
    }

    boolean hasPaintFallback() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK);
    }

    boolean hasWebViewTextZoom() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM);
    }

    boolean hasFlutterSettings() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_FLUTTER_SETTINGS);
    }

    boolean hasHyperOsNativeFlutter() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER);
    }

    String enabledDomainsCsv() {
        return enabledDomains.isEmpty() ? "" : String.join(",", enabledDomains);
    }

    String builtinDomainsCsv() {
        return builtinDomains.isEmpty() ? "" : String.join(",", builtinDomains);
    }

    String unknownDomainsCsv() {
        return unknownCustomDomains.isEmpty() ? "" : String.join(",", unknownCustomDomains);
    }

    FontHookArbitration.FontDomainPlan toFontDomainPlan() {
        return new FontHookArbitration.FontDomainPlan(
                hasResourcesFont(),
                hasWebViewTextZoom(),
                hasTextViewHooks(),
                hasTextViewSpRewrite(),
                hasTextViewAbsoluteRewrite(),
                hasTextViewCurrentPxFallback(),
                hasPaintFallback(),
                hasFlutterSettings(),
                hasHyperOsNativeFlutter(),
                false,
                reason);
    }

    private static Set<String> normalizeUnknownDomains(Set<String> domains) {
        LinkedHashSet<String> unknown = new LinkedHashSet<>();
        for (String domain : domains) {
            String id = domain == null ? "" : domain.trim();
            if (!id.isEmpty() && !FontHookDomainRegistry.isKnown(id)) {
                unknown.add(id);
            }
        }
        return unknown;
    }
}
