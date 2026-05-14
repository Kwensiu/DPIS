package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class FontDebugStatsTransportSourceTest {
    @Test
    public void targetProcessTransportDoesNotStartNonExportedModuleComponents() throws IOException {
        String source = read("src/main/java/com/dpis/module/FontDebugStatsTransport.java");

        assertTrue(source.contains("if (!isModuleContext(context))"));
        assertTrue(source.contains("FontDebugStatsFileBridge.write(context, extras);"));
        assertTrue(source.contains("return;"));
        assertTrue(source.contains("BuildConfig.APPLICATION_ID.equals(context.getPackageName())"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
