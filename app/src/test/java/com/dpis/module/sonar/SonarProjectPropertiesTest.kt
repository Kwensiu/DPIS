package com.dpis.module.sonar

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonarProjectPropertiesTest {
    @Test
    fun joinsContinuedCoverageExclusionsFromCheckedInFile() {
        val properties = SonarProjectProperties.load(
            SourceSmokeTestPaths.readRepositoryRoot("sonar-project.properties"),
        )
        val exclusions = properties.getValue("sonar.coverage.exclusions")
        assertTrue(exclusions.contains("**/presentation/**"))
        assertTrue(exclusions.contains("**/device/**"))
        assertTrue(exclusions.contains("app/src/main/java/com/dpis/module/ui/**"))
        assertTrue(exclusions.contains("*Activity.kt"))
        assertFalse(exclusions.contains("FontDebugLogcatBridge.kt"))
        assertFalse(exclusions.contains("LsposedLogReader.kt"))
        assertFalse(exclusions.contains("ProcessActionPolicy"))
        assertTrue(exclusions.length > 200)
        assertTrue(properties.getValue("sonar.sources").contains("app/src/main/java"))
    }

    @Test
    fun lineEqualsParserWouldDropContinuedExclusions() {
        val text = SourceSmokeTestPaths.readRepositoryRoot("sonar-project.properties")
        val broken = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .associate { line ->
                val separator = line.indexOf('=')
                line.substring(0, separator).trim() to line.substring(separator + 1).trim()
            }
        assertFalse(broken.getValue("sonar.coverage.exclusions").contains("**/presentation/**"))
    }
}
