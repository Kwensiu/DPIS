package com.dpis.module;

import static org.junit.Assert.assertFalse;
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
        assertTrue(source.contains("syncHyperOsNativeProxyAfterSave(item, views, state)"));
        assertTrue(source.contains("host.applyHyperOsNativeProxy(item, onFinished)"));
        assertTrue(source.contains("host.unmountHyperOsNativeProxy(item"));
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
        assertTrue(source.contains("refreshDialogState(views, state, style, systemHooksEnabled, item.packageName);"));
    }

    @Test
    public void binderDoesNotShowHyperOsNativeProxyStatusTextInSheet() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String layout = read("src/main/res/layout/dialog_app_config.xml");

        assertFalse(source.contains("bindHyperOsNativeWarning("));
        assertFalse(source.contains("resolveHyperOsNativeWarningText("));
        assertFalse(source.contains("HyperOsNativeProxyStatus.inspect(activity, item.packageName)"));
        assertFalse(layout.contains("dialog_hyperos_native_warning"));
    }

    @Test
    public void binderWiresFontHookDomainButtonToHost() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String layout = read("src/main/res/layout/dialog_app_config.xml");

        assertTrue(layout.contains("android:id=\"@+id/dialog_font_hook_domains_button\""));
        assertTrue(source.contains("void showFontHookDomains(AppListItem item, Runnable onStateChanged);"));
        assertTrue(source.contains("String getFontHookDomainsButtonText(String packageName);"));
        assertTrue(source.contains("views.fontHookDomainsButton.setOnClickListener"));
        assertTrue(source.contains("host.showFontHookDomains(item,"));
        assertTrue(source.contains("host.getFontHookDomainsButtonText(packageName)"));
        assertTrue(source.contains("bindFontHookDomainsButton(views.fontHookDomainsButton, packageName);"));
    }

    @Test
    public void fontHookDomainDialogUsesImmediateEditorLayout() throws IOException {
        String source = read("src/main/java/com/dpis/module/FontHookDomainDialog.java");
        String dialogLayout = read("src/main/res/layout/dialog_font_hook_domains.xml");
        String itemLayout = read("src/main/res/layout/item_font_hook_domain.xml");

        assertTrue(source.contains("setTitle(R.string.dialog_font_hook_domains_dialog_title)"));
        assertTrue(source.contains("host.saveCustom(packageName, selectedKnown, automaticKnown, unknown)"));
        assertTrue(source.contains("host.restoreRecommended(packageName)"));
        assertTrue(source.contains("FontHookDomainRegistry.orderedCustomizableDisplayIdsList()"));
        assertTrue(source.contains("FontHookDomainRegistry.orderedCustomizableDisplaySubset("));
        assertTrue(source.contains("createSubtitleText(activity, domainId)"));
        assertTrue(source.contains("resolveRiskDotColorRes(domainId)"));
        assertTrue(source.contains("font_hook_domain_risk_low"));
        assertTrue(source.contains("font_hook_domain_risk_medium"));
        assertTrue(source.contains("font_hook_domain_risk_high"));
        assertFalse(source.contains("new LinkedHashSet<>(FontHookDomainRegistry.orderedIdsList())"));
        assertTrue(source.contains("title.setText(resolveDomainTitleRes(domainId));"));
        assertTrue(source.contains("title.setText(domainId);"));
        assertFalse(source.contains("title.setText(known ? resolveDomainTitleRes(domainId) : 0);"));
        assertFalse(source.contains("setPositiveButton"));
        assertFalse(source.contains("setNegativeButton"));
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_known_container"));
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_unknown_container"));
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_restore_button"));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_title"));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_subtitle"));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_switch"));
    }



    @Test
    public void binderDoesNotKeepEmptyHyperOsSectionWrapper() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");

        assertFalse(source.contains("bindHyperOsNativeSection"));
    }

    @Test
    public void simplifiedChineseHyperOsProxyMessagesAreLocalized() throws IOException {
        String strings = read("src/main/res/values-zh-rCN/strings.xml");

        assertFalse(strings.contains("HyperOS Native Proxy applied. Restart target app."));
        assertFalse(strings.contains("HyperOS Native Proxy apply failed. Check root and native directory."));
        assertTrue(strings.contains("HyperOS &#x517C;&#x5BB9;&#x652F;&#x6301;"));
        assertTrue(strings.contains("&#x8BBE;&#x7F6E;&#x5931;&#x8D25;"));
    }

    @Test
    public void resetButtonOnlyClearsDialogInputsUntilSaved() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        int resetStart = source.indexOf("views.disableButton.setOnClickListener");
        int saveStart = source.indexOf("views.saveButton.setOnClickListener");
        String resetBlock = source.substring(resetStart, saveStart);

        assertFalse(resetBlock.contains("unmountHyperOsNativeProxy"));
    }

    @Test
    public void savingViewportConfigPublishesRuntimeViewportTarget() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");

        assertTrue(source.contains("ViewportApplyMode.SYSTEM_EMULATION.equals"));
        assertTrue(source.contains("ViewportPropertySyncer.publishTargetAsync(item.packageName, widthDp, viewportMode)"));
        assertTrue(source.contains("ViewportPropertySyncer.clearTargetAsync(item.packageName)"));
    }

    @Test
    public void savingEmptyFontConfigClearsFontRuntimeTargets() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");
        int clearStart = source.indexOf("if (fontScalePercent == null)");
        int configuredStart = source.indexOf("} else {", clearStart);
        String clearBlock = source.substring(clearStart, configuredStart);

        assertTrue(clearBlock.contains("FontRuntimePropertySyncer.clearTargetAsync(item.packageName)"));
    }

    @Test
    public void disablingDpisClearsRuntimePropertiesForAllCompatPaths() throws IOException {
        String source = read("src/main/java/com/dpis/module/MainActivity.java");
        int disableStart = source.indexOf("if (!enabled) {");
        int disableEnd = source.indexOf("}", disableStart);
        String disableBlock = source.substring(disableStart, disableEnd);

        assertTrue(disableBlock.contains("FontRuntimePropertySyncer.clearTargetAsync(packageName)"));
        assertTrue(disableBlock.contains("ViewportPropertySyncer.clearTargetAsync(packageName)"));
    }

    @Test
    public void savingFontConfigPublishesUnifiedFontRuntimeTarget() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");

        assertTrue(source.contains("FontRuntimePropertySyncer.publishTargetAsync("));
        assertTrue(source.contains("item.packageName, fontScalePercent"));
        assertTrue(source.contains("fontMode,"));
        assertTrue(source.contains("store.isHyperOsFlutterFontHookEnabled()"));
        assertTrue(source.contains("FontApplyMode.SYSTEM_EMULATION.equals"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
