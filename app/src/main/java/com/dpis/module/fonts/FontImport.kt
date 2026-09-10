package com.dpis.module.fonts

import java.util.Locale

object FontImport {
    const val LARGE_WARNING_BYTES = 256L * 1024L * 1024L
    const val FREE_SPACE_MARGIN_BYTES = 64L * 1024L * 1024L

    fun isPotentialInput(displayName: String?, mimeType: String?): Boolean {
        val lowerName = displayName?.lowercase(Locale.US).orEmpty()
        if (lowerName.endsWith(".ttf") || lowerName.endsWith(".otf") || lowerName.endsWith(".ttc")) {
            return true
        }
        return mimeType == "font/ttf"
            || mimeType == "font/otf"
            || mimeType == "application/x-font-ttf"
            || mimeType == "application/vnd.ms-opentype"
            || mimeType == "font/ttc"
            || mimeType == "font/collection"
            || mimeType == "application/x-font-ttc"
    }

    fun tempExtension(displayName: String?, mimeType: String?): String {
        val lowerName = displayName?.lowercase(Locale.US).orEmpty()
        if (lowerName.endsWith(".ttc")
            || mimeType == "font/ttc"
            || mimeType == "font/collection"
            || mimeType == "application/x-font-ttc"
        ) {
            return ".ttc"
        }
        if (lowerName.endsWith(".otf")
            || mimeType == "font/otf"
            || mimeType == "application/vnd.ms-opentype"
        ) {
            return ".otf"
        }
        return ".ttf"
    }

    fun hasSpace(sourceSizeBytes: Long, cacheUsable: Long, filesUsable: Long): Boolean {
        if (sourceSizeBytes < 0L) {
            return true
        }
        val required = if (sourceSizeBytes > (Long.MAX_VALUE - FREE_SPACE_MARGIN_BYTES) / 3L) {
            Long.MAX_VALUE
        } else {
            sourceSizeBytes * 3L + FREE_SPACE_MARGIN_BYTES
        }
        return cacheUsable >= required && filesUsable >= required
    }
}
