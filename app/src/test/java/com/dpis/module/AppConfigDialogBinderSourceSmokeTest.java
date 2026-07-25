package com.dpis.module;

import com.dpis.module.appconfig.LandAppDetailPaneBinder;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.appconfig.AppConfigDialogBinder;
import com.dpis.module.appconfig.AppConfigInputValidation;
import com.dpis.module.appconfig.AppConfigSaveHandler;

import com.dpis.module.applist.AppStatusFormatter;

import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry;

import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer;

import com.dpis.module.fonts.hookdomain.FontHookDomainDialog;

import com.dpis.module.runtime.font.FontRuntimePropertySyncer;

import com.dpis.module.viewport.DpiConfig;

import com.dpis.module.viewport.ViewportPropertySyncer;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.applist.AppListItem;

import com.dpis.module.quirks.WechatDpiPropertySyncer;
import com.dpis.module.quirks.WechatDpiSheetBinder;

import com.dpis.module.appconfig.WechatDpiConfig;

import com.dpis.module.appconfig.UnsavedBadgeBinder;

import com.dpis.module.appconfig.ConfigValueInputErrorBinder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class AppConfigDialogBinderSourceSmokeTest {
    @Test
    public void binder_wiresExpectedActionButtons() throws IOException {
        String binderSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String interactionsSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetInteractions.java");
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetActionBinder.java")
                + read("src/main/java/com/dpis/module/appconfig/AppConfigSheetModeValidationBinder.java");

        assertTrue(binderSource.contains("new AppConfigSheetInteractions(this, host)"));
        assertTrue(binderSource.contains(".bind(dialogView, item, views, state, style, systemHooksEnabled);"));
        assertTrue(interactionsSource.contains("new AppConfigSheetModeValidationBinder(binder, host)"));
        assertTrue(interactionsSource.contains("new AppConfigSheetActionBinder(binder, host)"));
        assertTrue(interactionsSource.contains(
                "modeValidationBinder.bindDialogValidation(dialogView, item, views, state, style, systemHooksEnabled);"));
        assertTrue(interactionsSource.contains(
                "actionBinder.bindDialogActions(dialogView, item, views, state, style, systemHooksEnabled);"));
        assertTrue(interactionsSource.contains(
                "modeValidationBinder.bindModeToggles(dialogView, item, views, state, style, systemHooksEnabled);"));
        assertTrue(interactionsSource.contains(
                "actionBinder.bindTypefaceSelectorAction(dialogView, item, views, state, style, systemHooksEnabled);"));
        assertTrue(source.contains("views.scopeButton.setOnClickListener"));
        assertTrue(source.contains("host.toggleScope(item, state.scopeSelected"));
        assertTrue(source.contains("views.dpisToggleButton.setOnClickListener"));
        assertTrue(source.contains("host.setDpisEnabled(item.packageName, nextEnabled)"));
        assertTrue(source.contains("views.startButton.setOnClickListener"));
        assertTrue(source.contains("ProcessAction.START"));
        assertTrue(source.contains("syncHyperOsNativeProxyAfterSave(item, views, state)"));
        assertTrue(source.contains("views.restartButton.setOnClickListener"));
        assertTrue(source.contains("ProcessAction.RESTART"));
        assertTrue(source.contains("views.stopButton.setOnClickListener"));
        assertTrue(source.contains("ProcessAction.STOP"));
        assertTrue(source.contains("views.disableButton.setOnClickListener"));
        assertTrue(source.contains("views.viewportInputView.setText(\"\")"));
        assertTrue(source.contains("state.clearHookChainStateForReset();"));
        assertTrue(source.contains("AppConfigDialogBinder.bindViewportModeToggle("));
        assertTrue(source.contains("views.viewportModeToggle, ViewportTargetType.RELATIVE_SCALE, true)"));
        assertTrue(source.contains("AppConfigDialogBinder.bindFontModeToggle("));
        assertTrue(source.contains("views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true)"));
        assertTrue(source.contains("host.saveAppConfig("));
        assertTrue(source.contains("views.saveButton.setOnClickListener"));
        assertTrue(source.contains("if (!WechatDpiSheetBinder.isInputValid(dialogView))"));
        assertTrue(source.contains("host.saveAppConfig("));
        assertTrue(source.contains("state.viewportScaleInput"));
        assertTrue(source.contains("state.viewportAbsoluteInput"));
        assertTrue(source.contains("state.draftFontHookDomainsRaw"));
        assertTrue(source.contains("showSaveButtonFeedback(views.saveButton)"));
        assertTrue(source.contains("binder.requestScopeAfterSuccessfulSave("));
        assertTrue(source.contains("dialogView, item, views, state, style, systemHooksEnabled);"));
        assertTrue(binderSource.contains("host.applyHyperOsNativeProxy(item, onFinished)"));
        assertTrue(binderSource.contains("host.unmountHyperOsNativeProxy(item"));
        assertTrue(binderSource.contains("host.isHyperOsNativeProxyCandidate(item)"));
    }

    @Test
    public void sheetModeAndInputChangesRefreshRetainedDraft() throws IOException {
        String interactionsSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetInteractions.java");
        String modeValidationSource =
                read("src/main/java/com/dpis/module/appconfig/AppConfigSheetModeValidationBinder.java");
        String mainActivitySource = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(interactionsSource.contains(
                "new AppConfigSheetModeValidationBinder(binder, host)"));
        assertTrue(countOccurrences(modeValidationSource, "host.onDraftStateChanged(state);") >= 7);
        assertTrue(mainActivitySource.contains("AppConfigEditorDraft captured = captureAppConfigDraft();"));
        assertTrue(mainActivitySource.contains("mainViewModel.setEditingDraft(captured);"));
    }

    @Test
    public void wechatDpiUsesSingleOfficialInput() throws IOException {
        String binder = read("src/main/java/com/dpis/module/quirks/WechatDpiSheetBinder.java");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(binder.contains("WechatDpiConfig.appliesTo(packageName)"));
        assertTrue(binder.contains("HapticFeedbackConstants.VIRTUAL_KEY"));
        assertTrue(binder.contains("MaterialAlertDialogBuilder"));
        assertTrue(binder.contains("R.string.dialog_wechat_dpi_help_title"));
        assertTrue(binder.contains("R.string.dialog_wechat_dpi_help_message"));
        assertTrue(binder.contains("WechatDpiConfig.isInputValid"));
        assertTrue(binder.contains("dialog_wechat_dpi_input"));
        assertTrue(binder.contains("DialogWindowSizer.applyStandardWidth("));
        assertTrue(binder.contains("anchor.getContext()"));
        assertTrue(strings.contains("WeChat DPI 200-1000"));
        assertTrue(strings.contains("WeChat-specific DisplayMetrics route"));
        assertTrue(strings.contains("Mini Programs are not supported yet."));
        assertFalse(strings.contains("dialog_wechat_target_field"));
        assertTrue(zhStrings.contains("微信 DPI 200-1000"));
        assertTrue(zhStrings.contains("暂不支持小程序"));
    }

    @Test
    public void wechatDpiSaveDoesNotClearViewportConfig() throws IOException {
        String binder = read("src/main/java/com/dpis/module/quirks/WechatDpiSheetBinder.java");
        int saveStart = binder.indexOf("static boolean save(");
        int saveEnd = binder.indexOf("static void clearDraft", saveStart);
        String saveBlock = binder.substring(saveStart, saveEnd);

        assertTrue(saveBlock.contains("store.setWechatDpi(packageName, dpi)"));
        assertTrue(saveBlock.contains("if (saved)"));
        assertTrue(saveBlock.contains("WechatDpiPropertySyncer.publishDpiAsync(packageName"));
        assertFalse(saveBlock.contains("clearTargetViewportWidthDp(packageName)"));
        assertFalse(saveBlock.contains("setTargetViewportApplyMode(packageName, ViewportApplyMode.OFF)"));
        assertFalse(saveBlock.contains("ViewportPropertySyncer.clearTargetAsync(packageName)"));
    }

    @Test
    public void wechatDpiPublishFollowsSavedHostState() throws IOException {
        String actionBinder = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetActionBinder.java");
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");

        int toggleStart = actionBinder.indexOf("views.dpisToggleButton.setOnClickListener");
        int toggleEnd = actionBinder.indexOf("views.fontHookDomainsButton.setOnClickListener", toggleStart);
        String toggleBlock = actionBinder.substring(toggleStart, toggleEnd);
        assertFalse(toggleBlock.contains("WechatDpiSheetBinder.publishForDpisState"));

        int hostStart = mainActivity.indexOf("public boolean setDpisEnabled(");
        int hostEnd = mainActivity.indexOf("@Override", hostStart + 1);
        String hostBlock = mainActivity.substring(hostStart, hostEnd);
        assertTrue(hostBlock.contains("if (saved)"));
        assertTrue(hostBlock.contains("WechatDpiSheetBinder.publishForDpisState("));
    }

    @Test
    public void appConfigSheetUsesUnsavedBadgeInsteadOfPreviewIndicator() throws IOException {
        String binderSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String layout = read("src/main/res/layout/dialog_app_config.xml");

        assertTrue(layout.contains("@layout/view_sheet_unsaved_badge_handle"));
        assertTrue(layout.contains("android:id=\"@+id/dialog_viewport_input\""));
        assertTrue(layout.contains("android:inputType=\"numberDecimal\""));
        assertFalse(layout.contains("dialog_preview_status"));
        assertFalse(layout.contains("dialog_global_prefill_preview_status"));
        assertTrue(binderSource.contains("state.captureSavedDraft(views, item != null && item.previewFromGlobalPrefill);"));
        assertTrue(binderSource.contains("UnsavedBadgeBinder.bind("));
        assertTrue(binderSource.contains("normalizeDraftText(normalizedHookDomainsRaw())"));
        assertTrue(binderSource.contains(".forRecommendedTemplateRaw(draftFontHookDomainsRaw)"));
        assertFalse(binderSource.contains("fontHookDomainsResetRequested ? \"font-reset\""));
        assertFalse(binderSource.contains("viewportApplyModeResetRequested ? \"viewport-reset\""));
        assertFalse(binderSource.contains("previewFromGlobalPrefill ? \"preview\" : \"stored\""));
        assertTrue(binderSource.contains("bindDpisToggleButton(views.dpisToggleButton, state.dpisEnabled"));
        assertTrue(binderSource.contains("dpisToggleButton.setEnabled(true);"));
        assertTrue(binderSource.contains("dpisToggleButton.setAlpha(1f);"));
    }

    @Test
    public void saveSuccessRequestsKnownMissingScopeOnce() throws IOException {
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        int methodStart = source.indexOf("void requestScopeAfterSuccessfulSave");
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
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        int methodStart = source.indexOf("void requestScopeAfterSuccessfulSave");
        int methodEnd = source.indexOf("private static boolean hasActiveDialogConfig", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertTrue(method.contains("if (!dialogView.isAttachedToWindow())"));
        assertTrue(method.contains("state.scopeSelected = true;"));
        assertTrue(method.contains("refreshDialogState(views, state, style, systemHooksEnabled, item);"));
        assertTrue(method.contains("() -> state.scopeRequestPending = false"));
    }

    @Test
    public void binder_wiresTypefaceSelector() throws IOException {
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String saveHandler = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.java");
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
        assertTrue(source.contains("DialogWindowSizer.applyLargeWidth(dialogHolder[0], activity)"));
        assertTrue(source.contains("R.string.dialog_typeface_tab_system"));
        assertTrue(source.contains("R.string.dialog_typeface_tab_imported"));
        assertTrue(source.contains("bindImportedTypefaceCollectionRows"));
        assertTrue(source.contains("showTypefaceFaceSelection"));
        assertTrue(source.contains("R.string.dialog_typeface_collection_label"));
        assertTrue(source.contains("R.string.dialog_typeface_face_select_title"));
        assertTrue(source.contains("host.openTypefaceLibrary"));
        assertTrue(selectorLayout.contains("@string/dialog_typeface_manage_action"));
        assertTrue(selectorLayout.contains("@string/dialog_typeface_done_action"));
        assertTrue(selectorLayout.contains("@dimen/dialog_surface_padding_horizontal"));
        assertTrue(selectorLayout.contains("android:id=\"@+id/typeface_scroll\""));
        assertTrue(selectorLayout.contains("android:layout_height=\"@dimen/dialog_typeface_list_height\""));
        assertTrue(source.contains("applyTypefaceDialogListHeight(root);"));
        assertTrue(source.contains("params.height = Math.min(configuredHeight, maxListHeight);"));
        assertTrue(selectorLayout.contains("@dimen/dialog_typeface_footer_button_height"));
        assertTrue(source.contains("R.dimen.dialog_typeface_option_min_height"));
        assertTrue(source.contains("R.dimen.dialog_typeface_option_padding_horizontal"));
        assertTrue(source.contains("doneButton.setOnClickListener"));
        assertFalse(source.contains("renameTypeface"));
        assertFalse(source.contains("confirmDeleteTypeface"));
        assertTrue(saveHandler.contains("setTargetTypefaceId"));
        assertTrue(saveHandler.contains("clearTargetTypefaceId"));
    }

    @Test
    public void binderTreatsSelectedTypefaceAsNativeProxyConfig() throws IOException {
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        int activeStart = source.indexOf("private static boolean hasActiveDialogConfig");
        int activeEnd = source.indexOf("private static void setSaveAndResetButtonsEnabled", activeStart);
        String activeBlock = source.substring(activeStart, activeEnd);

        assertTrue(activeBlock.contains("state.selectedTypefaceId"));
    }

    @Test
    public void typefaceSelectorKeepsMissingCurrentChoiceChecked() throws IOException {
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        int selectorStart = source.indexOf("void showTypefaceSelector");
        int selectorEnd = source.indexOf("private String resolveTypefaceDisplayText", selectorStart);
        String selectorBlock = source.substring(selectorStart, selectorEnd);

        assertTrue(source.contains("R.string.dialog_typeface_missing"));
        assertTrue(source.contains("containsSystemTypeface"));
        assertTrue(source.contains("containsImportedTypeface"));
        assertTrue(source.contains("option.matches(state.selectedTypefaceId)"));
    }

    @Test
    public void binder_validationWatcherUpdatesSaveStateAndStatus() throws IOException {
        String binderSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetModeValidationBinder.java")
                + read("src/main/java/com/dpis/module/appconfig/AppConfigSheetActionBinder.java");

        assertTrue(source.contains(
                "views.viewportInputView.addTextChangedListener(viewportValidationWatcher)"));
        assertTrue(source.contains(
                "views.fontInputView.addTextChangedListener(fontValidationWatcher)"));
        assertTrue(source.contains("AppConfigDialogBinder.updateSaveButtonState("));
        assertTrue(source.contains("refreshDialogState(views, state, style, systemHooksEnabled, item);"));
        assertTrue(binderSource.contains("AppStatusFormatter.formatCompact("));
        assertTrue(binderSource.contains("new AppStatusFormatter.StatusInput("));
        assertTrue(source.contains("state.selectedTypefaceId"));
        assertTrue(source.contains("showTypefaceSelector(views.typefaceSelectorButton, state,"));
        assertTrue(source.contains("views, state, style, systemHooksEnabled, item"));
    }

    @Test
    public void binderDoesNotShowHyperOsNativeProxyStatusTextInSheet() throws IOException {
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String layout = read("src/main/res/layout/dialog_app_config.xml");

        assertFalse(source.contains("bindHyperOsNativeWarning("));
        assertFalse(source.contains("resolveHyperOsNativeWarningText("));
        assertFalse(source.contains("HyperOsNativeProxyStatus.inspect(activity, item.packageName)"));
        assertFalse(layout.contains("hyperos_native_warning"));
    }

    @Test
    public void binderWiresFontHookDomainButtonToHost() throws IOException {
        String binderSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetActionBinder.java");
        String layout = read("src/main/res/layout/dialog_app_config.xml");

        assertTrue(layout.contains("android:id=\"@+id/dialog_font_hook_domains_button\""));
        assertTrue(layout.contains("@dimen/dialog_app_config_padding_horizontal"));
        assertTrue(layout.contains("@layout/view_sheet_unsaved_badge_handle"));
        assertTrue(layout.contains("@dimen/dialog_app_config_input_corner_radius"));
        assertTrue(layout.contains("@dimen/dialog_app_config_process_button_spacing_start"));
        assertTrue(layout.indexOf("android:id=\"@+id/dialog_font_hook_domains_button\"")
                < layout.indexOf("android:id=\"@+id/dialog_stop_button\""));
        assertTrue(layout.indexOf("android:id=\"@+id/dialog_font_hook_domains_button\"")
                < layout.indexOf("@string/dialog_advanced_section_title"));
        assertTrue(binderSource.contains("void showFontHookDomains(AppListItem item,"));
        assertTrue(binderSource.contains("AppConfigDialogState state,"));
        assertTrue(binderSource.contains("String getFontHookDomainsButtonText(AppListItem item,"));
        assertTrue(binderSource.contains("AppConfigDialogState state);"));
        assertTrue(source.contains("views.fontHookDomainsButton.setOnClickListener"));
        assertTrue(source.contains("host.showFontHookDomains(item, state,"));
        assertFalse(source.contains("currentFontConfigItem("));
        assertFalse(source.contains("withFontConfig("));
        assertTrue(binderSource.contains("host.getFontHookDomainsButtonText(item, state)"));
        assertTrue(source.contains("host.onDraftStateChanged(state);"));
        assertTrue(source.contains("state.draftFontHookDomainsRaw"));
    }

    @Test
    public void fontHookDomainDialogUsesImmediateEditorLayout() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/hookdomain/FontHookDomainDialog.java");
        String dialogLayout = read("src/main/res/layout/dialog_font_hook_domains.xml");
        String disabledHintBackground = read(
                "src/main/res/drawable/bg_font_hook_domains_disabled_hint.xml");
        String itemLayout = read("src/main/res/layout/item_font_hook_domain.xml");
        String viewportModeLayout = read("src/main/res/layout/item_viewport_apply_mode.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(source.contains("setTitle(R.string.dialog_font_hook_domains_dialog_title)"));
        assertTrue(source.contains("DialogWindowSizer.applyLargeWidth(dialog, activity)"));
        assertTrue(source.contains("dialog_hook_chain_tab_interface"));
        assertTrue(source.contains("dialog_hook_chain_tab_font"));
        assertTrue(source.contains("normalizeViewportApplyModeForDisplay(currentViewportApplyMode)"));
        assertTrue(source.contains(": ViewportApplyMode.AUTO"));
        assertTrue(source.contains("R.layout.item_viewport_apply_mode"));
        assertTrue(source.contains("MaterialRadioButton radioButton"));
        assertTrue(source.contains("font_hook_domains_interface_page"));
        assertTrue(source.contains("font_hook_domains_font_page"));
        assertTrue(source.contains("font_hook_domains_font_editable_content"));
        assertTrue(source.contains("boolean fontDomainsEditable"));
        assertTrue(source.contains("bindFontEditableContentEnabled("));
        assertTrue(source.contains("bindSelectedTabPage("));
        assertTrue(source.contains("setEnabledRecursive("));
        assertTrue(source.contains("host.saveCustom(packageName, selectedKnown, automaticKnown, unknown)"));
        assertTrue(source.contains("host.restoreRecommended(packageName)"));
        assertTrue(source.contains("FontHookDomainRegistry.orderedCustomizableDisplayIdsList()"));
        assertTrue(source.contains("FontHookDomainRegistry.orderedCustomizableDisplaySubset("));
        assertTrue(source.contains("createSubtitleText(activity, domainId)"));
        assertTrue(source.contains("resolveRiskDotColorRes(domainId)"));
        assertTrue(source.contains("font_hook_domain_risk_low"));
        assertTrue(source.contains("font_hook_domain_risk_medium"));
        assertTrue(source.contains("font_hook_domain_risk_high"));
        assertTrue(source.contains("font_hook_domain_warning"));
        assertTrue(source.contains("bindResourcesFontDefaultWarning("));
        assertTrue(source.contains("ID_RESOURCES_FONT.equals(domainId)"));
        assertFalse(source.contains("new LinkedHashSet<>(FontHookDomainRegistry.orderedIdsList())"));
        assertTrue(source.contains("title.setText(FontHookDomainRegistry.titleResFor(domainId));"));
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
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_font_editable_content"));
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_font_disabled_hint"));
        assertTrue(dialogLayout.contains("@string/dialog_font_hook_domains_font_disabled_hint"));
        assertTrue(dialogLayout.indexOf("@+id/font_hook_domains_font_disabled_hint")
                < dialogLayout.indexOf("@+id/font_hook_domains_interface_page"));
        assertTrue(dialogLayout.contains("android:layout_gravity=\"center_horizontal\""));
        assertTrue(dialogLayout.contains("@drawable/bg_font_hook_domains_disabled_hint"));
        assertTrue(dialogLayout.contains("@color/font_hook_domain_notice_text"));
        assertTrue(disabledHintBackground.contains(
                "@color/font_hook_domain_notice_container"));
        assertFalse(disabledHintBackground.contains("<stroke"));
        assertTrue(dialogLayout.contains("@dimen/font_hook_domains_dialog_padding_horizontal"));
        assertTrue(dialogLayout.contains("@dimen/font_hook_domains_tabs_spacing_bottom"));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_title"));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_subtitle"));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_warning"));
        assertTrue(itemLayout.contains("@string/dialog_font_hook_domain_resources_font_warning"));
        assertTrue(itemLayout.contains("@color/font_hook_domain_notice_text"));
        assertTrue(itemLayout.contains("android:visibility=\"gone\""));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_switch"));
        assertTrue(itemLayout.contains("@dimen/font_hook_domain_row_min_height"));
        assertTrue(itemLayout.contains("@dimen/font_hook_domain_row_padding_vertical"));
        assertTrue(viewportModeLayout.contains("@+id/viewport_apply_mode_radio"));
        assertTrue(viewportModeLayout.contains("com.google.android.material.radiobutton.MaterialRadioButton"));
        assertFalse(viewportModeLayout.contains("<FrameLayout"));
        assertTrue(viewportModeLayout.contains("android:layout_width=\"wrap_content\""));
        assertTrue(viewportModeLayout.contains("@dimen/viewport_apply_mode_row_min_height"));
        assertTrue(viewportModeLayout.contains("@dimen/viewport_apply_mode_text_spacing_end"));
        assertTrue(viewportModeLayout.contains("@dimen/viewport_apply_mode_radio_min_size"));
        assertTrue(viewportModeLayout.contains("@dimen/viewport_apply_mode_radio_padding"));
        assertTrue(zhStrings.contains("<string name=\"dialog_viewport_apply_auto\">&#x81EA;&#x52A8;</string>")
                || zhStrings.contains("<string name=\"dialog_viewport_apply_auto\">自动</string>"));
    }

    @Test
    public void viewportTargetTypeControlsInputHintAndStackedLabels() throws IOException {
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java")
                + read("src/main/java/com/dpis/module/appconfig/AppConfigSheetInteractions.java")
                + read("src/main/java/com/dpis/module/appconfig/AppConfigSheetModeValidationBinder.java");
        String layout = read("src/main/res/layout/dialog_app_config.xml");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(layout.contains("android:hint=\"@string/dialog_viewport_hint_scale\""));
        assertTrue(layout.contains("android:orientation=\"horizontal\""));
        assertTrue(layout.contains("android:id=\"@+id/dialog_viewport_mode_system_label\" android:layout_width=\"0dp\" android:layout_height=\"match_parent\""));
        assertTrue(layout.contains("android:id=\"@+id/dialog_viewport_mode_compat_label\" android:layout_width=\"0dp\" android:layout_height=\"match_parent\""));
        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, initialViewportType)"));
        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.RELATIVE_SCALE)"));
        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.ABSOLUTE_DP)"));
        assertTrue(source.contains("dialogView.findViewById(R.id.dialog_viewport_mode_compat_label)),"));
        assertFalse(source.contains("toggle.vertical"));
        assertTrue(strings.contains("Interface scale 30-300%"));
        assertTrue(strings.contains("Min width dp"));
        assertTrue(zhStrings.contains("&#x754C;&#x9762;&#x6BD4;&#x4F8B; 30-300%")
                || zhStrings.contains("界面比例 30-300%"));
        assertTrue(zhStrings.contains("&#x6700;&#x5C0F;&#x5BBD;&#x5EA6; dp")
                || zhStrings.contains("最小宽度 dp"));
    }

    @Test
    public void appConfigSheetDefaultsToScaleAndSystemFontMode() throws IOException {
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");

        assertTrue(source.contains("String initialViewportType = initialViewportTargetType(item)"));
        assertTrue(source.contains("bindViewportModeToggle(views.viewportModeToggle, initialViewportType, false)"));
        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, initialViewportType)"));
        assertTrue(source.contains("bindFontModeToggle(views.fontModeToggle, initialFontMode(item.fontMode), false)"));
        assertTrue(source.contains("private static String initialViewportTargetType(AppListItem item)"));
        assertTrue(source.contains("ViewportTargetType.normalize(item.viewportTargetType)"));
        assertTrue(source.contains("AppConfigInputValidation.initialFontMode(fontMode)"));
        assertTrue(source.contains("AppConfigInputValidation.initialViewportTargetType("));
    }

    @Test
    public void viewportModeSwitchKeepsSeparateInputValues() throws IOException {
        String binderSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String source = binderSource
                + read("src/main/java/com/dpis/module/appconfig/AppConfigSheetInteractions.java")
                + read("src/main/java/com/dpis/module/appconfig/AppConfigSheetModeValidationBinder.java");
        int switchStart = binderSource.indexOf("static void switchViewportTargetType");
        int visualStart = binderSource.indexOf("private static void updateModeToggleVisual", switchStart);
        String switchBlock = binderSource.substring(switchStart, visualStart);

        assertTrue(source.contains("String initialViewportInput = formatViewportInput(item.viewportTargetSpec)"));
        assertTrue(source.contains("state.updateViewportInput("));
        assertTrue(source.contains("AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle), s);"));
        int fontWatcherStart = source.indexOf("TextWatcher fontValidationWatcher");
        int fontWatcherEnd = source.indexOf(
                "views.viewportInputView.addTextChangedListener", fontWatcherStart);
        String fontWatcherBlock = source.substring(fontWatcherStart, fontWatcherEnd);
        assertFalse(fontWatcherBlock.contains("state.updateViewportInput("));
        assertTrue(source.contains("AppConfigDialogBinder.toggleViewportMode("));
        assertTrue(source.contains("views.viewportModeToggle, views.viewportInputView, state);"));
        assertTrue(switchBlock.contains("bindViewportModeToggle(viewportModeToggle, nextType, animate);"));
        assertTrue(switchBlock.contains("state.updateViewportInput(resolveViewportMode(viewportModeToggle),"));
        assertTrue(switchBlock.contains("viewportInputView.setText(state.viewportInputFor(nextType));"));
        assertTrue(source.contains("String viewportInputFor(String viewportTargetType)"));
        assertTrue(source.contains("void clearViewportInputs()"));
        assertFalse(source.contains("viewportScaleText"));
        assertFalse(source.contains("viewportAbsoluteText"));
    }

    @Test
    public void appConfigWizardHintUsesNamedDimensions() throws IOException {
        String coordinator = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogCoordinator.java");
        String appConfigLayout = read("src/main/res/layout/dialog_app_config.xml");
        String hintLayout = read("src/main/res/layout/view_app_config_wizard_hint.xml");
        String bubbleBackground = read("src/main/res/drawable/bg_app_config_wizard_bubble.xml");

        assertTrue(appConfigLayout.contains("@+id/dialog_advanced_wizard_hint_container"));
        assertTrue(appConfigLayout.contains("@layout/view_app_config_wizard_hint"));
        assertTrue(appConfigLayout.contains("<FrameLayout"));
        assertTrue(appConfigLayout.contains("android:layout_gravity=\"top|center_horizontal\""));
        assertTrue(appConfigLayout.contains("@dimen/dialog_app_config_wizard_hint_overlay_margin_top"));
        assertTrue(appConfigLayout.contains("@dimen/dialog_app_config_wizard_hint_overlay_elevation"));
        assertTrue(coordinator.contains("dialog_advanced_wizard_hint_container"));
        assertTrue(coordinator.contains("hint.setVisibility(View.VISIBLE)"));
        assertTrue(coordinator.contains("hint.setVisibility(View.GONE)"));
        assertFalse(coordinator.contains("positionAdvancedWizardHint"));
        assertFalse(coordinator.contains("overlayParent.addView"));
        assertFalse(coordinator.contains("R.layout.view_app_config_wizard_hint"));
        assertTrue(hintLayout.contains("@dimen/dialog_app_config_wizard_hint_min_height"));
        assertTrue(hintLayout.contains("@dimen/dialog_app_config_wizard_hint_padding_start"));
        assertTrue(hintLayout.contains("@dimen/dialog_app_config_wizard_hint_close_button_size"));
        assertTrue(hintLayout.contains("@dimen/dialog_app_config_wizard_hint_arrow_width"));
        assertTrue(hintLayout.contains("android:rotation=\"180\""));
        assertTrue(bubbleBackground.contains("@dimen/dialog_app_config_wizard_hint_corner_radius"));
        assertFalse(hintLayout.contains("\"28dp\""));
        assertFalse(hintLayout.contains("\"14dp\""));
    }

    @Test
    public void appConfigSheetUsesImeResizeAndScrollsFocusedInput() throws IOException {
        String coordinator = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogCoordinator.java");
        String appConfigLayout = read("src/main/res/layout/dialog_app_config.xml");

        assertTrue(appConfigLayout.contains("@+id/dialog_app_config_scroll"));
        assertTrue(coordinator.contains("SOFT_INPUT_ADJUST_RESIZE"));
        assertTrue(coordinator.contains("WindowInsetsCompat.Type.ime()"));
        assertTrue(coordinator.contains("setExpandedOffset(targetOffset)"));
        assertTrue(coordinator.contains("restoreImeSheetOffset(behavior, view)"));
        assertTrue(coordinator.contains("ValueAnimator.ofFloat(startTranslation, 0f)"));
        assertTrue(coordinator.contains("view.postDelayed(() ->"));
        assertTrue(coordinator.contains("if (!imeVisible)"));
        assertTrue(coordinator.contains("behavior.setFitToContents(false);"));
        assertTrue(coordinator.contains("behavior.setState(BottomSheetBehavior.STATE_EXPANDED);"));
        assertFalse(coordinator.contains("WindowInsetsAnimationCompat.Callback"));
        assertFalse(coordinator.contains("setMaxHeight("));
        assertFalse(coordinator.contains("DPIS-IME"));
        assertFalse(coordinator.contains("android.util.Log"));
        assertTrue(coordinator.contains("applyImeScrollPadding("));
        assertTrue(coordinator.contains("setClipToPadding(false)"));
        assertTrue(coordinator.contains("baseScrollPaddingBottom"));
        assertTrue(coordinator.contains("scrollView.setPadding("));
        assertTrue(coordinator.contains("smoothScrollBy(0, remainingDelta)"));
        assertFalse(coordinator.contains("applyImeHalfExpandedRatio("));
        assertFalse(coordinator.contains("restoreHalfExpandedRatio("));
        assertFalse(coordinator.contains("bottomSheet.getHeight() + imeBottom"));
        assertFalse(coordinator.contains("behavior.setHalfExpandedRatio(targetRatio);"));
        assertFalse(coordinator.contains("params.bottomMargin = imeBottom;"));
        assertFalse(coordinator.contains("expandedForIme"));
        assertTrue(coordinator.contains("scrollFocusedInputIntoView("));
        assertTrue(coordinator.contains("if (imeVisible)"));
        assertTrue(coordinator.contains("scrollFocusedInputAboveKeyboard("));
        assertTrue(coordinator.contains("focusedBottom + verticalPadding - keyboardTop"));
        assertTrue(coordinator.contains("inputVerticalPadding"));
        assertTrue(coordinator.contains("dialog_app_config_scroll"));
        assertTrue(coordinator.contains("smoothScrollTo("));
    }

    @Test
    public void appConfigSheetUsesSharedFormInputFocusBehavior() throws IOException {
        String interactions = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetInteractions.java");
        String validation = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetModeValidationBinder.java");
        String actions = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetActionBinder.java");
        String host = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String focusBinder = read("src/main/java/com/dpis/module/ui/FormInputFocusBinder.java");

        assertTrue(focusBinder.contains("public final class FormInputFocusBinder"));
        assertTrue(interactions.contains("new AppConfigSheetModeValidationBinder(binder, host)"));
        assertTrue(validation.contains("FormInputFocusBinder.bindDismissOnOutsideTouch"));
        assertTrue(validation.contains("WechatDpiSheetBinder.inputViewForFocus(dialogView)"));
        assertTrue(actions.contains("FormInputFocusBinder.clearFocusAndHideIme"));
        assertTrue(actions.contains("WechatDpiSheetBinder.inputViewForFocus(dialogView)"));
        assertFalse(host.contains("clearDialogInputFocus("));
    }

    @Test
    public void binderDoesNotKeepEmptyHyperOsSectionWrapper() throws IOException {
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");

        assertFalse(source.contains("bindHyperOsNativeSection"));
    }

    @Test
    public void simplifiedChineseHyperOsProxyFailureMessagesAreLocalized()
            throws IOException, ParserConfigurationException, SAXException {
        String strings = read("src/main/res/values-zh-rCN/strings.xml");
        String applyFailed = readStringValue(
                "src/main/res/values-zh-rCN/strings.xml",
                "dialog_hyperos_native_proxy_apply_failed");
        String unmountFailed = readStringValue(
                "src/main/res/values-zh-rCN/strings.xml",
                "dialog_hyperos_native_proxy_unmount_failed");

        assertFalse(strings.contains("HyperOS Native Proxy applied. Restart target app."));
        assertFalse(strings.contains("HyperOS Native Proxy apply failed. Check root and native directory."));
        assertTrue(applyFailed.contains("设置失败"));
        assertTrue(unmountFailed.contains("回滚失败"));
    }

    @Test
    public void resetButtonOnlyClearsDialogInputsUntilSaved() throws IOException {
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetActionBinder.java");
        int resetStart = source.indexOf("views.disableButton.setOnClickListener");
        int saveStart = source.indexOf("views.saveButton.setOnClickListener");
        String resetBlock = source.substring(resetStart, saveStart);

        assertFalse(resetBlock.contains("unmountHyperOsNativeProxy"));
    }

    @Test
    public void savingViewportConfigPublishesRuntimeViewportTarget() throws IOException {
        String saveSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.java");
        String mainSource = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(saveSource.contains("ViewportApplyMode.SYSTEM.equals"));
        assertTrue(mainSource.contains("scheduleRuntimePropertiesForTargetLaunch(packageName);"));
        assertTrue(mainSource.contains("ViewportPropertySyncer.syncTarget(packageName, store);"));
        assertTrue(mainSource.contains("finalizeAppConfigSaveWithRuntimeSync("));
        assertTrue(saveSource.contains("ViewportDraftValue.invalid()"));
        assertFalse(saveSource.contains("INVALID_DRAFT"));
        assertFalse(saveSource.contains("Integer.MIN_VALUE"));
    }

    @Test
    public void previewViewportApplyModeUsesMutableSheetStateForStatusAndSave() throws IOException {
        String binderSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String actionSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSheetActionBinder.java");
        String saveSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.java");

        assertTrue(binderSource.contains("String viewportApplyMode;"));
        assertTrue(binderSource.contains("this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);"));
        assertTrue(binderSource.contains("state.viewportApplyMode"));
        assertTrue(actionSource.contains("state.viewportApplyMode"));
        assertTrue(actionSource.contains("state.viewportApplyModeResetRequested"));
        assertTrue(saveSource.contains("String currentViewportApplyMode"));
        assertTrue(saveSource.contains("boolean viewportApplyModeResetRequested"));
        assertTrue(saveSource.contains("viewportApplyModeResetRequested, viewportTargetSpec"));
    }

    @Test
    public void savingEmptyFontScaleClearsOnlyFontScaleRuntimeTargets() throws IOException {
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.java");
        int clearStart = source.indexOf("if (fontScalePercent == null)");
        int configuredStart = source.indexOf("} else {", clearStart);
        String clearBlock = source.substring(clearStart, configuredStart);

        assertTrue(clearBlock.contains("ConfigDraftSaveSemantics.fontApplyModeForSave(fontMode)"));
        assertFalse(clearBlock.contains("FontRuntimePropertySyncer.clearFontScaleTargetAsync(item.packageName)"));
        assertFalse(clearBlock.contains("FontRuntimePropertySyncer.clearTargetAsync(item.packageName)"));
        assertFalse(clearBlock.contains("FontHookDomainPropertySyncer.clearTargetAsync(item.packageName)"));
    }

    @Test
    public void savingPreviewHookDomainsIsIndependentFromFontScaleBranch() throws IOException {
        String source = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.java");
        int persistCall = source.indexOf("persistPreviewOnlyConfig(");
        int fontScaleBranch = source.indexOf("if (fontScalePercent == null)");

        assertTrue(persistCall > 0);
        assertTrue(fontScaleBranch > persistCall);
        assertTrue(source.contains("publishFontHookDomainsAfterSave(item.packageName, store);"));
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
        String saveSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.java");
        String mainSource = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(mainSource.contains("scheduleRuntimePropertiesForTargetLaunch(packageName);"));
        assertTrue(mainSource.contains("FontRuntimePropertySyncer.syncTarget(packageName, store);"));
        assertTrue(mainSource.contains("finalizeAppConfigSaveWithRuntimeSync("));
        assertFalse(saveSource.contains("FontRuntimePropertySyncer.publishTargetAsync("));
        assertTrue(saveSource.contains("FontApplyMode.SYSTEM_EMULATION.equals"));
    }

    @Test
    public void savingTypefaceConfigPublishesRuntimeTypefaceTarget() throws IOException {
        String saveSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.java");
        String mainSource = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(mainSource.contains("scheduleRuntimePropertiesForTargetLaunch(packageName);"));
        assertTrue(mainSource.contains("FontRuntimePropertySyncer.syncTarget(packageName, store);"));
        assertFalse(saveSource.contains("FontRuntimePropertySyncer.publishTypefaceTargetAsync("));
    }

    @Test
    public void appConfigInputErrorsAreRenderedBySharedValidation() throws IOException {
        String binder = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String landBinder = read("src/main/java/com/dpis/module/appconfig/LandAppDetailPaneBinder.java");
        int updateStart = binder.indexOf("static boolean updateSaveButtonState(TextInputLayout");
        int updateEnd = binder.indexOf("static boolean updateSaveButtonState(View dialogView", updateStart);
        String updateBlock = binder.substring(updateStart, updateEnd);

        assertTrue(updateBlock.contains(
                "ConfigValueInputErrorBinder.bindFullMessage(viewportInputLayout, viewportValid);"));
        assertTrue(updateBlock.contains(
                "ConfigValueInputErrorBinder.bindFullMessage(fontInputLayout, fontValid);"));
        assertTrue(read("src/main/java/com/dpis/module/appconfig/ConfigValueInputErrorBinder.java")
                .contains("R.string.status_save_invalid"));
        assertFalse(landBinder.contains("R.string.status_save_invalid"));
    }

    @Test
    public void modeToggleThumbUsesHalfOfMeasuredTrackAfterRelayout() throws IOException {
        String binder = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogBinder.java");
        String dialogLayout = read("src/main/res/layout/dialog_app_config.xml");
        String templateLayout = read("src/main/res/layout/view_template_config_sheet_fields.xml");
        String landLayout = read("src/main/res/layout/view_land_app_detail.xml");

        assertTrue(binder.contains("private static int updateModeToggleThumbLayout(ModeToggle toggle)"));
        assertTrue(binder.contains("private static View modeToggleTrack(ModeToggle toggle)"));
        assertTrue(binder.contains("toggle.thumb.getParent() instanceof View"));
        assertTrue(binder.contains("int half = availableWidth / 2;"));
        assertTrue(binder.contains("params.width = half;"));
        assertTrue(binder.contains("container.getTag(R.id.mode_toggle_layout_listener)"));
        assertTrue(binder.contains("track.getViewTreeObserver().addOnGlobalLayoutListener(listener)"));
        assertTrue(binder.contains("modeUsesStartThumb(toggle) ? 0f : half"));

        assertThumbStartsAtZeroWidth(dialogLayout, "dialog_viewport_mode_toggle_thumb");
        assertThumbStartsAtZeroWidth(dialogLayout, "dialog_font_mode_toggle_thumb");
        assertThumbStartsAtZeroWidth(templateLayout, "template_config_viewport_mode_toggle_thumb");
        assertThumbStartsAtZeroWidth(templateLayout, "template_config_font_mode_toggle_thumb");
        assertThumbStartsAtZeroWidth(landLayout, "land_detail_viewport_mode_toggle_thumb");
        assertThumbStartsAtZeroWidth(landLayout, "land_detail_font_mode_toggle_thumb");
    }

    @Test
    public void landscapeDetailReassertsModeToggleSizeWhenRebound() throws IOException {
        String landBinder = read(
                "src/main/java/com/dpis/module/appconfig/LandAppDetailPaneBinder.java"
        );

        assertTrue(landBinder.contains("stabilizeModeToggleLayout(toggle.container);"));
        assertTrue(landBinder.contains("R.dimen.dialog_mode_toggle_width"));
        assertTrue(landBinder.contains("R.dimen.dialog_mode_toggle_row_height"));
        assertTrue(landBinder.contains("root.getViewTreeObserver().addOnGlobalLayoutListener("));
        assertTrue(landBinder.contains("primaryRow.getWidth() < threeButtonRequiredWidth"));
        assertTrue(landBinder.contains("container.post(() ->"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static String readStringValue(String relativePath, String name)
            throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        try (InputStream input = SourceSmokeTestPaths.open(relativePath)) {
            Document document = factory.newDocumentBuilder().parse(input);
            NodeList strings = document.getElementsByTagName("string");
            for (int i = 0; i < strings.getLength(); i++) {
                Element string = (Element) strings.item(i);
                if (name.equals(string.getAttribute("name"))) {
                    return string.getTextContent();
                }
            }
        }
        throw new IllegalArgumentException("Missing string resource: " + name);
    }

    private static void assertThumbStartsAtZeroWidth(String layout, String thumbId) {
        int thumbIndex = layout.indexOf("android:id=\"@+id/" + thumbId + "\"");
        assertTrue(thumbIndex >= 0);
        int viewStart = layout.lastIndexOf("<View", thumbIndex);
        int tagEnd = layout.indexOf(">", thumbIndex);
        assertTrue(viewStart >= 0 && tagEnd > viewStart);
        String declaration = layout.substring(viewStart, tagEnd);
        assertTrue(declaration.contains("android:layout_width=\"0dp\""));
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
