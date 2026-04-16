package com.dpis.module;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class LegacySmokeModuleFilesTest {
    @Test
    public void standaloneLegacyModuleDeclaresClassicEntryFiles() throws IOException {
        String manifest = readText(Path.of("..", "archive", "legacy-modules", "legacysmoke",
                "src", "main", "AndroidManifest.xml"));
        String xposedInit = readText(Path.of("..", "archive", "legacy-modules", "legacysmoke",
                "src", "main", "assets", "xposed_init"));

        assertTrue(manifest.contains("android:name=\"xposedmodule\""));
        assertTrue(manifest.contains("android:name=\"xposedminversion\""));
        assertTrue(manifest.contains("android:name=\"xposedscope\""));
        assertTrue(xposedInit.contains("com.dpis.legacysmoke.LegacySmokeHook"));
    }

    @Test
    public void comparisonDocCoversAllReferenceModules() throws IOException {
        String doc = readText(Path.of("..", "docs", "legacy-module-compare.md"));

        assertTrue(doc.contains("DPIS"));
        assertTrue(doc.contains("AppSettingsR"));
        assertTrue(doc.contains("SetAppFull"));
        assertTrue(doc.contains("InxLocker"));
        assertTrue(doc.contains("legacy-only"));
        assertTrue(doc.contains("YukiSmoke"));
    }

    @Test
    public void yukiSmokeModuleDeclaresYukiEntry() throws IOException {
        String manifest = readText(Path.of("..", "archive", "legacy-modules", "yukismoke",
                "src", "main", "AndroidManifest.xml"));
        String hookEntry = readText(Path.of("..", "archive", "legacy-modules", "yukismoke",
                "src", "main", "java",
                "com", "dpis", "yukismoke", "HookEntry.kt"));

        assertTrue(manifest.contains("android:name=\"xposedmodule\""));
        assertTrue(manifest.contains("android:name=\"xposedscope\""));
        assertTrue(hookEntry.contains("@InjectYukiHookWithXposed"));
        assertTrue(hookEntry.contains("IYukiHookXposedInit"));
        assertTrue(hookEntry.contains("loadSystem"));
        assertTrue(hookEntry.contains("dpis-system-yuki-v1"));
        assertTrue(hookEntry.contains("SystemServerDisplayEnvironmentInstaller.install"));
        assertTrue(hookEntry.contains("ConfigStoreFactory.createForSystemServerHost"));
    }

    @Test
    public void appModuleStaysModernOnlyWithoutClassicYukiEntry() throws IOException {
        String moduleMain = readText(Path.of("src", "main", "java", "com", "dpis", "module", "ModuleMain.java"));

        assertTrue(moduleMain.contains("extends XposedModule"));
        assertFalse(Files.exists(Path.of("src", "main", "assets", "xposed_init")));
        assertFalse(Files.exists(Path.of("src", "main", "java", "com", "dpis", "module", "HookEntry.kt")));
    }

    private static String readText(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
