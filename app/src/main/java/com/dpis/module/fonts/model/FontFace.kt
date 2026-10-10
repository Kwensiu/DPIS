package com.dpis.module.fonts

/** A selectable face within one physical font collection. */
class FontFace @JvmOverloads constructor(
    @JvmField val collectionId: String,
    ttcIndex: Int,
    @JvmField val collectionFace: Boolean = false
) {
    @JvmField
    val ttcIndex: Int = ttcIndex.coerceAtLeast(0)

    fun toLegacyId(): String = if (collectionFace) "${collectionId}_ttc_$ttcIndex" else collectionId

    companion object {
        @JvmStatic
        fun fromLegacyId(typefaceId: String?): FontFace? {
            if (typefaceId.isNullOrBlank()) return null
            val marker = typefaceId.lastIndexOf("_ttc_")
            if (marker <= 0 || marker + 5 >= typefaceId.length) {
                return FontFace(typefaceId, 0)
            }
            return typefaceId.substring(marker + 5).toIntOrNull()
                ?.takeIf { it >= 0 }
                ?.let { FontFace(typefaceId.substring(0, marker), it, true) }
                ?: FontFace(typefaceId, 0)
        }
    }
}
