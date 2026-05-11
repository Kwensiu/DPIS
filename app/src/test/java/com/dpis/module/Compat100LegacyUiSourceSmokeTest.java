package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class Compat100LegacyUiSourceSmokeTest {
    @Test
    public void scopeUnavailableActionPromptsManualLsposedSelection() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemScopeCoordinator.java");
        int unavailableStart = source.indexOf("if (service == null)");
        int unavailableEnd = source.indexOf("}", unavailableStart);
        String unavailableBlock = source.substring(unavailableStart, unavailableEnd);

        assertFalse(unavailableBlock.contains("openLsposedManager"));
        assertFalse(unavailableBlock.contains("scope_manual_manage_required"));
        assertFalse(unavailableBlock.contains("scope_manual_open_failed"));
        assertFalse(unavailableBlock.contains("R.string.status_save_requires_init"));
    }

    @Test
    public void unknownScopeHidesInjectionStatusAndDisablesScopeAction() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppStatusFormatter.java");
        String dialogBinder = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String strings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(source.contains("scopeKnown"));
        assertFalse(source.contains("labels.scopeUnknown"));
        assertTrue(dialogBinder.contains("scopeButton.setEnabled(scopeKnown);"));
        assertTrue(dialogBinder.contains("scopeButton.setAlpha(scopeKnown ? 1f : 0.6f);"));
        assertFalse(strings.contains("<string name=\"app_status_scope_unknown\">"));
        assertFalse(strings.contains("<string name=\"scope_manual_button\">"));
        assertTrue(strings.contains("LSPosed"));
        assertFalse(strings.contains(
                "remote preferences 未初始化，先重新打开模块 App</string>\n    <string name=\"scope_manual_manage_required\""));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
