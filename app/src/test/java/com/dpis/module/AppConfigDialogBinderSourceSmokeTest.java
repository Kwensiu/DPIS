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
        assertTrue(source.contains("bindViewportModeToggle(views.viewportModeToggle, ViewportTargetType.RELATIVE_SCALE, true)"));
        assertTrue(source.contains("bindFontModeToggle(views.fontModeToggle, FontApplyMode.FIELD_REWRITE, true)"));
        assertTrue(source.contains("host.saveAppConfig("));
        assertTrue(source.contains("views.saveButton.setOnClickListener"));
        assertTrue(source.contains("host.saveAppConfig("));
        assertTrue(source.contains("showSaveButtonFeedback(views.saveButton)"));
        assertTrue(source.contains("requestScopeAfterSuccessfulSave(dialogView, item, views, state, style, systemHooksEnabled)"));
    }

    @Test
    public void saveSuccessRequestsKnownMissingScopeOnce() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        int methodStart = source.indexOf("private void requestScopeAfterSuccessfulSave");
        int methodEnd = source.indexOf("private static boolean hasActiveDialogConfig", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertTrue(method.contains("!state.scopeKnown || state.scopeSelected || state.scopeRequestPending"));
        assertTrue(method.contains("state.scopeRequestPending = true;"));
        assertTrue(method.contains("boolean requestStarted = host.requestScope(item,"));
        assertTrue(method.contains("if (requestStarted)"));
        assertTrue(method.contains("host.showToast(R.string.save_scope_request_notice)"));
        assertTrue(method.contains("state.scopeRequestPending = false;"));
    }

    @Test
    public void saveTimeScopeCallbacksOnlyRefreshAttachedSheet() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        int methodStart = source.indexOf("private void requestScopeAfterSuccessfulSave");
        int methodEnd = source.indexOf("private static boolean hasActiveDialogConfig", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertTrue(method.contains("if (!dialogView.isAttachedToWindow())"));
        assertTrue(method.contains("state.scopeSelected = true;"));
        assertTrue(method.contains("refreshDialogState(views, state, style, systemHooksEnabled, item);"));
        assertTrue(method.contains("() -> state.scopeRequestPending = false"));
    }

    @Test
    public void binder_wiresTypefaceSelector() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String saveHandler = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");
        String layout = read("src/main/res/layout/dialog_app_config.xml");
        String selectorLayout = read("src/main/res/layout/dialog_typeface_selection.xml");

        assertTrue(layout.contains("dialog_typeface_selector_button"));
        assertTrue(layout.contains("@string/dialog_typeface_selector_value"));
        assertTrue(layout.indexOf("android:id=\"@+id/dialog_typeface_selector_button\"")
                < layout.indexOf("android:id=\"@+id/dialog_font_hook_domains_button\""));
        assertTrue(source.contains("bindTypefaceSelector"));
        assertTrue(source.contains("formatTypefaceSelectorText"));
        assertTrue(source.contains("SystemFontRegistry.listRecommendedFonts()"));
        assertTrue(source.contains("TabLayout"));
        assertTrue(source.contains("R.layout.dialog_typeface_selection"));
        assertTrue(source.contains("R.string.dialog_typeface_tab_system"));
        assertTrue(source.contains("R.string.dialog_typeface_tab_imported"));
        assertTrue(source.contains("host.openTypefaceLibrary"));
        assertTrue(selectorLayout.contains("@string/dialog_typeface_manage_action"));
        assertTrue(selectorLayout.contains("@string/dialog_typeface_done_action"));
        assertTrue(source.contains("doneButton.setOnClickListener"));
        assertFalse(source.contains("renameTypeface"));
        assertFalse(source.contains("confirmDeleteTypeface"));
        assertTrue(saveHandler.contains("setTargetTypefaceId"));
        assertTrue(saveHandler.contains("clearTargetTypefaceId"));
    }

    @Test
    public void binderTreatsSelectedTypefaceAsNativeProxyConfig() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        int activeStart = source.indexOf("private static boolean hasActiveDialogConfig");
        int activeEnd = source.indexOf("private static void setSaveAndResetButtonsEnabled", activeStart);
        String activeBlock = source.substring(activeStart, activeEnd);

        assertTrue(activeBlock.contains("state.selectedTypefaceId"));
    }

    @Test
    public void typefaceSelectorKeepsMissingCurrentChoiceChecked() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        int selectorStart = source.indexOf("private void showTypefaceSelector");
        int selectorEnd = source.indexOf("private String resolveTypefaceDisplayText", selectorStart);
        String selectorBlock = source.substring(selectorStart, selectorEnd);

        assertTrue(source.contains("R.string.dialog_typeface_missing"));
        assertTrue(source.contains("containsSystemTypeface"));
        assertTrue(source.contains("containsImportedTypeface"));
        assertTrue(source.contains("option.matches(state.selectedTypefaceId)"));
    }

    @Test
    public void binder_validationWatcherUpdatesSaveStateAndStatus() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");

        assertTrue(source.contains("views.viewportInputView.addTextChangedListener(validationWatcher)"));
        assertTrue(source.contains("views.fontInputView.addTextChangedListener(validationWatcher)"));
        assertTrue(source.contains("updateSaveButtonState(views.viewportInputLayout, views.viewportInputView,"));
        assertTrue(source.contains("refreshDialogState(views, state, style, systemHooksEnabled, item);"));
        assertTrue(source.contains("AppStatusFormatter.formatCompact("));
        assertTrue(source.contains("state.selectedTypefaceId"));
        assertTrue(source.contains("showTypefaceSelector(views.typefaceSelectorButton, state,"));
        assertTrue(source.contains("views, state, style, systemHooksEnabled, item"));
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
        assertTrue(layout.indexOf("android:id=\"@+id/dialog_font_hook_domains_button\"")
                < layout.indexOf("android:id=\"@+id/dialog_stop_button\""));
        assertTrue(layout.indexOf("android:id=\"@+id/dialog_font_hook_domains_button\"")
                < layout.indexOf("@string/dialog_advanced_section_title"));
        assertTrue(source.contains("void showFontHookDomains(AppListItem item, Runnable onStateChanged);"));
        assertTrue(source.contains("String getFontHookDomainsButtonText(String packageName);"));
        assertTrue(source.contains("views.fontHookDomainsButton.setOnClickListener"));
        assertTrue(source.contains("host.showFontHookDomains(item,"));
        assertTrue(source.contains("host.getFontHookDomainsButtonText(packageName)"));
        assertTrue(source.contains("bindFontHookDomainsButton(views.fontHookDomainsButton, item.packageName);"));
    }

    @Test
    public void fontHookDomainDialogUsesImmediateEditorLayout() throws IOException {
        String source = read("src/main/java/com/dpis/module/FontHookDomainDialog.java");
        String dialogLayout = read("src/main/res/layout/dialog_font_hook_domains.xml");
        String itemLayout = read("src/main/res/layout/item_font_hook_domain.xml");

        assertTrue(source.contains("setTitle(R.string.dialog_font_hook_domains_dialog_title)"));
        assertTrue(source.contains("dialog_hook_chain_tab_interface"));
        assertTrue(source.contains("dialog_hook_chain_tab_font"));
        assertTrue(source.contains("normalizeViewportApplyModeForDisplay(currentViewportApplyMode)"));
        assertTrue(source.contains(": ViewportApplyMode.AUTO"));
        assertTrue(source.contains("buttonView.setChecked(true);"));
        assertTrue(source.contains("font_hook_domains_interface_page"));
        assertTrue(source.contains("font_hook_domains_font_page"));
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
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_tabs"));
        assertTrue(dialogLayout.contains("app:tabBackground=\"@android:color/transparent\""));
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_interface_page"));
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_font_page"));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_title"));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_subtitle"));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_switch"));
    }

    @Test
    public void viewportTargetTypeControlsInputHintAndStackedLabels() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String layout = read("src/main/res/layout/dialog_app_config.xml");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(layout.contains("android:hint=\"@string/dialog_viewport_hint_scale\""));
        assertTrue(layout.contains("android:orientation=\"horizontal\""));
        assertTrue(layout.contains("android:id=\"@+id/dialog_viewport_mode_system_label\" android:layout_width=\"0dp\" android:layout_height=\"match_parent\""));
        assertTrue(layout.contains("android:id=\"@+id/dialog_viewport_mode_compat_label\" android:layout_width=\"0dp\" android:layout_height=\"match_parent\""));
        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, item.viewportTargetSpec.type())"));
        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.RELATIVE_SCALE)"));
        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.ABSOLUTE_DP)"));
        assertTrue(source.contains("dialogView.findViewById(R.id.dialog_viewport_mode_compat_label)),"));
        assertFalse(source.contains("toggle.vertical"));
        assertTrue(strings.contains("Interface scale 50-200%"));
        assertTrue(strings.contains("Min width dp"));
        assertTrue(zhStrings.contains("&#x754C;&#x9762;&#x6BD4;&#x4F8B; 50-200%")
                || zhStrings.contains("界面比例 50-200%"));
        assertTrue(zhStrings.contains("&#x6700;&#x5C0F;&#x5BBD;&#x5EA6; dp")
                || zhStrings.contains("最小宽度 dp"));
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

        assertTrue(source.contains("ViewportApplyMode.SYSTEM.equals"));
        assertTrue(source.contains("ViewportPropertySyncer.publishTargetAsync("));
        assertTrue(source.contains("item.packageName, viewportTargetSpec, viewportApplyMode"));
        assertTrue(source.contains("ViewportPropertySyncer.clearTargetAsync(item.packageName)"));
    }

    @Test
    public void savingEmptyFontScaleClearsOnlyFontScaleRuntimeTargets() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");
        int clearStart = source.indexOf("if (fontScalePercent == null)");
        int configuredStart = source.indexOf("} else {", clearStart);
        String clearBlock = source.substring(clearStart, configuredStart);

        assertTrue(clearBlock.contains("FontRuntimePropertySyncer.clearFontScaleTargetAsync(item.packageName)"));
        assertFalse(clearBlock.contains("FontRuntimePropertySyncer.clearTargetAsync(item.packageName)"));
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
        assertTrue(source.contains("FontHookDomainDecision.isHyperOsNativeFlutterEnabled("));
        assertTrue(source.contains("FontApplyMode.SYSTEM_EMULATION.equals"));
    }

    @Test
    public void savingTypefaceConfigPublishesRuntimeTypefaceTarget() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");

        assertTrue(source.contains("FontRuntimePropertySyncer.publishTypefaceTargetAsync(item.packageName, null)"));
        assertTrue(source.contains(
                "FontRuntimePropertySyncer.publishTypefaceTargetAsync(item.packageName, selectedTypefaceId)"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
