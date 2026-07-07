package com.dpis.module;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class ConfigBackupCodecSourceSmokeTest {
    @Test
    public void codecDefinesSchemaAndSupportedValueTypes() throws IOException {
        String source = read("src/main/java/com/dpis/module/backup/ConfigBackupCodec.java");

        assertTrue(source.contains("SCHEMA_VERSION = 3"));
        assertTrue(source.contains("KEY_PACKAGE_CONFIGS"));
        assertTrue(source.contains("KEY_RESOLUTION_CONFIGS"));
        assertTrue(source.contains("KEY_GLOBAL"));
        assertTrue(source.contains("KEY_DEFAULT_PREFILL"));
        assertTrue(source.contains("KEY_TEMPLATES"));
        assertTrue(source.contains("decodeSchemaV1"));
        assertTrue(source.contains("decodeSchemaV2"));
        assertTrue(source.contains("packageConfigs"));
        assertTrue(source.contains("defaultPrefill"));
        assertTrue(source.contains("package_config."));
        assertTrue(source.contains("packageConfigFieldKeyFromRemainder"));
        assertTrue(source.contains("KEY_TYPE"));
        assertTrue(source.contains("KEY_VALUE"));
        assertTrue(source.contains("switch (type)"));
        assertTrue(source.contains("Unsupported backup schema version"));
        assertTrue(source.contains("Unsupported backup value type"));
    }

    @Test
    public void codecSupportsTypefaceIdStringEntries() throws IOException {
        String codec = read("src/main/java/com/dpis/module/backup/ConfigBackupCodec.java");
        String store = read("src/main/java/com/dpis/module/DpisConfigStore.java");
        String settings = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");

        assertTrue(store.contains("\"font.\" + packageName + \".typeface_id\""));
        assertTrue(settings.contains("Map<String, Object> entries = localStore.snapshotBackup();"));
        assertTrue(settings.contains("String payload = ConfigBackupCodec.encode(entries);"));
        assertTrue(codec.contains("putPackageConfigEntry"));
        assertTrue(codec.contains("putPackageOwnedConfigEntry"));
        assertTrue(codec.contains("putDefaultPrefillEntry"));
        assertTrue(codec.contains("putTemplateEntry"));
        assertTrue(codec.contains("decodePackageConfigsInto"));
        assertTrue(codec.contains("decodePackageOwnedConfigsInto"));
        assertTrue(codec.contains("decodeTemplatesInto"));
        assertTrue(settings.contains("ConfigBackupCodec.decode(payload)"));
        assertTrue(settings.contains("localStore.replaceBackup(entries)"));
        assertTrue(store.contains("BACKUP_EXCLUDED_PREFIXES"));
        assertTrue(store.contains("\"font.library.\""));
        assertTrue(store.contains("\"font.debug.\""));
        assertTrue(store.contains("\"runtime.\""));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
