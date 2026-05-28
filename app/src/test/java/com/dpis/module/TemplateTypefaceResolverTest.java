package com.dpis.module;

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
        String systemFontId = SystemFontRegistry.buildFamilyIdForTest("sans-serif");

        TemplateConfigSummaryFormatter.TypefaceStatus status =
                new TemplateTypefaceResolver(() -> null).resolve(systemFontId);

        assertFalse(status.missing);
        assertNotNull(status.displayName);
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
                new TemplateTypefaceResolver(() -> store).resolve(entry.id);

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
                new TemplateTypefaceResolver(() -> store).resolve(entry.id);

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
}
