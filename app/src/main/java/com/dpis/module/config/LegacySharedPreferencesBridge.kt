package com.dpis.module.config

import com.dpis.module.DpisLog
import android.content.SharedPreferences
import org.w3c.dom.Element
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/**
 * Owns the legacy SharedPreferences XML compatibility boundary.
 *
 * The current preference store remains authoritative. This bridge only imports
 * historical XML snapshots and mirrors committed state for legacy readers.
 */
internal class LegacySharedPreferencesBridge(
    private val preferences: SharedPreferences,
    private val mirrorFile: File?
) {
    fun importXml(
        sourceFile: File?,
        replaceEntries: (MutableMap<String?, Any?>) -> Boolean
    ): Boolean {
        if (sourceFile == null || !sourceFile.exists()) {
            return false
        }
        return try {
            val entries = readSharedPreferencesXml(sourceFile)
            preserveCurrentTemplateEntries(entries)
            !entries.isEmpty() && replaceEntries(entries)
        } catch (throwable: Throwable) {
            DpisLog.e("legacy shared prefs import failed", throwable)
            false
        }
    }

    fun mirror(): Boolean {
        val target = mirrorFile ?: return true
        return try {
            // Legacy XSharedPreferences reads the conventional XML path even on
            // Android builds whose active SharedPreferences directory differs.
            val parent = target.parentFile
            if (parent == null || (!parent.exists() && !parent.mkdirs() && !parent.exists())) {
                return false
            }
            val tempFile = File(parent, target.name + ".tmp")
            writeSharedPreferencesXml(preferences.getAll(), tempFile)
            try {
                Files.move(
                    tempFile.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (ignored: AtomicMoveNotSupportedException) {
                Files.move(
                    tempFile.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            true
        } catch (throwable: IOException) {
            DpisLog.e("legacy shared prefs mirror failed", throwable)
            false
        } catch (throwable: Throwable) {
            DpisLog.e("legacy shared prefs mirror failed", throwable)
            false
        }
    }

    private fun preserveCurrentTemplateEntries(entries: MutableMap<String?, Any?>) {
        // Templates share the historical preference group but are not part of
        // runtime configuration migration; stale XML must not delete them.
        for (entry in preferences.getAll().entries) {
            val key = entry.key
            val value = ConfigPreferenceValueCodec.normalize(entry.value)
            if (key != null && key.startsWith("template.") && value != null) {
                entries[key] = value
            }
        }
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun writeSharedPreferencesXmlForTest(entries: MutableMap<String, *>?, targetFile: File) {
            writeSharedPreferencesXml(entries, targetFile)
        }

        @JvmStatic
        @Throws(Exception::class)
        fun readSharedPreferencesXmlForTest(sourceFile: File?): MutableMap<String?, Any?> {
            return readSharedPreferencesXml(sourceFile)
        }

        @Throws(IOException::class)
        private fun writeSharedPreferencesXml(entries: MutableMap<String, *>?, targetFile: File) {
            Files.write(
                targetFile.toPath(),
                sharedPreferencesXml(entries).toByteArray(StandardCharsets.UTF_8)
            )
        }

        private fun sharedPreferencesXml(entries: MutableMap<String, *>?): String {
            val builder = StringBuilder()
            builder.append("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n")
            builder.append("<map>\n")
            if (entries != null) {
                for (entry in entries.entries) {
                    appendPreferenceXmlEntry(builder, entry.key, ConfigPreferenceValueCodec.normalize(entry.value))
                }
            }
            builder.append("</map>\n")
            return builder.toString()
        }

        @Throws(Exception::class)
        private fun readSharedPreferencesXml(sourceFile: File?): MutableMap<String?, Any?> {
            val entries = LinkedHashMap<String?, Any?>()
            val factory = DocumentBuilderFactory.newInstance()
            setXmlFeatureIfSupported(factory, "http://apache.org/xml/features/disallow-doctype-decl", true)
            setXmlFeatureIfSupported(factory, "http://xml.org/sax/features/external-general-entities", false)
            setXmlFeatureIfSupported(factory, "http://xml.org/sax/features/external-parameter-entities", false)
            val root = factory.newDocumentBuilder().parse(sourceFile).documentElement
            if (root == null || root.tagName != "map") {
                return entries
            }
            val children = root.childNodes
            for (index in 0 until children.length) {
                val node = children.item(index)
                if (node !is Element) continue
                val key = node.getAttribute("name")
                if (key.isNullOrEmpty()) continue
                readSharedPreferencesXmlValue(node)?.let { entries[key] = it }
            }
            return entries
        }

        private fun setXmlFeatureIfSupported(
            factory: DocumentBuilderFactory,
            feature: String,
            value: Boolean
        ) {
            try {
                factory.setFeature(feature, value)
            } catch (ignored: ParserConfigurationException) {
                // Android XML implementations vary; the file is app-owned.
            }
        }

        private fun readSharedPreferencesXmlValue(element: Element): Any? {
            return try {
                when (element.tagName) {
                    "string" -> element.textContent
                    "int" -> element.getAttribute("value").toInt()
                    "long" -> element.getAttribute("value").toLong()
                    "float" -> element.getAttribute("value").toFloat()
                    "boolean" -> element.getAttribute("value").toBoolean()
                    "set" -> readSharedPreferencesXmlStringSet(element)
                    else -> null
                }
            } catch (ignored: RuntimeException) {
                null
            }
        }

        private fun readSharedPreferencesXmlStringSet(element: Element): LinkedHashSet<String?> {
            val values = LinkedHashSet<String?>()
            val children = element.childNodes
            for (index in 0 until children.length) {
                val node = children.item(index)
                if (node is Element && node.tagName == "string") {
                    values.add(node.textContent)
                }
            }
            return values
        }

        private fun appendPreferenceXmlEntry(builder: StringBuilder, key: String?, value: Any?) {
            if (key.isNullOrEmpty() || value == null) return
            val escapedKey = escapeXml(key)
            when (value) {
                is String -> builder.append("    <string name=\"").append(escapedKey).append("\">")
                    .append(escapeXml(value)).append("</string>\n")
                is Int -> appendPrimitiveXmlEntry(builder, "int", escapedKey, value.toString())
                is Long -> appendPrimitiveXmlEntry(builder, "long", escapedKey, value.toString())
                is Float -> appendPrimitiveXmlEntry(builder, "float", escapedKey, value.toString())
                is Boolean -> appendPrimitiveXmlEntry(builder, "boolean", escapedKey, value.toString())
                is MutableSet<*> -> {
                    if (value.isEmpty()) {
                        builder.append("    <set name=\"").append(escapedKey).append("\" />\n")
                    } else {
                        builder.append("    <set name=\"").append(escapedKey).append("\">\n")
                        for (item in value) if (item is String) {
                            builder.append("        <string>").append(escapeXml(item)).append("</string>\n")
                        }
                        builder.append("    </set>\n")
                    }
                }
            }
        }

        private fun appendPrimitiveXmlEntry(
            builder: StringBuilder,
            tag: String,
            escapedKey: String,
            value: String
        ) {
            builder.append("    <").append(tag).append(" name=\"").append(escapedKey)
                .append("\" value=\"").append(escapeXml(value)).append("\" />\n")
        }

        private fun escapeXml(value: String): String {
            val builder = StringBuilder(value.length)
            for (character in value) {
                when (character) {
                    '&' -> builder.append("&amp;")
                    '<' -> builder.append("&lt;")
                    '>' -> builder.append("&gt;")
                    '"' -> builder.append("&quot;")
                    '\'' -> builder.append("&apos;")
                    else -> builder.append(character)
                }
            }
            return builder.toString()
        }
    }
}
