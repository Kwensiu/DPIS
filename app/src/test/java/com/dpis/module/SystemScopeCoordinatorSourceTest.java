package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class SystemScopeCoordinatorSourceTest {
    @Test
    public void compat100EffectiveStateDoesNotRequireLibxposedService() throws Exception {
        String source = read("src/main/java/com/dpis/module/SystemScopeCoordinator.java");

        assertTrue(source.contains("\"compat100\".equals(BuildConfig.FLAVOR)"));
        assertTrue(source.contains("return desiredEnabled;"));
    }

    private static String read(String relativePath) throws Exception {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
