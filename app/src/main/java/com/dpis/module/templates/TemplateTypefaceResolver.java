package com.dpis.module.templates;

import com.dpis.module.fonts.SystemFontEntry;
import com.dpis.module.fonts.SystemFontRegistry;

public final class TemplateTypefaceResolver implements TemplateConfigSummaryFormatter.TypefaceResolver {
    public interface ImportedTypefaceProvider {
        TemplateConfigSummaryFormatter.TypefaceStatus resolve(String typefaceId);
    }

    public interface SystemTypefaceProvider {
        boolean canLoad(String typefaceId);

        String displayName(String typefaceId);
    }

    private final ImportedTypefaceProvider importedTypefaceProvider;
    private final SystemTypefaceProvider systemTypefaceProvider;

    public TemplateTypefaceResolver(ImportedTypefaceProvider importedTypefaceProvider) {
        this(importedTypefaceProvider, new AndroidSystemTypefaceProvider());
    }

    public TemplateTypefaceResolver(ImportedTypefaceProvider importedTypefaceProvider,
            SystemTypefaceProvider systemTypefaceProvider) {
        this.importedTypefaceProvider = importedTypefaceProvider;
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
        if (importedTypefaceProvider != null) {
            TemplateConfigSummaryFormatter.TypefaceStatus imported =
                    importedTypefaceProvider.resolve(typefaceId);
            if (imported != null && imported.resolved()) {
                return imported;
            }
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
                if (typefaceId.equals(entry.id())) {
                    return entry.displayName();
                }
            }
            return typefaceId;
        }
    }
}
