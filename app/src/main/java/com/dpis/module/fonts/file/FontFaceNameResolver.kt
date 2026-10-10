package com.dpis.module.fonts

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/** Reads family and style names from a TTC without loading an Android Typeface. */
object FontFaceNameResolver {
    private const val TAG_NAME = 0x6E616D65 // name
    private const val SFNT_HEADER_SIZE = 12
    private const val TABLE_RECORD_SIZE = 16
    private const val NAME_HEADER_SIZE = 6
    private const val NAME_RECORD_SIZE = 12
    private const val NAME_FAMILY = 1
    private const val NAME_STYLE = 2
    private const val NAME_TYPOGRAPHIC_FAMILY = 16
    private const val NAME_TYPOGRAPHIC_STYLE = 17
    private const val WINDOWS_ENGLISH_US = 0x0409

    @JvmStatic
    fun resolveTtcFaceLabel(file: File?, ttcIndex: Int, fallback: String): String {
        if (file == null || !file.isFile || ttcIndex < 0) return fallback
        val inspection = FontFileInspector.inspect(file)
        if (inspection.kind != FontFileKind.TTC || ttcIndex >= inspection.ttc.offsets.size) {
            return fallback
        }
        return try {
            RandomAccessFile(file, "r").use { input ->
                val records = readNameRecords(input, inspection.ttc.offsets[ttcIndex])
                val family = preferredName(records, NAME_TYPOGRAPHIC_FAMILY)
                    ?: preferredName(records, NAME_FAMILY)
                val style = preferredName(records, NAME_TYPOGRAPHIC_STYLE)
                    ?: preferredName(records, NAME_STYLE)
                when {
                    family.isNullOrBlank() -> fallback
                    style.isNullOrBlank() -> family
                    else -> "$family $style"
                }
            }
        } catch (_: IOException) {
            fallback
        } catch (_: RuntimeException) {
            fallback
        }
    }

    private fun readNameRecords(input: RandomAccessFile, sfntOffset: Long): List<NameRecord> {
        val fileLength = input.length()
        if (!fits(sfntOffset, SFNT_HEADER_SIZE.toLong(), fileLength)) return emptyList()
        input.seek(sfntOffset + 4)
        val tableCount = input.readUnsignedShort()
        val tableDirectoryEnd =
            sfntOffset + SFNT_HEADER_SIZE + tableCount.toLong() * TABLE_RECORD_SIZE
        if (!fits(sfntOffset, tableDirectoryEnd - sfntOffset, fileLength)) return emptyList()
        var nameOffset = -1L
        var nameLength = 0L
        input.seek(sfntOffset + SFNT_HEADER_SIZE)
        repeat(tableCount) {
            val tag = input.readInt()
            input.skipBytes(4)
            val offset = input.readInt().toUInt().toLong()
            val length = input.readInt().toUInt().toLong()
            if (tag == TAG_NAME && nameOffset < 0) {
                nameOffset = offset
                nameLength = length
            }
        }
        if (nameOffset < 0 || !fits(
                nameOffset,
                nameLength,
                fileLength
            ) || nameLength < NAME_HEADER_SIZE
        ) {
            return emptyList()
        }
        input.seek(nameOffset + 2)
        val recordCount = input.readUnsignedShort()
        val stringOffset = input.readUnsignedShort()
        val recordsEnd = NAME_HEADER_SIZE + recordCount.toLong() * NAME_RECORD_SIZE
        if (recordsEnd > nameLength || stringOffset < recordsEnd) return emptyList()
        val records = ArrayList<NameRecord>()
        input.seek(nameOffset + NAME_HEADER_SIZE)
        repeat(recordCount) {
            val platformId = input.readUnsignedShort()
            val encodingId = input.readUnsignedShort()
            val languageId = input.readUnsignedShort()
            val nameId = input.readUnsignedShort()
            val length = input.readUnsignedShort()
            val offset = input.readUnsignedShort()
            val valueOffset = nameOffset + stringOffset + offset
            if (length == 0 || !fits(
                    valueOffset,
                    length.toLong(),
                    nameOffset + nameLength
                )
            ) return@repeat
            val resumeAt = input.filePointer
            val value = ByteArray(length)
            input.seek(valueOffset)
            input.readFully(value)
            input.seek(resumeAt)
            decodeName(value, platformId, encodingId)?.takeIf { it.isNotBlank() }?.let {
                records += NameRecord(nameId, languageId, it.trim())
            }
        }
        return records
    }

    private fun preferredName(records: List<NameRecord>, requestedNameId: Int): String? {
        var fallback: NameRecord? = null
        for (record in records) {
            if (record.nameId != requestedNameId) continue
            if (record.languageId == WINDOWS_ENGLISH_US || record.languageId == 0) return record.value
            if (fallback == null) fallback = record
        }
        return fallback?.value
    }

    private fun decodeName(value: ByteArray, platformId: Int, encodingId: Int): String? {
        val charset = when {
            platformId == 0 || platformId == 3 -> StandardCharsets.UTF_16BE
            platformId == 1 && encodingId == 0 -> Charset.forName("x-MacRoman")
            else -> return null
        }
        return String(value, charset).replace('\u0000', ' ').trim()
    }

    private fun fits(offset: Long, length: Long, limit: Long): Boolean =
        offset >= 0 && length >= 0 && offset <= limit && length <= limit - offset

    private data class NameRecord(val nameId: Int, val languageId: Int, val value: String)
}
