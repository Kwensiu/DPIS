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

    @Test
    public void licensePageIncludesDpisProjectLicense() throws IOException {
        String source = read("src/main/java/com/dpis/module/OpenSourceLicenseActivity.java");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("createProjectLicenseItem()"));
        assertTrue(source.contains("R.raw.gpl_3_0"));
        assertTrue(strings.contains("open_source_license_project_summary"));
        assertTrue(strings.contains("GPL-3.0-or-later"));
    }

    @Test
    public void notFoundPathShowsMissingThirdPartyLicenseIndicator() throws IOException {
        String source = read("src/main/java/com/dpis/module/OpenSourceLicenseActivity.java")
                .replace("\r\n", "\n");
        int notFoundCatch = source.indexOf("} catch (Resources.NotFoundException e) {");
        int throwableCatch = source.indexOf("} catch (Throwable t) {", notFoundCatch);

        assertTrue(notFoundCatch >= 0);
        assertTrue(throwableCatch > notFoundCatch);

        String notFoundBranch = source.substring(notFoundCatch, throwableCatch);
        assertTrue(notFoundBranch.contains("List<LicenseItem> items = new ArrayList<>();"));
        assertTrue(notFoundBranch.contains("items.add(createProjectLicenseItem());"));
        assertTrue(notFoundBranch.contains(
                "items.add(emptyItem(getString(R.string.open_source_license_empty)));"));
        assertTrue(notFoundBranch.contains("return items;"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
