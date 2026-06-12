package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public class BatchScopeRequestCoordinatorSourceSmokeTest {
    @Test
    public void batchScopeRequestUsesOneListRequestAndManualFallbacks() throws IOException {
        String coordinator = read("src/main/java/com/dpis/module/BatchScopeRequestCoordinator.java");
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(coordinator.contains("\"modern\".equals(BuildConfig.FLAVOR)"));
        assertTrue(coordinator.contains("scopeRequester.getScope()"));
        assertTrue(coordinator.contains("scopeRequester.requestScope(requestPackages,"));
        assertFalse(coordinator.contains("Collections.singletonList"));
        assertTrue(coordinator.contains("quick_template_scope_manual_required"));
        assertTrue(coordinator.contains("onScopeRequestApproved(List<String> approved)"));
        assertTrue(coordinator.contains("host.requestAppsLoad()"));
        assertTrue(mainActivity.contains("new BatchScopeRequestCoordinator("));
        assertTrue(mainActivity.contains("createBatchScopeRequestHost()"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
