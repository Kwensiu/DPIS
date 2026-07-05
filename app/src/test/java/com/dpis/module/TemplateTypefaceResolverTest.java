package com.dpis.module;

import com.dpis.module.templates.TemplateConfigSummaryFormatter;
import com.dpis.module.templates.TemplateTypefaceResolver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public final class TemplateTypefaceResolverTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void resolvesKnownSystemFontIdWithoutImportedFile() {
        String systemFontId = "system-family:sans-serif";

        TemplateConfigSummaryFormatter.TypefaceStatus status =
                new TemplateTypefaceResolver(
                        typefaceId -> null,
                        new FakeSystemTypefaceProvider(systemFontId, "Sans Serif"))
                        .resolve(systemFontId);

        assertFalse(status.missing);
        assertNotNull(status.displayName);
    }

    @Test
    public void reportsStaleSystemFontIdMissing() {
        String staleSystemFontId = "system-font:not-present-on-this-device";

        TemplateConfigSummaryFormatter.TypefaceStatus status =
                new TemplateTypefaceResolver(typefaceId -> null).resolve(staleSystemFontId);

        assertTrue(status.missing);
        assertFalse(staleSystemFontId.isBlank());
    }

    @Test
    public void resolvesImportedFontOnlyWhenStoredFileExists() throws Exception {
        FontLibraryStore store = new FontLibraryStore(
                new FakePrefs(),
                temporaryFolder.newFolder("fonts"));
        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Example.ttf", "fake-font-data"),
                "Example.ttf",
                1234L);

        TemplateConfigSummaryFormatter.TypefaceStatus status =
                new TemplateTypefaceResolver(importedProvider(store)).resolve(entry.id);

        assertFalse(status.missing);
        assertNotNull(status.displayName);
    }

    @Test
    public void reportsImportedFontMissingWhenOnlyMetadataRemains() throws Exception {
        FontLibraryStore store = new FontLibraryStore(
                new FakePrefs(),
                temporaryFolder.newFolder("fonts"));
        FontLibraryEntry entry = store.registerCopiedFontForTest(
                writeFile("Gone.ttf", "fake-font-data"),
                "Gone.ttf",
                1234L);
        assertTrue(new File(entry.storedPath).delete());

        TemplateConfigSummaryFormatter.TypefaceStatus status =
                new TemplateTypefaceResolver(importedProvider(store)).resolve(entry.id);

        assertTrue(status.missing);
        assertFalse(entry.id.isBlank());
    }

    private File writeFile(String name, String content) throws Exception {
        File file = temporaryFolder.newFile(name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    private static TemplateTypefaceResolver.ImportedTypefaceProvider importedProvider(
            FontLibraryStore store) {
        return typefaceId -> {
            FontLibraryEntry imported = store.findById(typefaceId);
            if (imported != null && store.resolveFontFile(typefaceId) != null) {
                return TemplateConfigSummaryFormatter.TypefaceStatus.resolved(
                        typefaceId, imported.displayName);
            }
            return TemplateConfigSummaryFormatter.TypefaceStatus.missing(typefaceId);
        };
    }

    private static final class FakeSystemTypefaceProvider
            implements TemplateTypefaceResolver.SystemTypefaceProvider {
        private final String loadableId;
        private final String displayName;

        FakeSystemTypefaceProvider(String loadableId, String displayName) {
            this.loadableId = loadableId;
            this.displayName = displayName;
        }

        @Override
        public boolean canLoad(String typefaceId) {
            return loadableId.equals(typefaceId);
        }

        @Override
        public String displayName(String typefaceId) {
            return displayName;
        }
    }
}
