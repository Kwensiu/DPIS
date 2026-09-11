plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.agp.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.sonarqube)
}

// org.sonarqube 6.3.1 still resolves AGP AppExtension/BaseExtension on Android
// modules. AGP 9 replaced those types, so skip subprojects and analyze from
// sonar-project.properties at the root instead.
subprojects {
    sonar {
        isSkipProject = true
    }
}

sonar {
    properties {
        property("sonar.gradle.skipCompile", "true")
        loadSonarProjectProperties(file("sonar-project.properties")).forEach { (key, value) ->
            property(key, value)
        }
    }
}

/**
 * Join backslash-continued values. A line-per-`=` reader drops
 * `sonar.coverage.exclusions` and must stay in sync with
 * `SonarProjectProperties` in app unit tests.
 */
fun loadSonarProjectProperties(file: java.io.File): Map<String, String> {
    val result = linkedMapOf<String, String>()
    var pendingKey: String? = null
    val pendingValue = StringBuilder()
    file.readLines().forEach { raw ->
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#")) {
            return@forEach
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
            return@forEach
        }
        val separator = line.indexOf('=')
        if (separator < 0) {
            return@forEach
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

tasks.register("Delete", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
