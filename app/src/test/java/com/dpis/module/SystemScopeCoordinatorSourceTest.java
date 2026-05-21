package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class SystemScopeCoordinatorSourceTest {
    @Test
    public void compat100EffectiveStateDoesNotRequireLibxposedService() throws Exception {
        String source = read("src/main/java/com/dpis/module/SystemScopeCoordinator.java");

        assertTrue(source.contains("\"compat100\".equals(BuildConfig.FLAVOR)"));
        assertTrue(source.contains("return desiredEnabled;"));
    }

    @Test
    public void requestScopeReportsStartedAndClearsPendingOnAllOutcomes() throws Exception {
        String source = read("src/main/java/com/dpis/module/SystemScopeCoordinator.java");
        int methodStart = source.indexOf("boolean requestScope(String packageName,\n            String appLabel,\n            Runnable onTurnedInScope,\n            Runnable onRequestFinished,\n            boolean showNotice)");
        int methodEnd = source.indexOf("boolean resolveSystemHookEffectiveEnabled", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertTrue(method.contains("if (service == null)"));
        assertTrue(method.contains("return false;"));
        assertTrue(method.contains("if (showNotice)"));
        assertTrue(method.contains("host.showToast(R.string.system_hooks_scope_request_notice);"));
        assertTrue(method.contains("service.requestScope(Collections.singletonList(packageName)"));
        assertTrue(method.contains("if (onRequestFinished != null)"));
        assertTrue(method.contains("onRequestFinished.run();"));
        assertTrue(method.contains("return true;"));
        assertTrue(method.contains("catch (RuntimeException exception)"));
        assertTrue(method.contains("host.showToast(R.string.scope_add_failed, exception.getMessage())"));
    }

    private static String read(String relativePath) throws Exception {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
