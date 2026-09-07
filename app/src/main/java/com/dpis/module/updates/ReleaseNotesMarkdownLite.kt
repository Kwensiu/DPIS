package com.dpis.module.updates

import java.util.Locale
import java.util.regex.Pattern

object ReleaseNotesMarkdownLite {
    private val VERSION_HEADING_PATTERN: Pattern = Pattern.compile(
        "^##\\s*+\\[[^]]++]\\(https?://[^)]++\\)\\s*+\\(\\d{4}-\\d{2}-\\d{2}\\)\\s*$"
    )
    private val GENERATED_SECTION_PATTERN: Pattern = Pattern.compile(
        "^##\\s+(?:new contributors|contributors)\\s*$",
        Pattern.CASE_INSENSITIVE
    )
    private val FULL_CHANGELOG_PATTERN: Pattern = Pattern.compile(
        "^(?:#+\\s*)?\\*?\\*?full changelog\\*?\\*?:?.*$",
        Pattern.CASE_INSENSITIVE
    )

    @JvmStatic
    fun filterBodyForLocale(markdown: String?, locale: Locale?): String {
        val normalized = if (markdown != null) markdown.replace("\r\n", "\n") else ""
        val lines: Array<String?> = normalized.split("\n".toRegex()).toTypedArray()
        var start = 0
        while (start < lines.size && lines[start]!!.trim { it <= ' ' }.isEmpty()) {
            start++
        }
        if (start < lines.size && VERSION_HEADING_PATTERN.matcher(lines[start]!!.trim { it <= ' ' })
                .matches()
        ) {
            start++
            while (start < lines.size && lines[start]!!.trim { it <= ' ' }.isEmpty()) {
                start++
            }
        }

        val body = StringBuilder()
        for (i in start..<lines.size) {
            body.append(lines[i])
            if (i < lines.size - 1) {
                body.append('\n')
            }
        }
        val content = removeGeneratedReleaseMetadata(body.toString()).trim { it <= ' ' }
        val sectionSplit = splitByDividerLine(content)
        if (sectionSplit == null) {
            return content.trim { it <= ' ' }
        }

        val chinesePart = sectionSplit[0]!!.trim { it <= ' ' }
        val englishPart = sectionSplit[1]!!.trim { it <= ' ' }
        val isChinese = locale != null && locale.language.startsWith("zh")
        val preferred = if (isChinese) chinesePart else englishPart
        if (!preferred.isEmpty()) {
            return preferred
        }
        return (englishPart + "\n" + chinesePart).trim { it <= ' ' }
    }

    /** Removes GitHub-generated publication metadata while retaining release-note prose.  */
    private fun removeGeneratedReleaseMetadata(content: String): String {
        val lines: Array<String?> = content.split("\\n".toRegex()).toTypedArray()
        val filtered = StringBuilder()
        var skippingGeneratedSection = false
        for (raw in lines) {
            val line = if (raw != null) raw else ""
            val trimmed = line.trim { it <= ' ' }
            if (GENERATED_SECTION_PATTERN.matcher(trimmed).matches()) {
                skippingGeneratedSection = true
                continue
            }
            if (skippingGeneratedSection && trimmed.startsWith("#")) {
                skippingGeneratedSection = false
            }
            if (skippingGeneratedSection || FULL_CHANGELOG_PATTERN.matcher(trimmed).matches()) {
                continue
            }
            appendLine(filtered, line)
        }
        return filtered.toString()
    }

    private fun splitByDividerLine(content: String): Array<String?>? {
        val lines: Array<String?> = content.split("\n".toRegex()).toTypedArray()
        val top = StringBuilder()
        val bottom = StringBuilder()
        var foundDivider = false
        for (raw in lines) {
            val line = if (raw == null) "" else raw.trim { it <= ' ' }
            if (!foundDivider && line.matches("-{3,}".toRegex())) {
                foundDivider = true
                continue
            }
            if (!foundDivider) {
                appendLine(top, raw)
            } else {
                appendLine(bottom, raw)
            }
        }
        if (!foundDivider) {
            return null
        }
        return arrayOf<String?>(top.toString(), bottom.toString())
    }

    private fun appendLine(builder: StringBuilder, line: String?) {
        if (builder.length > 0) {
            builder.append('\n')
        }
        builder.append(line)
    }
}
