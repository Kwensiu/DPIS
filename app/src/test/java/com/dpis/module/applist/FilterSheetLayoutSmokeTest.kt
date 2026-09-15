package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class FilterSheetLayoutSmokeTest {
    @Test
    fun catalogueAndWearFiltersStayOnProductSurfaces() {
        val catalogue = read(
            "src/main/java/com/dpis/module/applist/presentation/AppFilterSheet.kt"
        )
        val scaffold = read(
            "src/main/java/com/dpis/module/ui/presentation/design/FilterSheet.kt"
        )
        val wear = read(
            "src/main/java/com/dpis/module/ui/presentation/wear/WearWorkspaceContent.kt"
        )
        val strings = read("src/main/res/values/strings.xml")

        assertTrue(catalogue.contains("internal fun AppFilterSheet("))
        assertTrue(scaffold.contains("Box("))
        assertTrue(scaffold.contains(".weight(1f)"))
        assertTrue(!scaffold.contains("Spacer(Modifier.weight(1f))"))
        assertTrue(catalogue.contains("FilterSheetScrollChipRow("))
        assertTrue(wear.contains("fun WearAppFilterPage("))
        assertTrue(wear.contains("R.string.filter_show_system_apps"))
        assertTrue(wear.contains("R.string.filter_scoped_only"))
        assertTrue(wear.contains("R.string.filter_width_only"))
        assertTrue(wear.contains("R.string.filter_font_only"))
        assertTrue(strings.contains("filter_show_system_apps"))
        assertTrue(strings.contains("filter_scoped_only"))
        assertTrue(strings.contains("filter_width_only"))
        assertTrue(strings.contains("filter_font_only"))
        assertTrue(!strings.contains("filter_sheet_subtitle"))
        assertTrue(!strings.contains("filter_reset_button"))
        assertTrue(!strings.contains("filter_apply_button"))
        assertTrue(!catalogue.contains("android:id=\"@+id/filter_reset_button\""))
        assertTrue(!catalogue.contains("android:id=\"@+id/filter_apply_button\""))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
