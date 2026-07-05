package com.dpis.module.hooks;

import com.dpis.module.FontHookArbitration;
import com.dpis.module.FontHookDomainRegistry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class HookDomainPlan {
    public final Set<String> enabledDomains;
    public final Set<String> builtinDomains;
    public final Set<String> unknownCustomDomains;
    public final String source;
    public final String reason;

    public HookDomainPlan(Set<String> enabledDomains,
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

    public boolean hasResourcesFont() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_RESOURCES_FONT);
    }

    public boolean hasSystemServerFont() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT);
    }

    public boolean hasActivityThreadFont() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT);
    }

    public boolean hasTextViewHooks() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE)
                || enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE)
                || enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK)
                || enabledDomains.contains(FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK);
    }

    public boolean hasTextViewSpRewrite() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE);
    }

    public boolean hasTextViewAbsoluteRewrite() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE);
    }

    public boolean hasTextViewCurrentPxFallback() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK);
    }

    public boolean hasPaintFallback() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK);
    }

    public boolean hasWebViewTextZoom() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM);
    }

    public boolean hasFlutterSettings() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_FLUTTER_SETTINGS);
    }

    public boolean hasHyperOsNativeFlutter() {
        return enabledDomains.contains(FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER);
    }

    public String enabledDomainsCsv() {
        return enabledDomains.isEmpty() ? "" : String.join(",", enabledDomains);
    }

    public String builtinDomainsCsv() {
        return builtinDomains.isEmpty() ? "" : String.join(",", builtinDomains);
    }

    public String unknownDomainsCsv() {
        return unknownCustomDomains.isEmpty() ? "" : String.join(",", unknownCustomDomains);
    }

    public FontHookArbitration.FontDomainPlan toFontDomainPlan() {
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
