package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class ProcessActionHandlerSourceSmokeTest {
    private static final String PROCESS_ACTION_HANDLER_SOURCE =
            "src/main/java/com/dpis/module/process/ProcessActionHandler.java";

    @Test
    public void processActionsDoNotUseMonkeyToLaunchApps() throws IOException {
        String source = read(PROCESS_ACTION_HANDLER_SOURCE);
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
        String source = read(PROCESS_ACTION_HANDLER_SOURCE);
        String rootLauncher = read("src/main/java/com/dpis/module/root/RootAppProcessLauncher.java");

        assertFalse(source.contains("rootAccessCache"));
        assertTrue(source.contains("rootLauncher.start(packageName)"));
        assertFalse(rootLauncher.contains("hasRootAccess()"));
        assertFalse(rootLauncher.contains("rootAccessCache"));
    }

    @Test
    public void processActionsRequireRootOnlyForRestartAndStop() throws IOException {
        String source = read(PROCESS_ACTION_HANDLER_SOURCE);
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("requiresRoot(action) && !hasRootAccess()"));
        assertTrue(source.contains("rootRequiredMessageResId(action)"));
        assertTrue(source.contains("return action == Action.RESTART || action == Action.STOP;"));
        assertTrue(source.contains("RootAccessProbe.probe()"));
        assertFalse(source.contains("rootAccessCache"));
        assertTrue(strings.contains("dialog_process_restart_requires_root"));
        assertTrue(strings.contains("dialog_process_stop_requires_root"));
    }

    @Test
    public void processActionsDoNotExposeTemporaryLaunchModeToasts() throws IOException {
        String source = read(PROCESS_ACTION_HANDLER_SOURCE);
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
        String source = read(PROCESS_ACTION_HANDLER_SOURCE);

        assertTrue(source.contains("item.systemApp && action != Action.START"));
        assertFalse(source.contains("new AlertDialog.Builder(activity)"));
    }

    @Test
    public void processActionConfirmationUsesSharedComposeDialog() throws IOException {
        String source = read(PROCESS_ACTION_HANDLER_SOURCE);
        String dialog = read("src/main/java/com/dpis/module/ui/dialog/ConfirmDialog.kt");

        assertTrue(source.contains("ConfirmDialog.show("));
        assertTrue(source.contains("R.string.dialog_process_action_confirm_title"));
        assertTrue(source.contains("R.string.dialog_process_action_confirm_message"));
        assertFalse(source.contains("R.layout.dialog_process_action_confirm"));
        assertTrue(dialog.contains("fun ConfirmDialogContent("));
        assertTrue(dialog.contains("DialogWindowSizer.applyStandardWidth(dialog, activity)"));
        assertTrue(dialog.contains("R.dimen.dialog_surface_padding_horizontal"));
        assertTrue(dialog.contains("R.dimen.dialog_body_spacing"));
        assertTrue(dialog.contains("R.dimen.dialog_action_spacing_top"));
        assertTrue(dialog.contains("R.dimen.dialog_action_spacing_between"));
        assertTrue(dialog.contains("R.color.dpis_warn_container"));
    }

    @Test
    public void systemAppConfirmationShowsAppLabelAndUsesMatchingFormatArgs() throws IOException {
        String source = read(PROCESS_ACTION_HANDLER_SOURCE);
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("actionLabel,"));
        assertTrue(source.contains("item.label)"));
        assertTrue(strings.contains("%2$s\\n"));
        assertFalse(strings.contains("%3$s\\n"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
