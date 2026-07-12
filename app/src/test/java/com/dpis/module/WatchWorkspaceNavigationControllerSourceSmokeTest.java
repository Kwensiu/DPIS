package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class WatchWorkspaceNavigationControllerSourceSmokeTest {
    @Test
    public void compactWatchNavigationUsesTheExistingWorkspaceMenu() throws IOException {
        String source = read("src/main/java/com/dpis/module/ui/WatchWorkspaceNavigationController.java");

        assertTrue(source.contains("WatchUiMode.shouldUseCompactUi(context)"));
        assertTrue(source.contains("navigationView.getMenu().size() != WORKSPACE_COUNT"));
        assertTrue(source.contains("navigationContainer.setVisibility(View.GONE)"));
        assertTrue(source.contains("MENU_BUTTON_SIZE_DP = 48"));
        assertTrue(source.contains("MENU_ARC_RADIUS_DP = 104"));
        assertTrue(source.contains("MENU_ARC_START_ANGLE_DEGREES = 210"));
        assertTrue(source.contains("Math.toRadians(angleDegrees)"));
        assertTrue(source.contains("mainButton.setShapeAppearanceModel"));
        assertTrue(source.contains("MENU_CORNER_RADIUS_DP = MENU_BUTTON_SIZE_DP / 2"));
        assertTrue(source.contains("host.onWorkspaceSelected(item.getItemId())"));
        assertTrue(source.contains("colorSecondaryContainer"));
    }

    @Test
    public void mainActivityKeepsWatchNavigationOutsideWorkspaceStateRendering() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("WatchWorkspaceNavigationController.attachIfSupported("));
        assertTrue(source.contains("watchWorkspaceNavigationController.setSelectedItem("));
        assertTrue(source.contains("watchWorkspaceNavigationController.closeMenuIfExpanded()"));
        assertTrue(source.contains("MainUiAction.workspaceModeChanged(workspaceModeForButtonId(itemId))"));
    }

    @Test
    public void compactRoundUiUsesSafePaddingAndAvoidsTheRedundantPermissionBadge()
            throws IOException {
        String watchMode = read("src/main/java/com/dpis/module/ui/WatchUiMode.java");
        String insets = read("src/main/java/com/dpis/module/ui/WindowInsetsBinder.java");
        String fontScale = read(
                "src/main/java/com/dpis/module/settings/SystemFontScaleToolBinder.java");
        String dimensions = read("src/main/res/values-round/dimens.xml");

        assertTrue(watchMode.contains("shouldApplyRoundSafePadding"));
        assertTrue(insets.contains("R.dimen.round_screen_safe_padding"));
        assertTrue(insets.contains("int initialRoundSafePadding"));
        assertTrue(insets.contains("Insets.NONE, initialRoundSafePadding"));
        assertTrue(insets.contains("windowManager.getCurrentWindowMetrics().getWindowInsets()"));
        assertTrue(insets.contains("RoundedCorner.POSITION_TOP_LEFT"));
        assertTrue(insets.contains("safeDrawing.top + roundSafePadding"));
        assertTrue(fontScale.contains("WatchUiMode.shouldUseCompactUi(activity)"));
        assertTrue(fontScale.contains("case PERMISSION_REQUIRED:"));
        assertTrue(dimensions.contains("round_screen_safe_padding\">32dp"));
        assertTrue(dimensions.contains("page_toolbar_padding_top\">28dp"));
    }

    @Test
    public void compactWatchChromeCentersStandaloneWorkspaceTitlesAndUsesDenseSearchTokens()
            throws IOException {
        String main = read("src/main/java/com/dpis/module/MainActivity.java");
        String chrome = read("src/main/java/com/dpis/module/ui/WatchWorkspaceChromeBinder.java");
        String home = read("src/main/res/layout/home_workspace.xml");
        String settings = read("src/main/res/layout/settings_workspace.xml");
        String dimensions = read("src/main/res/values-round/dimens.xml");

        assertTrue(main.contains("WatchWorkspaceChromeBinder.applyIfSupported("));
        assertTrue(chrome.contains("WatchUiMode.shouldUseCompactUi(context)"));
        assertTrue(chrome.contains("title.setGravity(Gravity.CENTER)"));
        assertTrue(chrome.contains("R.id.home_workspace_subtitle"));
        assertTrue(chrome.contains("applyTopContainerInsets(View topContainer)"));
        assertTrue(chrome.contains("compactWatch = topContainer != null"));
        assertTrue(chrome.contains("R.id.home_workspace_title"));
        assertTrue(chrome.contains("R.id.settings_workspace_title"));
        assertTrue(home.contains("@+id/home_workspace_title"));
        assertTrue(home.contains("@+id/home_workspace_subtitle"));
        assertTrue(home.contains("home_workspace_header_safe_padding"));
        assertTrue(settings.contains("@+id/settings_workspace_title"));
        assertTrue(dimensions.contains("main_search_card_height\">48dp"));
        assertTrue(dimensions.contains("main_toolbar_padding_horizontal\">8dp"));
        assertTrue(dimensions.contains("round_screen_safe_padding\">32dp"));
        assertTrue(dimensions.contains("home_workspace_round_safe_padding\">6dp"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
