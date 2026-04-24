package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class UpdateStateStoreTest {
    @Test
    public void storeDefinesSharedPreferencesKeysAndReadsWritesUpdateState() throws IOException {
        String source = read("src/main/java/com/dpis/module/UpdateStateStore.java");

        assertTrue(source.contains("final class UpdateStateStore"));
        assertTrue(source.contains("static final String PREFS_NAME"));
        assertTrue(source.contains("static final String KEY_LAST_UPDATE_CHECK_TIMESTAMP"));
        assertTrue(source.contains("static final String KEY_LAST_UPDATE_CHECK_FAILED"));
        assertTrue(source.contains("static final String KEY_LAST_PROMPTED_UPDATE_VERSION_CODE"));
        assertTrue(source.contains("long getLastUpdateCheckTimestamp()"));
        assertTrue(source.contains("boolean wasLastUpdateCheckFailed()"));
        assertTrue(source.contains("int getLastPromptedUpdateVersionCode()"));
        assertTrue(source.contains("void setLastUpdateCheckTimestamp("));
        assertTrue(source.contains("void setLastUpdateCheckFailed("));
        assertTrue(source.contains("void setLastPromptedUpdateVersionCode("));
        assertTrue(source.contains("UpdateCoordinator.State buildCoordinatorState("));
        assertTrue(source.contains("void applyStartupCheckState("));
        assertTrue(source.contains("void applyPromptedVersion("));
    }

    @Test
    public void storeBuildsCoordinatorStateFromPersistedFields() throws IOException {
        String storeSource = read("src/main/java/com/dpis/module/UpdateStateStore.java");
        String coordinatorSource = read("src/main/java/com/dpis/module/UpdateCoordinator.java");

        assertTrue(storeSource.contains("getLastUpdateCheckTimestamp()"));
        assertTrue(storeSource.contains("wasLastUpdateCheckFailed()"));
        assertTrue(storeSource.contains("getLastPromptedUpdateVersionCode()"));
        assertTrue(coordinatorSource.contains("lastUpdateCheckTimestampMs"));
        assertTrue(coordinatorSource.contains("lastUpdateCheckFailed"));
        assertTrue(coordinatorSource.contains("lastPromptedUpdateVersionCode"));
    }

    @Test
    public void storeApplyStartupCheckStatePersistsTimestampAndFailure() throws IOException {
        String source = read("src/main/java/com/dpis/module/UpdateStateStore.java");

        assertTrue(source.contains("setLastUpdateCheckTimestamp(state.lastUpdateCheckTimestampMs)"));
        assertTrue(source.contains("setLastUpdateCheckFailed(state.lastUpdateCheckFailed)"));
    }

    @Test
    public void storeApplyPromptedVersionPersistsVersionCode() throws IOException {
        String source = read("src/main/java/com/dpis/module/UpdateStateStore.java");

        assertTrue(source.contains("setLastPromptedUpdateVersionCode(state.lastPromptedUpdateVersionCode)"));
    }

    @Test
    public void storeNullGuardsAreInPlace() throws IOException {
        String source = read("src/main/java/com/dpis/module/UpdateStateStore.java");

        assertTrue(source.contains("if (state == null) {"));
        assertTrue(source.contains("throw new IllegalArgumentException("));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
