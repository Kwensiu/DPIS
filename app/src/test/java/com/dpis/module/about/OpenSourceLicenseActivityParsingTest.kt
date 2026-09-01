package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSourceLicenseActivityParsingTest {
    @Test
    fun parserReadsCatalogAndStringLicenseReferences() {
        val source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java")

        assertTrue(source.contains("JSONObject licenseCatalog = root.optJSONObject(\"licenses\")"))
        assertTrue(source.contains("resolveLicenses(library.optJSONArray(\"licenses\"), licenseCatalog)"))
        assertTrue(source.contains("if (entry instanceof String)"))
        assertTrue(source.contains("licenseCatalog.optJSONObject(normalizedKey)"))
    }

    @Test
    fun parserBuildsLicenseDetailWithResolvedContent() {
        val source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java")

        assertTrue(source.contains("static String buildLicenseDetail"))
        assertTrue(source.contains("detailBuilder.append(license.name)"))
        assertTrue(source.contains("detailBuilder.append(\"\\n\\n\").append(license.content)"))
    }

    @Test
    fun licensePageIncludesDpisProjectLicense() {
        val source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java")
        val strings = read("src/main/res/values/strings.xml")

        assertTrue(source.contains("createProjectLicenseItem()"))
        assertTrue(source.contains("R.raw.gpl_3_0"))
        assertTrue(strings.contains("open_source_license_project_summary"))
        assertTrue(strings.contains("GPL-3.0-or-later"))
    }

    @Test
    fun licenseComposePageUsesLazyListAndPreview() {
        val content = read("src/main/java/com/dpis/module/about/presentation/OpenSourceLicenseContent.kt")

        assertTrue(content.contains("fun OpenSourceLicenseContent("))
        assertTrue(content.contains("LazyColumn("))
        assertTrue(content.contains("items("))
        assertTrue(content.contains("SecondaryPageScaffold("))
        assertTrue(content.contains("SegmentedListItem("))
        assertTrue(content.contains("rememberClickAction"))
        assertTrue(content.contains("OpenSourceLicenseContentPreview"))
    }

    @Test
    fun licenseDetailDialogUsesMaterialLargeWidth() {
        val source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java")
        assertTrue(source.contains("LicenseDetailDialog.show(this"))

        val dialog = read("src/main/java/com/dpis/module/about/presentation/LicenseDetailDialog.kt")
        assertTrue(dialog.contains("verticalScroll(rememberScrollState())"))
        assertTrue(dialog.contains("DialogWindowSizer.applyLargeWidth(dialog, activity)"))
    }

    @Test
    fun notFoundPathShowsMissingThirdPartyLicenseIndicator() {
        val source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.java")
            .replace("\r\n", "\n")
        val notFoundCatch = source.indexOf("} catch (Resources.NotFoundException e) {")
        val throwableCatch = source.indexOf("} catch (Throwable t) {", notFoundCatch)

        assertTrue(notFoundCatch >= 0)
        assertTrue(throwableCatch > notFoundCatch)

        val notFoundBranch = source.substring(notFoundCatch, throwableCatch)
        assertTrue(notFoundBranch.contains("List<LicenseItem> items = new ArrayList<>();"))
        assertTrue(notFoundBranch.contains("items.add(createProjectLicenseItem());"))
        assertTrue(notFoundBranch.contains("items.add(emptyItem(getString(R.string.open_source_license_empty)));"))
        assertTrue(notFoundBranch.contains("return items;"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
