package com.dpis.module;

import com.dpis.module.updates.UpdateStateStore;

import com.dpis.module.updates.UpdateCoordinator;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class UpdateStateStoreTest {
    @Test
    public void storeDefinesSharedPreferencesKeysAndReadsWritesUpdateState() throws IOException {
        String source = read("src/main/java/com/dpis/module/updates/UpdateStateStore.kt");

        assertTrue(source.contains("class UpdateStateStore"));
        assertTrue(source.contains("const val PREFS_NAME"));
        assertTrue(source.contains("const val KEY_LAST_UPDATE_CHECK_TIMESTAMP"));
        assertTrue(source.contains("const val KEY_LAST_UPDATE_CHECK_FAILED"));
        assertTrue(source.contains("const val KEY_LAST_PROMPTED_UPDATE_VERSION_CODE"));
        assertTrue(source.contains("fun getLastUpdateCheckTimestamp()"));
        assertTrue(source.contains("fun wasLastUpdateCheckFailed()"));
        assertTrue(source.contains("fun getLastPromptedUpdateVersionCode()"));
        assertTrue(source.contains("fun setLastUpdateCheckTimestamp("));
        assertTrue(source.contains("fun setLastUpdateCheckFailed("));
        assertTrue(source.contains("fun setLastPromptedUpdateVersionCode("));
        assertTrue(source.contains("fun buildCoordinatorState("));
        assertTrue(source.contains("fun applyStartupCheckState("));
        assertTrue(source.contains("fun applyPromptedVersion("));
    }

    @Test
    public void storeBuildsCoordinatorStateFromPersistedFields() throws IOException {
        String storeSource = read("src/main/java/com/dpis/module/updates/UpdateStateStore.kt");
        String coordinatorSource = read("src/main/java/com/dpis/module/updates/UpdateCoordinator.java");

        assertTrue(storeSource.contains("getLastUpdateCheckTimestamp()"));
        assertTrue(storeSource.contains("wasLastUpdateCheckFailed()"));
        assertTrue(storeSource.contains("getLastPromptedUpdateVersionCode()"));
        assertTrue(coordinatorSource.contains("lastUpdateCheckTimestampMs"));
        assertTrue(coordinatorSource.contains("lastUpdateCheckFailed"));
        assertTrue(coordinatorSource.contains("lastPromptedUpdateVersionCode"));
    }

    @Test
    public void storeApplyStartupCheckStatePersistsTimestampAndFailure() throws IOException {
        String source = read("src/main/java/com/dpis/module/updates/UpdateStateStore.kt");

        assertTrue(source.contains("setLastUpdateCheckTimestamp(state.lastUpdateCheckTimestampMs)"));
        assertTrue(source.contains("setLastUpdateCheckFailed(state.lastUpdateCheckFailed)"));
    }

    @Test
    public void storeApplyPromptedVersionPersistsVersionCode() throws IOException {
        String source = read("src/main/java/com/dpis/module/updates/UpdateStateStore.kt");

        assertTrue(source.contains("setLastPromptedUpdateVersionCode(state.lastPromptedUpdateVersionCode)"));
    }

    @Test
    public void storeNullGuardsAreInPlace() throws IOException {
        String source = read("src/main/java/com/dpis/module/updates/UpdateStateStore.kt");

        assertTrue(source.contains("if (state == null) return"));
        assertTrue(source.contains("requireNotNull"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
