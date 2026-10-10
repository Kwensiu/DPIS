package com.dpis.module.fonts

import android.graphics.Typeface
import java.io.File
import java.io.FileDescriptor
import java.util.Locale

object FontTypefaceLoader {
    @JvmStatic
    fun load(file: File?, ttcIndex: Int): Typeface? {
        if (file == null || !file.canRead()) return null
        return try {
            if (isTtc(file) || ttcIndex > 0) {
                Typeface.Builder(file)
                    .setTtcIndex(ttcIndex.coerceAtLeast(0))
                    .build()
            } else {
                Typeface.createFromFile(file)
            }
        } catch (_: Throwable) {
            null
        }
    }

    /** A seekable descriptor is required because Typeface may mmap a TTC collection. */
    @JvmStatic
    fun load(descriptor: FileDescriptor?, ttcIndex: Int): Typeface? {
        if (descriptor == null || !descriptor.valid()) return null
        return try {
            Typeface.Builder(descriptor)
                .setTtcIndex(ttcIndex.coerceAtLeast(0))
                .build()
        } catch (_: Throwable) {
            null
        }
    }

    private fun isTtc(file: File): Boolean =
        file.name.lowercase(Locale.US).endsWith(".ttc")
}
