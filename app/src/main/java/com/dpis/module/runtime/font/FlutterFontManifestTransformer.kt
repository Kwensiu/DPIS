package com.dpis.module.runtime.font

import org.json.JSONArray
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * Applies the generic Flutter default-family rule without depending on a
 * target application's package name or manifest formatting.
 */
object FlutterFontManifestTransformer {
    private const val DEFAULT_FAMILY = "Roboto"
    private const val PLACEHOLDER_ASSET = "dpis/typeface.ttf"
    private val FAMILY_PATTERN: Pattern = Pattern.compile(
        "\"family\"\\s*+:\\s*+\"((?:\\\\.|[^\"\\\\])*+)\""
    )

    /**
     * Adds a manifest-owned default family only when the app does not already
     * declare that family. A null result means that the input is not a valid
     * Flutter font manifest array and must be left untouched.
     */
    @JvmStatic
    fun addDefaultFamilyIfMissing(manifest: String?): String? {
        if (manifest == null) {
            return null
        }
        try {
            val families = JSONArray(manifest)
            for (index in 0..<families.length()) {
                val family = families.optJSONObject(index)
                if (family != null
                    && DEFAULT_FAMILY == family.optString("family", null)
                ) {
                    return manifest.trim { it <= ' ' }
                }
            }
            val replacementFamily = JSONObject()
            replacementFamily.put("family", DEFAULT_FAMILY)
            val fonts = JSONArray()
            val font = JSONObject()
            font.put("asset", PLACEHOLDER_ASSET)
            fonts.put(font)
            replacementFamily.put("fonts", fonts)
            families.put(replacementFamily)
            return families.toString()
        } catch (ignored: Throwable) {
            // Local JVM unit tests use Android's stub JSON classes. Keep the
            // same validated transformation available there without adding a
            // second JSON dependency to the app.
            return addDefaultFamilyWithValidatedArraySyntax(manifest)
        }
    }

    private fun addDefaultFamilyWithValidatedArraySyntax(manifest: String): String? {
        val trimmed = manifest.trim { it <= ' ' }
        if (!isTopLevelArray(trimmed)) {
            return null
        }
        val matcher = FAMILY_PATTERN.matcher(trimmed)
        while (matcher.find()) {
            if (DEFAULT_FAMILY == matcher.group(1)) {
                return trimmed
            }
        }
        val body = trimmed.substring(0, trimmed.length - 1).trim { it <= ' ' }
        val family = ("{\"family\":\"" + DEFAULT_FAMILY
                + "\",\"fonts\":[{\"asset\":\"" + PLACEHOLDER_ASSET + "\"}]}")
        return if (body == "[") body + family + "]" else body + "," + family + "]"
    }

    private fun isTopLevelArray(value: String): Boolean {
        if (!value.startsWith("[") || !value.endsWith("]")) {
            return false
        }
        var squareDepth = 0
        var curlyDepth = 0
        var quoted = false
        var escaped = false
        for (index in 0..<value.length) {
            val current = value.get(index)
            if (quoted) {
                if (escaped) {
                    escaped = false
                } else if (current == '\\') {
                    escaped = true
                } else if (current == '"') {
                    quoted = false
                }
                continue
            }
            if (current == '"') {
                quoted = true
            } else if (current == '[') {
                squareDepth++
            } else if (current == ']') {
                squareDepth--
                if (squareDepth < 0) {
                    return false
                }
            } else if (current == '{') {
                curlyDepth++
            } else if (current == '}') {
                curlyDepth--
                if (curlyDepth < 0) {
                    return false
                }
            }
        }
        return !quoted && !escaped && squareDepth == 0 && curlyDepth == 0
    }
}
