package com.dpis.module;

final class TemplateTypefaceResolver implements TemplateConfigSummaryFormatter.TypefaceResolver {
    interface FontLibraryProvider {
        FontLibraryStore get();
    }

    interface SystemTypefaceProvider {
        boolean canLoad(String typefaceId);

        String displayName(String typefaceId);
    }

    private final FontLibraryProvider fontLibraryProvider;
    private final SystemTypefaceProvider systemTypefaceProvider;

    TemplateTypefaceResolver(FontLibraryProvider fontLibraryProvider) {
        this(fontLibraryProvider, new AndroidSystemTypefaceProvider());
    }

    TemplateTypefaceResolver(FontLibraryProvider fontLibraryProvider,
            SystemTypefaceProvider systemTypefaceProvider) {
        this.fontLibraryProvider = fontLibraryProvider;
        this.systemTypefaceProvider = systemTypefaceProvider != null
                ? systemTypefaceProvider
                : new AndroidSystemTypefaceProvider();
    }

    @Override
    public TemplateConfigSummaryFormatter.TypefaceStatus resolve(String typefaceId) {
        if (typefaceId == null || typefaceId.isBlank()) {
            return TemplateConfigSummaryFormatter.TypefaceStatus.none();
        }
        if (SystemFontRegistry.isSystemFontId(typefaceId)) {
            if (systemTypefaceProvider.canLoad(typefaceId)) {
                return TemplateConfigSummaryFormatter.TypefaceStatus.resolved(
                        typefaceId, systemTypefaceProvider.displayName(typefaceId));
            }
            return TemplateConfigSummaryFormatter.TypefaceStatus.missing(typefaceId);
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

    private static final class AndroidSystemTypefaceProvider implements SystemTypefaceProvider {
        @Override
        public boolean canLoad(String typefaceId) {
            return SystemFontRegistry.loadTypeface(typefaceId) != null;
        }

        @Override
        public String displayName(String typefaceId) {
            for (SystemFontEntry entry : SystemFontRegistry.listRecommendedFonts()) {
                if (typefaceId.equals(entry.id)) {
                    return entry.displayName;
                }
            }
            return typefaceId;
        }
    }
}
