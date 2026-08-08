package com.dpis.module;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ExperimentalSettingsActivitySourceSmokeTest {
    @Test
    public void laboratoryShowsCenteredEmptyState() throws IOException {
        String source = read(
                "src/main/java/com/dpis/module/settings/ExperimentalSettingsActivity.java");
        String content = read("src/main/java/com/dpis/module/ui/compose/ExperimentalSettingsContent.kt");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("SupportActivityContent.installExperimentalSettings(this);"));
        assertTrue(content.contains("SecondaryPageScaffold("));
        assertTrue(content.contains("R.string.settings_experimental_title"));
        assertTrue(content.contains("contentAlignment = Alignment.Center"));
        assertTrue(content.contains("R.string.settings_experimental_empty"));
        assertTrue(strings.contains("settings_experimental_empty"));
        assertFalse(source.contains("ExperimentalSettingsStore"));
        assertFalse(content.contains("ExperimentalSettingsStore"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
