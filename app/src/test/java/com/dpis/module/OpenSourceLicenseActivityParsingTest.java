package com.dpis.module;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class OpenSourceLicenseActivityParsingTest {
    @Test
    public void parserReadsCatalogAndStringLicenseReferences() throws IOException {
        String source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java");

        assertTrue(source.contains("JSONObject licenseCatalog = root.optJSONObject(\"licenses\")"));
        assertTrue(source.contains("resolveLicenses(library.optJSONArray(\"licenses\"), licenseCatalog)"));
        assertTrue(source.contains("if (entry instanceof String)"));
        assertTrue(source.contains("licenseCatalog.optJSONObject(normalizedKey)"));
    }

    @Test
    public void parserBuildsLicenseDetailWithResolvedContent() throws IOException {
        String source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java");

        assertTrue(source.contains("static String buildLicenseDetail"));
        assertTrue(source.contains("detailBuilder.append(license.name)"));
        assertTrue(source.contains("detailBuilder.append(\"\\n\\n\").append(license.content)"));
    }

    @Test
    public void licensePageIncludesDpisProjectLicense() throws IOException {
        String source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("createProjectLicenseItem()"));
        assertTrue(source.contains("R.raw.gpl_3_0"));
        assertTrue(strings.contains("open_source_license_project_summary"));
        assertTrue(strings.contains("GPL-3.0-or-later"));
    }

    @Test
    public void licenseLayoutsUseNamedDimensions() throws IOException {
        String pageLayout = read("src/main/res/layout/activity_open_source_license.xml");
        String itemLayout = read("src/main/res/layout/item_open_source_license.xml");

        assertTrue(pageLayout.contains("@dimen/open_source_license_padding_horizontal"));
        assertTrue(pageLayout.contains("@dimen/page_card_corner_radius"));
        assertTrue(pageLayout.contains("@dimen/open_source_license_divider_height"));
        assertTrue(itemLayout.contains("@dimen/open_source_license_item_min_height"));
        assertTrue(itemLayout.contains("@dimen/open_source_license_item_padding_vertical"));
        assertTrue(itemLayout.contains("@dimen/open_source_license_item_summary_spacing_top"));
    }

    @Test
    public void licenseDetailDialogUsesMaterialLargeWidth() throws IOException {
        String source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java");

        assertTrue(source.contains("new MaterialAlertDialogBuilder(this)"));
        assertTrue(source.contains("androidx.appcompat.app.AlertDialog dialog = builder.create();"));
        assertTrue(source.contains("DialogWindowSizer.applyLargeWidth(dialog, this);"));
    }

    @Test
    public void notFoundPathShowsMissingThirdPartyLicenseIndicator() throws IOException {
        String source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java")
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
        return SourceSmokeTestPaths.read(relativePath);
    }
}
