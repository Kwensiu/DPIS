package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FontDebugStatsFileBridgeTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void importIfNewerIgnoresOlderFileData() {
        FakePrefs preferences = new FakePrefs();
        preferences.edit()
                .putLong(FontDebugStatsStore.KEY_UPDATED_AT, 200L)
                .putString(FontDebugStatsStore.KEY_CHAIN_5S, "new")
                .apply();
        Properties properties = new Properties();
        properties.setProperty(FontDebugStatsStore.EXTRA_UPDATED_AT, "100");
        properties.setProperty(FontDebugStatsStore.EXTRA_CHAIN_5S, "old");

        FontDebugStatsFileBridge.importIfNewer(preferences, properties);

        assertEquals(200L, preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L));
        assertEquals("new", preferences.getString(FontDebugStatsStore.KEY_CHAIN_5S, null));
    }

    @Test
    public void importIfNewerAppliesNewerFileData() {
        FakePrefs preferences = new FakePrefs();
        preferences.edit()
                .putLong(FontDebugStatsStore.KEY_UPDATED_AT, 100L)
                .putString(FontDebugStatsStore.KEY_CHAIN_5S, "old")
                .apply();
        Properties properties = new Properties();
        properties.setProperty(FontDebugStatsStore.EXTRA_UPDATED_AT, "200");
        properties.setProperty(FontDebugStatsStore.EXTRA_CHAIN_5S, "new");
        properties.setProperty(FontDebugStatsStore.EXTRA_EVENT_TOTAL, "3");

        FontDebugStatsFileBridge.importIfNewer(preferences, properties);

        assertEquals(200L, preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L));
        assertEquals("new", preferences.getString(FontDebugStatsStore.KEY_CHAIN_5S, null));
        assertEquals(3, preferences.getInt(FontDebugStatsStore.KEY_EVENT_TOTAL, 0));
    }

    @Test
    public void importIfNewerIgnoresMissingTimestamp() {
        FakePrefs preferences = new FakePrefs();
        preferences.edit()
                .putLong(FontDebugStatsStore.KEY_UPDATED_AT, 100L)
                .putString(FontDebugStatsStore.KEY_CHAIN_5S, "current")
                .apply();
        Properties properties = new Properties();
        properties.setProperty(FontDebugStatsStore.EXTRA_CHAIN_5S, "missing timestamp");

        FontDebugStatsFileBridge.importIfNewer(preferences, properties);

        assertEquals(100L, preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L));
        assertEquals("current", preferences.getString(FontDebugStatsStore.KEY_CHAIN_5S, null));
    }

    @Test
    public void resolveFileUsesAppSpecificExternalDir() throws IOException {
        File externalFilesDir = temporaryFolder.newFolder("external-files");

        File file = FontDebugStatsFileBridge.resolveFileForTest(externalFilesDir);

        assertEquals(new File(new File(externalFilesDir, "font_debug_stats"),
                "font_debug_stats.properties"), file);
    }

    @Test
    public void importIfNewerReadsLegacyFileBeforeCleanup() throws IOException {
        FakePrefs preferences = new FakePrefs();
        File legacyFile = createStatsFile(temporaryFolder.newFolder("Downloads", "DPIS"),
                200L, "legacy");

        FontDebugStatsFileBridge.importIfNewer(preferences, legacyFile);
        FontDebugStatsFileBridge.deleteLegacyPublicFileForTest(legacyFile);

        assertEquals(200L, preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L));
        assertEquals("legacy", preferences.getString(FontDebugStatsStore.KEY_CHAIN_5S, null));
        assertFalse(legacyFile.exists());
    }

    @Test
    public void deleteLegacyPublicFileDeletesOnlyExactLegacyFile() throws IOException {
        File downloads = temporaryFolder.newFolder("Downloads");
        File legacyDir = new File(downloads, "DPIS");
        File siblingDir = new File(downloads, "Other");
        File siblingFile = new File(legacyDir, "keep.properties");
        File otherFile = new File(siblingDir, "font_debug_stats.properties");
        assertEquals(true, legacyDir.mkdirs());
        assertEquals(true, siblingDir.mkdirs());
        writeText(siblingFile, "keep");
        writeText(otherFile, "keep");
        File legacyFile = FontDebugStatsFileBridge.resolveLegacyPublicFileForTest(downloads);
        writeText(legacyFile, "legacy");

        FontDebugStatsFileBridge.deleteLegacyPublicFileForTest(legacyFile);

        assertFalse(legacyFile.exists());
        assertEquals(true, legacyDir.isDirectory());
        assertEquals(true, siblingFile.isFile());
        assertEquals(true, otherFile.isFile());
    }

    @Test
    public void deleteLegacyPublicFileRemovesEmptyLegacyDir() throws IOException {
        File downloads = temporaryFolder.newFolder("Downloads");
        File legacyFile = FontDebugStatsFileBridge.resolveLegacyPublicFileForTest(downloads);
        File legacyDir = legacyFile.getParentFile();
        assertEquals(true, legacyDir.mkdirs());
        writeText(legacyFile, "legacy");

        FontDebugStatsFileBridge.deleteLegacyPublicFileForTest(legacyFile);

        assertFalse(legacyFile.exists());
        assertFalse(legacyDir.exists());
    }

    private static File createStatsFile(File dir, long updatedAt, String chain5s) throws IOException {
        File file = new File(dir, "font_debug_stats.properties");
        Properties properties = new Properties();
        properties.setProperty(FontDebugStatsStore.EXTRA_UPDATED_AT, String.valueOf(updatedAt));
        properties.setProperty(FontDebugStatsStore.EXTRA_CHAIN_5S, chain5s);
        try (FileOutputStream output = new FileOutputStream(file)) {
            properties.store(output, null);
        }
        return file;
    }

    private static void writeText(File file, String value) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            assertEquals(true, parent.mkdirs());
        }
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
