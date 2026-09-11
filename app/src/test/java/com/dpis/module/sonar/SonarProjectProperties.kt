package com.dpis.module.sonar

/**
 * Loads `sonar-project.properties` the same way root `build.gradle.kts` must:
 * join backslash-continued values. A line-per-`=` reader drops the coverage
 * exclusion list and publishes an empty gate.
 */
internal object SonarProjectProperties {
    fun load(text: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        var pendingKey: String? = null
        val pendingValue = StringBuilder()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) {
                continue
            }
            val pending = pendingKey
            if (pending != null) {
                val continued = line.endsWith("\\")
                val chunk = (if (continued) line.removeSuffix("\\") else line).trim()
                pendingValue.append(chunk)
                if (!continued) {
                    result[pending] = pendingValue.toString()
                    pendingKey = null
                    pendingValue.setLength(0)
                }
                continue
            }
            val separator = line.indexOf('=')
            if (separator < 0) {
                continue
            }
            val key = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim()
            if (value.endsWith("\\")) {
                pendingKey = key
                pendingValue.append(value.removeSuffix("\\").trim())
            } else {
                result[key] = value
            }
        }
        pendingKey?.let { result[it] = pendingValue.toString() }
        return result
    }
}
