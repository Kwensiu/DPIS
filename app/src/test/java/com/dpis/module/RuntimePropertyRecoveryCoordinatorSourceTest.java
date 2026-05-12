package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public final class RuntimePropertyRecoveryCoordinatorSourceTest {

    @Test
    public void coordinatorCentralizesRuntimeMirrorResync() throws IOException {
        String source = readProjectFile(
                "src/main/java/com/dpis/module/RuntimePropertyRecoveryCoordinator.java");
        String app = readProjectFile("src/main/java/com/dpis/module/DpisApplication.java");
        String receiver = readProjectFile(
                "src/main/java/com/dpis/module/DpisPackageLifecycleReceiver.java");

        assertTrue(source.contains("ViewportPropertySyncer.syncConfiguredTargetsAsync(store)"));
        assertTrue(source.contains("FontRuntimePropertySyncer.syncConfiguredTargetsAsync(store)"));
        assertTrue(source.contains("idempotent"));
        assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore)"));
        assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(remoteStore)"));
        assertTrue(receiver.contains("best-effort triggers"));
        assertTrue(receiver.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(store)"));
    }

    private static String readProjectFile(String relativePath) throws IOException {
        Path path = Path.of(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
