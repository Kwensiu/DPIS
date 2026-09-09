package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSourceLicenseActivityParsingTest {
    @Test
    fun parserReadsCatalogAndStringLicenseReferences() {
        val source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.kt")

        assertTrue(source.contains("val licenseCatalog = root.optJSONObject(\"licenses\")"))
        assertTrue(source.contains("resolveLicenses("))
        assertTrue(source.contains("library.optJSONArray(\"licenses\")"))
        assertTrue(source.contains("if (entry is String)"))
        assertTrue(source.contains("licenseCatalog.optJSONObject(normalizedKey)"))
    }

    @Test
    fun parserBuildsLicenseDetailWithResolvedContent() {
        val source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.kt")

        assertTrue(source.contains("fun buildLicenseDetail"))
        assertTrue(source.contains("detailBuilder.append(license.name)"))
        assertTrue(source.contains("detailBuilder.append(\"\\n\\n\").append(license.content)"))
    }

    @Test
    fun licensePageIncludesDpisProjectLicense() {
        val source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.kt")
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
        assertTrue(content.contains("LicenseDetailDialog("))
        assertTrue(content.contains("onOpenUrl"))
        assertTrue(!content.contains("onItemSelected"))
    }

    @Test
    fun licenseDetailDialogUsesComposeModalState() {
        val activity = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.kt")
        val content = read("src/main/java/com/dpis/module/about/presentation/OpenSourceLicenseContent.kt")
        val dialog = read("src/main/java/com/dpis/module/about/presentation/LicenseDetailDialog.kt")
        val wear = read("src/main/java/com/dpis/module/ui/presentation/wear/WearSecondaryPages.kt")

        assertTrue(!activity.contains("LicenseDetailDialog.show("))
        assertTrue(activity.contains("installOpenSourceLicenses("))
        assertTrue(activity.contains("::openUrl"))
        assertTrue(content.contains("var selectedItem by remember"))
        assertTrue(wear.contains("var selectedItem by remember"))
        assertTrue(wear.contains("LicenseDetailDialog("))
        assertTrue(dialog.contains("ModalDialog(onDismissRequest = onDismiss)"))
        assertTrue(dialog.contains("verticalScroll(rememberScrollState())"))
        assertTrue(dialog.contains("DialogColumn("))
        assertTrue(!dialog.contains("MaterialAlertDialogBuilder"))
        assertTrue(!dialog.contains("DialogWindowSizer"))
    }

    @Test
    fun notFoundPathShowsMissingThirdPartyLicenseIndicator() {
        val source = read("src/main/java/com/dpis/module/about/OpenSourceLicenseActivity.kt")
            .replace("\r\n", "\n")
        val notFoundCatch = source.indexOf("} catch (_: Resources.NotFoundException) {")
        val throwableCatch = source.indexOf("} catch (_: Throwable) {", notFoundCatch)

        assertTrue(notFoundCatch >= 0)
        assertTrue(throwableCatch > notFoundCatch)

        val notFoundBranch = source.substring(notFoundCatch, throwableCatch)
        assertTrue(notFoundBranch.contains("createProjectLicenseItem()"))
        assertTrue(notFoundBranch.contains("R.string.open_source_license_empty"))
        assertTrue(notFoundBranch.contains("return listOf("))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
