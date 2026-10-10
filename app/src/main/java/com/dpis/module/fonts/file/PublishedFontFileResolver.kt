package com.dpis.module.fonts

import java.io.File

object PublishedFontFileResolver {
    private val publicFontDirectory = File("/data/local/tmp")

    @JvmStatic
    fun resolve(typefaceId: String?): File? = resolveInDirectory(publicFontDirectory, typefaceId)

    @JvmStatic
    fun resolveInDirectory(directory: File?, typefaceId: String?): File? {
        if (directory == null || typefaceId.isNullOrBlank()) return null
        listOf(".ttf", ".otf", ".ttc").firstNotNullOfOrNull { extension ->
            File(directory, "dpis_$typefaceId$extension").takeIf { it.isFile }
        }?.let { return it }
        val collectionId = stripTtcFaceSuffix(typefaceId)
        return if (collectionId != typefaceId) {
            File(directory, "dpis_$collectionId.ttc").takeIf { it.isFile }
        } else {
            null
        }
    }

    private fun stripTtcFaceSuffix(typefaceId: String): String {
        val marker = typefaceId.lastIndexOf("_ttc_")
        if (marker <= 0 || marker + 5 >= typefaceId.length) return typefaceId
        if (!typefaceId.substring(marker + 5).all(Char::isDigit)) return typefaceId
        return typefaceId.substring(0, marker)
    }
}
