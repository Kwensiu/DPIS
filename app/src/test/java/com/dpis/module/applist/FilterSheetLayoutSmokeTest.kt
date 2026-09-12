package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class FilterSheetLayoutSmokeTest {
    @Test
    fun filterSheetContainsOnlyCompactInteractiveSwitches() {
        val layout = read(
            "src/main/java/com/dpis/module/applist/presentation/AppFilterComposeSheet.kt"
        )
        val strings = read("src/main/res/values/strings.xml")

        assertTrue(layout.contains("R.string.filter_show_system_apps"))
        assertTrue(layout.contains("R.string.filter_scoped_only"))
        assertTrue(layout.contains("R.string.filter_width_only"))
        assertTrue(layout.contains("R.string.filter_font_only"))
        assertTrue(layout.contains("R.dimen.filter_sheet_drag_handle_width"))
        assertTrue(layout.contains("R.dimen.filter_sheet_drag_handle_height"))
        assertTrue(layout.contains("R.dimen.filter_sheet_padding_horizontal"))
        assertTrue(layout.contains("contentAlignment = Alignment.Center"))
        assertTrue(layout.contains("MaterialTheme.colorScheme.outlineVariant"))
        assertTrue(!layout.contains("filter_sheet_subtitle"))
        assertTrue(!layout.contains("android:id=\"@+id/filter_reset_button\""))
        assertTrue(!layout.contains("android:id=\"@+id/filter_apply_button\""))
        assertTrue(layout.contains("R.dimen.filter_sheet_first_switch_spacing_top"))
        assertTrue(strings.contains("filter_show_system_apps"))
        assertTrue(strings.contains("filter_scoped_only"))
        assertTrue(strings.contains("filter_width_only"))
        assertTrue(strings.contains("filter_font_only"))
        assertTrue(!strings.contains("filter_sheet_subtitle"))
        assertTrue(!strings.contains("filter_reset_button"))
        assertTrue(!strings.contains("filter_apply_button"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
