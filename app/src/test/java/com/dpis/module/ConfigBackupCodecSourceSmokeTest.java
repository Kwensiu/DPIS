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
        assertTrue(settings.contains("ConfigBackupCoordinator"));
        assertTrue(settings.contains(".export(uri)"));
        assertTrue(codec.contains("putPackageConfigEntry"));
        assertTrue(codec.contains("putPackageOwnedConfigEntry"));
        assertTrue(codec.contains("putDefaultPrefillEntry"));
        assertTrue(codec.contains("putTemplateEntry"));
        assertTrue(codec.contains("decodePackageConfigsInto"));
        assertTrue(codec.contains("decodePackageOwnedConfigsInto"));
        assertTrue(codec.contains("decodeTemplatesInto"));
        assertTrue(settings.contains(".restore(uri)"));
        assertTrue(store.contains("BackupKeyPolicy"));
        assertTrue(read("src/main/java/com/dpis/module/backup/BackupKeyPolicy.java")
                .contains("font.library."));
    }

    @Test
    public void backupKeyPolicyKeepsRuntimeStateLocal() throws IOException {
        String policy = read("src/main/java/com/dpis/module/backup/BackupKeyPolicy.java");
        assertTrue(policy.contains("font.library."));
        assertTrue(policy.contains("runtime."));
        assertTrue(policy.contains("isImportable"));
    }

    @Test
    public void codecExposesTypedDocumentBoundaryAndInputLimit() throws IOException {
        String codec = read("src/main/java/com/dpis/module/backup/ConfigBackupCodec.java");
        assertTrue(codec.contains("decodeDocument"));
        assertTrue(codec.contains("MAX_JSON_CHARS"));
        assertTrue(read("src/main/java/com/dpis/module/backup/ConfigBackupCoordinator.java")
                .contains("normalizeLegacyResolutionKeys"));
        assertTrue(read("src/main/java/com/dpis/module/backup/BackupDocument.java")
                .contains("BackupMetadata metadata"));
    }

    @Test
    public void backupPolicyRejectsUnknownKeys() {
        org.junit.Assert.assertFalse(
                com.dpis.module.backup.BackupKeyPolicy.isImportable("unknown.preference"));
        org.junit.Assert.assertTrue(
                com.dpis.module.backup.BackupKeyPolicy.isImportable("package_config.com.example.viewport.width_dp"));
        org.junit.Assert.assertTrue(
                com.dpis.module.backup.BackupKeyPolicy.isImportable("fluid_cloud.hole_left"));
        org.junit.Assert.assertTrue(
                com.dpis.module.backup.BackupKeyPolicy.isImportable("ui.interface_scale_percent"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
