package com.dpis.module

import androidx.compose.ui.graphics.Color
import com.dpis.module.ui.compose.owningSurfaceFadeColor
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwningSurfaceFadePolicyTest {
    @Test
    fun fullyVisibleOpaqueSurfaceKeepsThatColor() {
        val surface = Color(0xFFE8DEF8)
        assertEquals(surface, owningSurfaceFadeColor(surface, 1f))
    }

    @Test
    fun visibilityScalesOwningSurfaceAlphaInsteadOfReplacingIt() {
        val surface = Color(0x80E8DEF8)
        val faded = owningSurfaceFadeColor(surface, 0.5f)
        assertEquals(0.25f, faded.alpha, 0.002f)
        assertEquals(surface.red, faded.red, 0f)
        assertEquals(surface.green, faded.green, 0f)
        assertEquals(surface.blue, faded.blue, 0f)
    }

    @Test
    fun lightOwningSurfaceDoesNotBecomeIndependentBlack() {
        val surface = Color(0xFFFFFBFE)
        val faded = owningSurfaceFadeColor(surface, 1f)
        assertEquals(surface, faded)
        assertTrue(faded.red > 0.9f)
        assertTrue(faded.green > 0.9f)
        assertTrue(faded.blue > 0.9f)
    }

    @Test
    fun surfaceDissolveImplementationDoesNotUseOcclusionShadow() {
        val dissolve = read("src/main/java/com/dpis/module/ui/presentation/editor/HorizontalScrollEdgeFade.kt")
        val tokens = read("src/main/java/com/dpis/module/ui/presentation/design/EdgeFade.kt")
        val occlusion = read("src/main/java/com/dpis/module/ui/presentation/editor/EdgeOcclusionFade.kt")

        assertTrue(dissolve.contains("owningSurfaceColor: Color"))
        assertFalse(dissolve.contains("owningSurfaceColor: Color ="))
        assertFalse(dissolve.contains("Color.Black"))
        assertTrue(dissolve.contains("owningSurfaceFadeColor("))
        assertTrue(tokens.contains("fun owningSurfaceFadeColor("))
        assertTrue(occlusion.contains("val ShadowColor = Color.Black"))
        assertTrue(occlusion.contains("owningSurfaceColor: Color"))
    }

    @Test
    fun filterSheetChipFadeMatchesSheetContainer() {
        val scaffold = read("src/main/java/com/dpis/module/ui/presentation/design/FilterSheet.kt")
        val catalogue = read("src/main/java/com/dpis/module/applist/presentation/AppFilterSheet.kt")

        assertTrue(scaffold.contains("containerColor = MaterialTheme.colorScheme.surfaceContainer"))
        assertTrue(scaffold.contains("fun FilterSheetScrollChipRow("))
        assertTrue(scaffold.contains("owningSurfaceColor = MaterialTheme.colorScheme.surfaceContainer"))
        assertTrue(catalogue.contains("FilterSheetScrollChipRow("))
        assertFalse(catalogue.contains("HorizontalScrollWithEdgeFade("))
        assertFalse(catalogue.contains("Color.Black"))
    }

    @Test
    fun dissolveFadeCallSitesPassAColorSchemeSurface() {
        val sources = kotlinSources()
        val horizontalCalls = sources.filter { (_, text) ->
            text.contains("HorizontalScrollWithEdgeFade(") &&
                !text.contains("internal fun HorizontalScrollWithEdgeFade(")
        }
        assertTrue(horizontalCalls.isNotEmpty())
        horizontalCalls.forEach { (path, text) ->
            assertTrue(
                "$path must pass owningSurfaceColor from the color scheme",
                text.contains("owningSurfaceColor = MaterialTheme.colorScheme."),
            )
            assertFalse(
                "$path must not use an independent black fade",
                Regex("owningSurfaceColor\\s*=\\s*Color\\.Black").containsMatchIn(text),
            )
        }

        val dialogCalls = sources.filter { (_, text) ->
            text.contains("dialogListContentFade(") &&
                !text.contains("internal fun Modifier.dialogListContentFade(")
        }
        assertTrue(dialogCalls.isNotEmpty())
        dialogCalls.forEach { (path, text) ->
            assertTrue(
                "$path must fade dialog overflow into a color-scheme surface",
                text.contains("owningSurfaceColor = MaterialTheme.colorScheme."),
            )
        }
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)

    private fun kotlinSources(): List<Pair<String, String>> {
        val roots = listOf(Path.of("app/src/main/java"), Path.of("src/main/java"))
        val root = roots.first(Files::exists)
        Files.walk(root).use { stream ->
            return stream
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".kt") }
                .map { it.toString() to Files.readString(it, StandardCharsets.UTF_8) }
                .toList()
        }
    }
}
