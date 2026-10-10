package com.dpis.module.fonts

import java.io.File
import java.io.FileInputStream
import java.io.IOException

object FontFileInspector {
    private const val FONT_HEADER_TRUE_TYPE = 0x00010000
    private const val FONT_HEADER_OPEN_TYPE = 0x4F54544F // OTTO
    private const val FONT_HEADER_APPLE_TRUE_TYPE = 0x74727565 // true
    private const val FONT_HEADER_TTC = 0x74746366 // ttcf

    @JvmStatic
    fun inspect(file: File?): Result {
        val signature = readSignature(file)
        if (signature == FONT_HEADER_TTC) {
            val ttc = TtcFontCollectionParser.parse(file)
            return Result(if (ttc.valid) FontFileKind.TTC else FontFileKind.UNSUPPORTED, ttc)
        }
        if (signature == FONT_HEADER_OPEN_TYPE) {
            return Result(FontFileKind.OTF, TtcFontCollectionParser.Result.invalid())
        }
        if (signature == FONT_HEADER_TRUE_TYPE || signature == FONT_HEADER_APPLE_TRUE_TYPE) {
            return Result(FontFileKind.TTF, TtcFontCollectionParser.Result.invalid())
        }
        return Result(FontFileKind.UNSUPPORTED, TtcFontCollectionParser.Result.invalid())
    }

    private fun readSignature(file: File?): Int {
        if (file == null || !file.isFile) return 0
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(4)
                if (input.read(header) != header.size) return 0
                ((header[0].toInt() and 0xFF) shl 24) or
                        ((header[1].toInt() and 0xFF) shl 16) or
                        ((header[2].toInt() and 0xFF) shl 8) or
                        (header[3].toInt() and 0xFF)
            }
        } catch (_: IOException) {
            0
        }
    }

    class Result(
        @JvmField val kind: FontFileKind,
        @JvmField val ttc: TtcFontCollectionParser.Result
    )
}
