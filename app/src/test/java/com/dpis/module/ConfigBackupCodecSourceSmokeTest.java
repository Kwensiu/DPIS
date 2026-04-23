package com.dpis.module;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class ConfigBackupCodecSourceSmokeTest {
    @Test
    public void codecDefinesSchemaAndSupportedValueTypes() throws IOException {
        String source = read("src/main/java/com/dpis/module/ConfigBackupCodec.java");

        assertTrue(source.contains("SCHEMA_VERSION = 1"));
        assertTrue(source.contains("TYPE_STRING_SET"));
        assertTrue(source.contains("TYPE_INT"));
        assertTrue(source.contains("TYPE_LONG"));
        assertTrue(source.contains("TYPE_FLOAT"));
        assertTrue(source.contains("TYPE_BOOLEAN"));
        assertTrue(source.contains("switch (type)"));
        assertTrue(source.contains("Unsupported backup schema version"));
        assertTrue(source.contains("Unsupported backup value type"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
