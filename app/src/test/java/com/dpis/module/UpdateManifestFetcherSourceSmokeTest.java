package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class UpdateManifestFetcherSourceSmokeTest {
    @Test
    public void manifestFetcherOwnsJsonParsingAndSharedHttpContract() throws IOException {
        String source = read("src/main/java/com/dpis/module/UpdateManifestFetcher.java");

        assertTrue(source.contains("final class UpdateManifestFetcher"));
        assertTrue(source.contains("static StartupUpdateManifest fetch("));
        assertTrue(source.contains("connection.setRequestMethod(\"GET\")"));
        assertTrue(source.contains("connection.setRequestProperty(\"Accept\", \"application/json\")"));
        assertTrue(source.contains("new JSONObject(body)"));
        assertTrue(source.contains("private static String readUtf8("));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
