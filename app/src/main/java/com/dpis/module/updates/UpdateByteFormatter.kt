package com.dpis.module.updates

import java.util.Locale

object UpdateByteFormatter {
    @JvmStatic
    fun format(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        var value = bytes.toDouble()
        val units = arrayOf("KB", "MB", "GB", "TB")
        var unitIndex = -1
        do {
            value /= 1024.0
            unitIndex++
        } while (value >= 1024.0 && unitIndex < units.size - 1)
        return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex])
    }
}
