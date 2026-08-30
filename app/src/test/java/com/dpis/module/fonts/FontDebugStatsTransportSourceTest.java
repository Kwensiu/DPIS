package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class FontDebugStatsTransportSourceTest {
    @Test
    public void targetProcessTransportDoesNotStartNonExportedModuleComponents() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontDebugStatsTransport.java");

        assertTrue(source.contains("if (!isModuleContext(context))"));
        assertTrue(source.contains("FontDebugStatsFileBridge.write(context, extras);"));
        assertTrue(source.contains("return;"));
        assertTrue(source.contains("BuildConfig.APPLICATION_ID.equals(context.getPackageName())"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
