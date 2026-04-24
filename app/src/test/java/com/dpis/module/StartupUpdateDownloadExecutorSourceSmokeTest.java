package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class StartupUpdateDownloadExecutorSourceSmokeTest {
    @Test
    public void downloadExecutorDefinesCancellationAndProgressContracts() throws IOException {
        String source = read("src/main/java/com/dpis/module/StartupUpdateDownloadExecutor.java");

        assertTrue(source.contains("final class StartupUpdateDownloadExecutor"));
        assertTrue(source.contains("interface Cancellation"));
        assertTrue(source.contains("interface Listener"));
        assertTrue(source.contains("void onConnectionOpened("));
        assertTrue(source.contains("void onProgress("));
        assertTrue(source.contains("void download(Uri downloadUri"));
        assertTrue(source.contains("DownloadCanceledException"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
