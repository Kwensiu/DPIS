package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class ProcessActionHandlerSourceSmokeTest {
    @Test
    public void processActionsDoNotUseMonkeyToLaunchApps() throws IOException {
        String source = read("src/main/java/com/dpis/module/ProcessActionHandler.java");

        assertFalse(source.contains("monkey -p"));
        assertTrue(source.contains("rootStartPackage(packageName)"));
        assertTrue(source.contains("am start --user current"));
        assertTrue(source.contains("-a android.intent.action.MAIN"));
        assertTrue(source.contains("-c android.intent.category.LAUNCHER"));
        assertTrue(source.contains("flattenToShortString()"));
        assertTrue(source.contains("shellQuote("));
        assertTrue(source.contains("getLaunchIntentForPackage(packageName)"));
        assertTrue(source.contains("startActivity(launchIntent)"));
    }

    @Test
    public void rootStartDoesNotProbeOrCacheRootBeforeFallback() throws IOException {
        String source = read("src/main/java/com/dpis/module/ProcessActionHandler.java");
        String rootStartPackage = source.substring(
                source.indexOf("private ShellResult rootStartPackage"),
                source.indexOf("private ShellResult startPackage"));

        assertFalse(rootStartPackage.contains("hasRootAccess()"));
        assertFalse(rootStartPackage.contains("rootAccessCache"));
        assertTrue(rootStartPackage.contains("runSuCommand("));
    }

    @Test
    public void processActionsRequireRootOnlyForRestartAndStop() throws IOException {
        String source = read("src/main/java/com/dpis/module/ProcessActionHandler.java");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("requiresRoot(action) && !hasRootAccess()"));
        assertTrue(source.contains("rootRequiredMessageResId(action)"));
        assertTrue(strings.contains("dialog_process_restart_requires_root"));
        assertTrue(strings.contains("dialog_process_stop_requires_root"));
        assertFalse(source.contains("showToast(R.string.dialog_process_requires_root)"));
    }

    @Test
    public void processActionsDoNotExposeTemporaryLaunchModeToasts() throws IOException {
        String source = read("src/main/java/com/dpis/module/ProcessActionHandler.java");
        String strings = read("src/main/res/values/strings.xml");

        assertFalse(source.contains("dialog_process_launch_mode_root"));
        assertFalse(source.contains("dialog_process_launch_mode_fallback"));
        assertFalse(source.contains("dialog_process_launch_mode_force_stop_failed"));
        assertFalse(strings.contains("dialog_process_launch_mode_root"));
        assertFalse(strings.contains("dialog_process_launch_mode_fallback"));
        assertFalse(strings.contains("dialog_process_launch_mode_force_stop_failed"));
    }

    @Test
    public void systemAppStartDoesNotShowRiskConfirmation() throws IOException {
        String source = read("src/main/java/com/dpis/module/ProcessActionHandler.java");

        assertTrue(source.contains("item.systemApp && action != Action.START"));
        assertFalse(source.contains("new AlertDialog.Builder(activity)"));
    }

    @Test
    public void processActionConfirmationUsesCustomDialogLayout() throws IOException {
        String source = read("src/main/java/com/dpis/module/ProcessActionHandler.java");
        String layout = read("src/main/res/layout/dialog_process_action_confirm.xml");

        assertTrue(source.contains("R.layout.dialog_process_action_confirm"));
        assertTrue(source.contains("process_action_confirm_title"));
        assertTrue(source.contains("process_action_confirm_message"));
        assertTrue(source.contains("process_action_confirm_proceed_button"));
        assertTrue(source.contains("process_action_confirm_cancel_button"));
        assertTrue(layout.contains("Widget.Dpis.DialogActionButton.Outlined.Warn"));
        assertTrue(layout.contains("Widget.Dpis.DialogActionButton.Outlined"));
    }

    @Test
    public void systemAppConfirmationShowsAppLabelAndUsesMatchingFormatArgs() throws IOException {
        String source = read("src/main/java/com/dpis/module/ProcessActionHandler.java");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("actionLabel,\r\n                item.label")
                || source.contains("actionLabel,\n                item.label"));
        assertTrue(strings.contains("%2$s\\n"));
        assertFalse(strings.contains("%3$s\\n"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
