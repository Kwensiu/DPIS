package com.dpis.module;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpenSourceLicenseActivityParsingTest {
    @Test
    public void parserReadsCatalogAndStringLicenseReferences() throws IOException {
        String source = read("src/main/java/com/dpis/module/OpenSourceLicenseActivity.java");

        assertTrue(source.contains("JSONObject licenseCatalog = root.optJSONObject(\"licenses\")"));
        assertTrue(source.contains("resolveLicenses(library.optJSONArray(\"licenses\"), licenseCatalog)"));
        assertTrue(source.contains("if (entry instanceof String)"));
        assertTrue(source.contains("licenseCatalog.optJSONObject(normalizedKey)"));
    }

    @Test
    public void parserBuildsLicenseDetailWithResolvedContent() throws IOException {
        String source = read("src/main/java/com/dpis/module/OpenSourceLicenseActivity.java");

        assertTrue(source.contains("static String buildLicenseDetail"));
        assertTrue(source.contains("detailBuilder.append(license.name)"));
        assertTrue(source.contains("detailBuilder.append(\"\\n\\n\").append(license.content)"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
