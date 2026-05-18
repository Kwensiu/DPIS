package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class SystemPropertyConfigPreferencesSourceTest {
    @Test
    public void usesTtlBasedSnapshotRefresh() throws Exception {
        String source = read("src/main/java/com/dpis/module/SystemPropertyConfigPreferences.java");

        assertTrue(source.contains("SNAPSHOT_TTL_MILLIS = 2_000L"));
        assertTrue(source.contains("cachedAtMillis"));
        assertTrue(source.contains("(now - cachedAtMillis) < SNAPSHOT_TTL_MILLIS"));
    }

    @Test
    public void readsHookDomainOverrideFromRuntimePropertyMirror() throws Exception {
        String source = read("src/main/java/com/dpis/module/SystemPropertyConfigPreferences.java");

        assertTrue(source.contains("FontHookDomainPropertyBridge.readOverride(packageName)"));
        assertTrue(source.contains("values.put(hookDomainsKey(), String.join(\",\""));
        assertTrue(source.contains("font.\" + packageName + \".hook_domains"));
    }

    private static String read(String relativePath) throws Exception {
        Path path = Path.of(relativePath);
        if (!Files.exists(path)) {
            path = Path.of("app", relativePath);
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
