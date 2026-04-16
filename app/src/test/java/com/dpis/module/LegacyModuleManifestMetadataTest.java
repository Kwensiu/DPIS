package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public final class LegacyModuleManifestMetadataTest {

    @Test
    public void manifestDeclaresLegacyXposedMetadata() throws IOException {
        String manifest = readProjectFile("src/main/AndroidManifest.xml");

        assertTrue(manifest.contains("android:name=\"xposedmodule\""));
        assertTrue(manifest.contains("android:name=\"xposeddescription\""));
        assertTrue(manifest.contains("android:name=\"xposedminversion\""));
        assertTrue(manifest.contains("android:name=\"xposedsharedprefs\""));
        assertFalse(manifest.contains("android:name=\"xposedscope\""));
        assertTrue(manifest.contains("de.robv.android.xposed.category.MODULE_SETTINGS"));
    }

    @Test
    public void scopeResourceNotDeclaredForDynamicScopes() {
        assertFalse(Files.exists(Path.of("src/main/res/values/arrays.xml")));
    }

    @Test
    public void modernScopeListIncludesSystemAsRecommendedScope() throws IOException {
        String scopeList = readProjectFile("src/main/resources/META-INF/xposed/scope.list");
        assertTrue(scopeList.contains("system"));
    }

    private static String readProjectFile(String relativePath) throws IOException {
        Path path = Path.of(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
