package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class AppConfigDialogBinderSourceSmokeTest {
    @Test
    public void binder_wiresExpectedActionButtons() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");

        assertTrue(source.contains("views.scopeButton.setOnClickListener"));
        assertTrue(source.contains("host.toggleScope(item, state.scopeSelected"));
        assertTrue(source.contains("views.dpisToggleButton.setOnClickListener"));
        assertTrue(source.contains("host.setDpisEnabled(item.packageName, nextEnabled)"));
        assertTrue(source.contains("views.startButton.setOnClickListener"));
        assertTrue(source.contains("ProcessAction.START"));
        assertTrue(source.contains("views.restartButton.setOnClickListener"));
        assertTrue(source.contains("ProcessAction.RESTART"));
        assertTrue(source.contains("views.stopButton.setOnClickListener"));
        assertTrue(source.contains("ProcessAction.STOP"));
        assertTrue(source.contains("views.disableButton.setOnClickListener"));
        assertTrue(source.contains("views.viewportInputView.setText(\"\")"));
        assertTrue(source.contains("bindViewportModeToggle(views.viewportModeToggle, ViewportApplyMode.FIELD_REWRITE, true)"));
        assertTrue(source.contains("bindFontModeToggle(views.fontModeToggle, FontApplyMode.FIELD_REWRITE, true)"));
        assertTrue(source.contains("host.saveAppConfig("));
        assertTrue(source.contains("views.saveButton.setOnClickListener"));
        assertTrue(source.contains("host.saveAppConfig("));
        assertTrue(source.contains("showSaveButtonFeedback(views.saveButton)"));
    }

    @Test
    public void binder_validationWatcherUpdatesSaveStateAndStatus() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");

        assertTrue(source.contains("views.viewportInputView.addTextChangedListener(validationWatcher)"));
        assertTrue(source.contains("views.fontInputView.addTextChangedListener(validationWatcher)"));
        assertTrue(source.contains("updateSaveButtonState(views.viewportInputLayout, views.viewportInputView,"));
        assertTrue(source.contains("refreshDialogState(views, state, style, systemHooksEnabled);"));
    }

    @Test
    public void binderShowsHyperOsNativeProxyStatus() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("HyperOsNativeProxyStatus.inspect(activity, item.packageName)"));
        assertTrue(source.contains("dialog_hyperos_native_proxy_present"));
        assertTrue(source.contains("dialog_hyperos_native_proxy_missing"));
        assertTrue(source.contains("dialog_hyperos_native_proxy_unknown"));
        assertTrue(strings.contains("dialog_hyperos_native_proxy_present"));
        assertTrue(strings.contains("dialog_hyperos_native_proxy_missing"));
        assertTrue(strings.contains("dialog_hyperos_native_proxy_unknown"));
    }

    @Test
    public void savingEmptyFontConfigClearsHyperOsNativeTarget() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");

        assertTrue(source.contains("HyperOsNativeFontPropertySyncer.clearFontTargetAsync(item.packageName)"));
    }

    @Test
    public void savingFontConfigPublishesHyperOsNativeTargetWhenModeEnabled() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");

        assertTrue(source.contains("FontApplyMode.isEnabled(fontMode)"));
        assertTrue(source.contains("HyperOsNativeFontPropertySyncer.publishForceFontTargetAsync("));
        assertTrue(source.contains("item.packageName, fontScalePercent"));
    }
    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
