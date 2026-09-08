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
        file("sonar-project.properties").readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .forEach { line ->
                val separator = line.indexOf('=')
                property(line.substring(0, separator).trim(), line.substring(separator + 1).trim())
            }
    }
}

tasks.register("Delete", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
