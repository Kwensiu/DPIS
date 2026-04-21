package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class LegacySmokeModuleFilesTest {
    @Test
    public void standaloneLegacyModuleDeclaresClassicEntryFiles() throws IOException {
        String manifest = readResourceText("legacy-modules/legacysmoke/AndroidManifest.xml");
        String xposedInit = readResourceText("legacy-modules/legacysmoke/xposed_init");

        assertTrue(manifest.contains("android:name=\"xposedmodule\""));
        assertTrue(manifest.contains("android:name=\"xposedminversion\""));
        assertTrue(manifest.contains("android:name=\"xposedscope\""));
        assertTrue(xposedInit.contains("com.dpis.legacysmoke.LegacySmokeHook"));
    }

    @Test
    public void comparisonDocCoversLegacyReferenceModules() throws IOException {
        String doc = readText(Path.of("..", "docs", "archive", "reports", "legacy-module-compare.md"));

        assertTrue(doc.contains("DPIS"));
        assertTrue(doc.contains("AppSettingsR"));
        assertTrue(doc.contains("SetAppFull"));
        assertTrue(doc.contains("InxLocker"));
        assertTrue(doc.contains("legacy-only"));
    }

    @Test
    public void appModuleStaysModernOnlyWithoutLegacyHookEntryFiles() throws IOException {
        String moduleMain = readText(Path.of("src", "main", "java", "com", "dpis", "module", "ModuleMain.java"));

        assertTrue(moduleMain.contains("extends XposedModule"));
        assertFalse(Files.exists(Path.of("src", "main", "assets", "xposed_init")));
        assertFalse(Files.exists(Path.of("src", "main", "java", "com", "dpis", "module", "HookEntry.kt")));
    }

    private static String readText(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static String readResourceText(String resourcePath) throws IOException {
        try (InputStream inputStream = LegacySmokeModuleFilesTest.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing test resource: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
