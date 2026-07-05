package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class ProcessActionHandlerSourceSmokeTest {
    @Test
    public void processActionsDoNotUseMonkeyToLaunchApps() throws IOException {
        String source = read("src/main/java/com/dpis/module/ProcessActionHandler.java");
        String rootLauncher = read("src/main/java/com/dpis/module/root/RootAppProcessLauncher.java");

        assertFalse(source.contains("monkey -p"));
        assertTrue(source.contains("new RootAppProcessLauncher(activity)"));
        assertTrue(source.contains("rootLauncher.start(packageName)"));
        assertTrue(rootLauncher.contains("am start --user current"));
        assertTrue(rootLauncher.contains("-a android.intent.action.MAIN"));
        assertTrue(rootLauncher.contains("-c android.intent.category.LAUNCHER"));
        assertTrue(rootLauncher.contains("flattenToShortString()"));
        assertTrue(rootLauncher.contains("shellQuote("));
        assertTrue(source.contains("getLaunchIntentForPackage(packageName)"));
        assertTrue(source.contains("startActivity(launchIntent)"));
    }

    @Test
    public void sharedRootLauncherDoesNotProbeOrCacheRootBeforeFallback() throws IOException {
        String source = read("src/main/java/com/dpis/module/ProcessActionHandler.java");
        String rootLauncher = read("src/main/java/com/dpis/module/root/RootAppProcessLauncher.java");

        assertFalse(source.contains("rootAccessCache"));
        assertTrue(source.contains("rootLauncher.start(packageName)"));
        assertFalse(rootLauncher.contains("hasRootAccess()"));
        assertFalse(rootLauncher.contains("rootAccessCache"));
    }

    @Test
    public void processActionsRequireRootOnlyForRestartAndStop() throws IOException {
        String source = read("src/main/java/com/dpis/module/ProcessActionHandler.java");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("requiresRoot(action) && !hasRootAccess()"));
        assertTrue(source.contains("rootRequiredMessageResId(action)"));
        assertTrue(source.contains("return action == Action.RESTART || action == Action.STOP;"));
        assertTrue(source.contains("RootAccessProbe.cachedResult()"));
        assertTrue(source.contains("RootAccessProbe.probe()"));
        assertFalse(source.contains("rootAccessCache"));
        assertTrue(strings.contains("dialog_process_restart_requires_root"));
        assertTrue(strings.contains("dialog_process_stop_requires_root"));
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
        assertTrue(source.contains("DialogWindowSizer.applyStandardWidth(dialog, activity)"));
        assertTrue(layout.contains("@dimen/dialog_surface_padding_horizontal"));
        assertTrue(layout.contains("@dimen/dialog_body_spacing"));
        assertTrue(layout.contains("@dimen/dialog_action_spacing_top"));
        assertTrue(layout.contains("@dimen/dialog_action_spacing_between"));
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
        return SourceSmokeTestPaths.read(relativePath);
    }
}
