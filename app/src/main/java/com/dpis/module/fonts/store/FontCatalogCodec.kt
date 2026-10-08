package com.dpis.module.fonts

import java.util.LinkedHashMap
import java.util.Locale

/** Encodes the persisted font catalog without changing its legacy JSON shape. */
internal object FontCatalogCodec {
    private const val ID = "id"
    private const val DISPLAY_NAME = "displayName"
    private const val SOURCE_FILE_NAME = "sourceFileName"
    private const val STORED_FILE_NAME = "storedFileName"
    private const val STORED_PATH = "storedPath"
    private const val SHA256 = "sha256"
    private const val IMPORTED_AT = "importedAtEpochMs"
    private const val TTC_INDEX = "ttcIndex"
    private const val COLLECTION_ID = "collectionId"
    private const val COLLECTION_DISPLAY_NAME = "collectionDisplayName"
    private const val PUBLICATION_STATUS = "publicationStatus"

    fun encode(entries: List<FontLibraryEntry>): String = entries.joinToString(prefix = "[", postfix = "]", separator = ",") { entry ->
        "{" + listOf(
            pair(ID, entry.id), pair(DISPLAY_NAME, entry.displayName), pair(SOURCE_FILE_NAME, entry.sourceFileName),
            pair(STORED_FILE_NAME, entry.storedFileName), pair(STORED_PATH, entry.storedPath), pair(SHA256, entry.sha256),
            "${quote(IMPORTED_AT)}:${entry.importedAtEpochMs}", "${quote(TTC_INDEX)}:${entry.ttcIndex}",
            pair(COLLECTION_ID, entry.collectionId), pair(COLLECTION_DISPLAY_NAME, entry.collectionDisplayName),
            pair(PUBLICATION_STATUS, entry.publicationStatus.name),
        ).joinToString(",") + "}"
    }

    fun decode(rawJson: String?): List<Map<String, String>>? {
        if (rawJson == null) return null
        val cursor = Cursor(rawJson)
        if (!cursor.consume('[')) return null
        if (cursor.consume(']')) return emptyList()
        val objects = ArrayList<Map<String, String>>()
        do {
            val parsed = parseObject(cursor) ?: return null
            objects += parsed
        } while (cursor.consume(','))
        return if (cursor.consume(']') && cursor.exhausted()) objects else null
    }

    private fun parseObject(cursor: Cursor): Map<String, String>? {
        if (!cursor.consume('{')) return null
        val result = LinkedHashMap<String, String>()
        if (cursor.consume('}')) return result
        do {
            val key = cursor.readString() ?: return null
            if (!cursor.consume(':')) return null
            result[key] = cursor.readValueAsString() ?: return null
        } while (cursor.consume(','))
        return result.takeIf { cursor.consume('}') }
    }

    private fun pair(key: String, value: String?): String = "${quote(key)}:${quote(value ?: "")}"
    private fun quote(value: String): String = buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000c' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (character < '\u0020') append("\\u").append(character.code.toString(16).padStart(4, '0'))
                    else append(character.toString())
                }
            }
        }
        append('"')
    }

    private class Cursor(private val text: String) {
        private var index = 0
        fun consume(expected: Char): Boolean { skipWhitespace(); if (index >= text.length || text[index] != expected) return false; index++; return true }
        fun exhausted(): Boolean { skipWhitespace(); return index == text.length }
        fun readString(): String? {
            skipWhitespace(); if (index >= text.length || text[index] != '"') return null
            index++; val result = StringBuilder()
            while (index < text.length) {
                when (val character = text[index++]) {
                    '"' -> return result.toString()
                    '\\' -> when {
                        index >= text.length -> return null
                        else -> when (val escaped = text[index++]) {
                            '"', '\\', '/' -> result.append(escaped)
                            'b' -> result.append('\b'); 'f' -> result.append('\u000c'); 'n' -> result.append('\n')
                            'r' -> result.append('\r'); 't' -> result.append('\t')
                            'u' -> { if (index + 4 > text.length) return null; result.append(text.substring(index, index + 4).toIntOrNull(16)?.toChar() ?: return null); index += 4 }
                            else -> return null
                        }
                    }
                    else -> result.append(character)
                }
            }
            return null
        }
        fun readValueAsString(): String? {
            skipWhitespace(); return if (index < text.length && text[index] == '"') readString() else {
                val start = index; while (index < text.length && text[index] !in ",}") index++
                text.substring(start, index).trim().takeIf { it.isNotEmpty() }
            }
        }
        private fun skipWhitespace() { while (index < text.length && text[index].isWhitespace()) index++ }
    }
}
