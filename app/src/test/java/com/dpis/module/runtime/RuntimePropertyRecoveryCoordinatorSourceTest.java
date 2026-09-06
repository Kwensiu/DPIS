package com.dpis.module;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public final class RuntimePropertyRecoveryCoordinatorSourceTest {

    @Test
    public void coordinatorCentralizesRuntimeMirrorResync() throws IOException {
        String source = readProjectFile(
                "src/main/java/com/dpis/module/runtime/RuntimePropertyRecoveryCoordinator.java");
        String app = readProjectFile("src/main/java/com/dpis/module/DpisApplication.kt");
        String receiver = readProjectFile(
                "src/main/java/com/dpis/module/runtime/DpisPackageLifecycleReceiver.java");

        assertTrue(source.contains("ViewportPropertySyncer.syncConfiguredTargetsAsync(store)"));
        assertTrue(source.contains("FontRuntimePropertySyncer.syncConfiguredTargetsAsync(store)"));
        assertTrue(source.contains("FontHookDomainPropertySyncer.syncConfiguredTargetsAsync(store)"));
        assertTrue(source.contains("WechatDpiPropertySyncer.syncConfiguredTargetsAsync(store)"));
        assertTrue(source.contains("RuntimeDebugPropertySyncer.publishAsync("));
        assertTrue(source.contains("store.isGlobalLogEnabled()"));
        assertTrue(source.contains("store.isFontDebugOverlayEnabled()"));
        assertTrue(source.contains("idempotent"));
        assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(configStore)"));
        assertTrue(app.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(refreshedStore)"));
        assertTrue(app.contains("migrateLocalConfigStore(configStore)"));
        assertTrue(app.contains("migrateLocalConfigStore(localStore)"));
        assertTrue(app.contains("store.migrateLegacyWechatDpi()"));
        assertTrue(app.contains("store.migrateLegacyPackageConfigToAggregated()"));
        assertTrue(receiver.contains("best-effort triggers"));
        assertTrue(receiver.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(store)"));
    }

    private static String readProjectFile(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
