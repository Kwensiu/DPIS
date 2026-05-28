package com.dpis.module;

final class TemplateTypefaceResolver implements TemplateConfigSummaryFormatter.TypefaceResolver {
    interface FontLibraryProvider {
        FontLibraryStore get();
    }

    private final FontLibraryProvider fontLibraryProvider;

    TemplateTypefaceResolver(FontLibraryProvider fontLibraryProvider) {
        this.fontLibraryProvider = fontLibraryProvider;
    }

    @Override
    public TemplateConfigSummaryFormatter.TypefaceStatus resolve(String typefaceId) {
        if (typefaceId == null || typefaceId.isBlank()) {
            return TemplateConfigSummaryFormatter.TypefaceStatus.none();
        }
        for (SystemFontEntry entry : SystemFontRegistry.listRecommendedFonts()) {
            if (typefaceId.equals(entry.id)) {
                return TemplateConfigSummaryFormatter.TypefaceStatus.resolved(
                        typefaceId, entry.displayName);
            }
        }
        if (SystemFontRegistry.isSystemFontId(typefaceId)) {
            return TemplateConfigSummaryFormatter.TypefaceStatus.resolved(typefaceId, typefaceId);
        }
        FontLibraryStore fontLibraryStore = fontLibraryProvider != null
                ? fontLibraryProvider.get()
                : null;
        if (fontLibraryStore == null) {
            return TemplateConfigSummaryFormatter.TypefaceStatus.missing(typefaceId);
        }
        FontLibraryEntry imported = fontLibraryStore.findById(typefaceId);
        if (imported != null && fontLibraryStore.resolveFontFile(typefaceId) != null) {
            return TemplateConfigSummaryFormatter.TypefaceStatus.resolved(
                    typefaceId, imported.displayName);
        }
        return TemplateConfigSummaryFormatter.TypefaceStatus.missing(typefaceId);
    }
}
