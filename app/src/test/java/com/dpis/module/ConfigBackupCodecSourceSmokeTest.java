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

    @Test
    public void codecSupportsTypefaceIdStringEntries() throws IOException {
        String codec = read("src/main/java/com/dpis/module/ConfigBackupCodec.java");
        String store = read("src/main/java/com/dpis/module/DpiConfigStore.java");
        String settings = read("src/main/java/com/dpis/module/SystemServerSettingsActivity.java");

        assertTrue(store.contains("\"font.\" + packageName + \".typeface_id\""));
        assertTrue(settings.contains("Map<String, Object> entries = localStore.snapshotBackup();"));
        assertTrue(settings.contains("String payload = ConfigBackupCodec.encode(entries);"));
        assertTrue(codec.contains("encoded.put(KEY_TYPE, TYPE_STRING);"));
        assertTrue(codec.contains("case TYPE_STRING -> encoded.optString(KEY_VALUE, \"\")"));
        assertTrue(codec.contains("TYPE_STRING"));
        assertTrue(settings.contains("ConfigBackupCodec.decode(payload)"));
        assertTrue(settings.contains("localStore.replaceBackup(entries)"));
        assertTrue(store.contains("BACKUP_EXCLUDED_PREFIXES"));
        assertTrue(store.contains("\"font.library.\""));
        assertTrue(store.contains("\"font.debug.\""));
        assertTrue(store.contains("\"runtime.\""));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
