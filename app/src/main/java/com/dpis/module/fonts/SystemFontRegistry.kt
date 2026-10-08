package com.dpis.module.fonts

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontStyle
import android.graphics.fonts.SystemFonts
import android.os.Build
import org.w3c.dom.Element
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

object SystemFontRegistry {
    private const val FILE_ID_PREFIX = "system-font:"
    private const val FAMILY_ID_PREFIX = "system-family:"
    private const val ID_SEPARATOR = ":"
    private val fontConfigFiles = arrayOf(
        File("/system/etc/fonts.xml"),
        File("/system_ext/etc/hyper_fonts.xml"),
        File("/system_ext/etc/miui_fonts.xml"),
        File("/product/etc/mi_fonts_customization.xml")
    )
    private val recommendedFamilies = arrayOf(
        "sans-serif" to "Sans Serif",
        "sans-serif-condensed" to "Sans Serif Condensed",
        "serif" to "Serif",
        "monospace" to "Monospace",
        "source-sans-pro" to "Source Sans Pro",
        "roboto-flex" to "Roboto Flex",
        "cursive" to "Cursive"
    )

    @JvmStatic
    fun isSystemFontId(typefaceId: String?): Boolean =
        typefaceId?.let { it.startsWith(FILE_ID_PREFIX) || it.startsWith(FAMILY_ID_PREFIX) } == true

    @JvmStatic
    fun listRecommendedFonts(): List<SystemFontEntry> =
        listRecommendedFonts(readDeclaredFamilyNames())

    @JvmStatic
    @SuppressLint("NewApi")
    fun listAvailableFonts(): List<SystemFontEntry> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        return try {
            SystemFonts.getAvailableFonts().mapNotNull { font ->
                val file = font.file ?: return@mapNotNull null
                if (!file.canRead()) return@mapNotNull null
                val id = buildFontId(file.absolutePath, font.ttcIndex) ?: return@mapNotNull null
                id to SystemFontEntry(id, formatDisplayName(font))
            }.distinctBy { it.first }.map { it.second }.sortedBy { it.displayName().lowercase() }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    @JvmStatic
    fun loadTypeface(typefaceId: String?): Typeface? {
        decodeFamilyName(typefaceId)?.let {
            return try {
                Typeface.create(it, Typeface.NORMAL)
            } catch (_: Throwable) {
                null
            }
        }
        val font = findFontById(typefaceId) ?: return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val file = font.file ?: return null
        if (!file.canRead()) return null
        return try {
            Typeface.Builder(file).setTtcIndex(font.ttcIndex).build()
        } catch (_: Throwable) {
            null
        }
    }

    @JvmStatic
    fun buildFontIdForTest(path: String?, ttcIndex: Int): String? = buildFontId(path, ttcIndex)
    @JvmStatic
    fun buildFamilyIdForTest(familyName: String?): String? = buildFamilyId(familyName)
    @JvmStatic
    fun listRecommendedFontsForTest(declaredFamilyNames: Set<String>?): List<SystemFontEntry> =
        listRecommendedFonts(declaredFamilyNames)

    private fun listRecommendedFonts(names: Set<String>?): List<SystemFontEntry> = names.orEmpty()
        .let { declared ->
            recommendedFamilies.mapNotNull { (name, display) ->
                if (name in declared) buildFamilyId(name)?.let {
                    SystemFontEntry(
                        it,
                        display
                    )
                } else null
            }
        }

    private fun buildFamilyId(name: String?): String? =
        name?.takeIf { it.isNotBlank() }?.trim()?.let { FAMILY_ID_PREFIX + it }

    private fun decodeFamilyName(id: String?): String? =
        id?.takeIf { it.startsWith(FAMILY_ID_PREFIX) }?.substring(FAMILY_ID_PREFIX.length)?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun buildFontId(path: String?, index: Int): String? =
        if (path.isNullOrBlank() || index < 0) null else FILE_ID_PREFIX + hashPathAndIndex(
            path,
            index
        )

    @SuppressLint("NewApi")
    private fun findFontById(id: String?): Font? {
        if (id == null || !id.startsWith(FILE_ID_PREFIX) || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            SystemFonts.getAvailableFonts().firstOrNull { font ->
                font.file?.let {
                    it.canRead() && id == buildFontId(
                        it.absolutePath,
                        font.ttcIndex
                    )
                } == true
            }
        } catch (_: Throwable) {
            null
        }
    }

    @SuppressLint("NewApi")
    private fun formatDisplayName(font: Font): String {
        val fileName = font.file?.name ?: "System font"
        val style = font.style
        val slant = if (style.slant == FontStyle.FONT_SLANT_ITALIC) "italic" else "normal"
        val suffix =
            "w${style.weight}, $slant" + if (font.ttcIndex > 0) ", ttc ${font.ttcIndex}" else ""
        return String.format(Locale.US, "%s (%s)", fileName, suffix)
    }

    private fun readDeclaredFamilyNames(): Set<String> =
        fontConfigFiles.flatMapTo(LinkedHashSet()) { readDeclaredFamilyNames(it) }

    private fun readDeclaredFamilyNames(file: File): Set<String> {
        if (!file.canRead()) return emptySet()
        return try {
            val families =
                DocumentBuilderFactory.newInstance().apply { isExpandEntityReferences = false }
                    .newDocumentBuilder().parse(file).getElementsByTagName("family")
            buildSet {
                for (index in 0 until families.length) (families.item(index) as? Element)?.getAttribute(
                    "name"
                )?.takeIf { it.isNotBlank() }?.trim()?.let(::add)
            }
        } catch (_: Throwable) {
            emptySet()
        }
    }

    private fun hashPathAndIndex(path: String, index: Int): String = try {
        MessageDigest.getInstance("SHA-256")
            .digest("$path$ID_SEPARATOR$index".toByteArray(StandardCharsets.UTF_8)).take(8)
            .joinToString("") { "%02x".format(Locale.US, it) }
    } catch (_: Exception) {
        "${path}:$index".hashCode().toUInt().toString(16)
    }
}
