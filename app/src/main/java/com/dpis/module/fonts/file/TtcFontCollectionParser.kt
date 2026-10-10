package com.dpis.module.fonts

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Collections

object TtcFontCollectionParser {
    private const val SIGNATURE_TTC = 0x74746366 // ttcf
    private const val MAX_FACE_COUNT = 128
    private const val HEADER_SIZE = 12L

    @JvmStatic
    fun parse(file: File?): Result {
        if (file == null || !file.isFile) return Result.invalid()
        val fileLength = file.length()
        if (fileLength < HEADER_SIZE) return Result.invalid()
        return try {
            RandomAccessFile(file, "r").use { input ->
                if (input.readInt() != SIGNATURE_TTC) return Result.invalid()
                val version = input.readInt()
                val count = input.readInt().toUInt().toLong()
                if (count == 0L || count > MAX_FACE_COUNT) return Result.invalid()
                val offsetTableLength = HEADER_SIZE + count * 4L
                if (offsetTableLength > fileLength) return Result.invalid()
                val offsets = ArrayList<Long>(count.toInt())
                repeat(count.toInt()) {
                    val offset = input.readInt().toUInt().toLong()
                    if (offset < offsetTableLength || offset >= fileLength) return Result.invalid()
                    offsets += offset
                }
                Result(true, version, Collections.unmodifiableList(offsets))
            }
        } catch (_: IOException) {
            Result.invalid()
        }
    }

    class Result internal constructor(
        @JvmField val valid: Boolean,
        @JvmField val version: Int,
        @JvmField val offsets: List<Long>
    ) {
        companion object {
            @JvmStatic
            fun invalid(): Result = Result(false, 0, emptyList())
        }
    }
}
