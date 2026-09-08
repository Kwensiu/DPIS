plugins {
    alias(libs.plugins.sonarqube)
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
