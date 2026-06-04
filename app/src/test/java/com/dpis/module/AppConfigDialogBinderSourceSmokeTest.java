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
        String binderSource = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String interactionsSource = read("src/main/java/com/dpis/module/AppConfigSheetInteractions.java");
        String source = read("src/main/java/com/dpis/module/AppConfigSheetActionBinder.java")
                + read("src/main/java/com/dpis/module/AppConfigSheetModeValidationBinder.java");

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
        assertTrue(source.contains("state.clearPreviewOnlyStateForReset();"));
        assertTrue(source.contains("AppConfigDialogBinder.bindViewportModeToggle("));
        assertTrue(source.contains("views.viewportModeToggle, ViewportTargetType.RELATIVE_SCALE, true)"));
        assertTrue(source.contains("AppConfigDialogBinder.bindFontModeToggle("));
        assertTrue(source.contains("views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true)"));
        assertTrue(source.contains("host.saveAppConfig("));
        assertTrue(source.contains("views.saveButton.setOnClickListener"));
        assertTrue(source.contains("if (!WechatTargetFieldSheetBinder.isInputValid(dialogView))"));
        assertTrue(source.contains("host.saveAppConfig("));
        assertTrue(source.contains("state.viewportScaleInput"));
        assertTrue(source.contains("state.viewportAbsoluteInput"));
        assertTrue(source.contains("state.previewFontHookDomainsRaw"));
        assertTrue(source.contains("showSaveButtonFeedback(views.saveButton)"));
        assertTrue(source.contains("binder.requestScopeAfterSuccessfulSave("));
        assertTrue(source.contains("dialogView, item, views, state, style, systemHooksEnabled);"));
        assertTrue(binderSource.contains("host.applyHyperOsNativeProxy(item, onFinished)"));
        assertTrue(binderSource.contains("host.unmountHyperOsNativeProxy(item"));
    }

    @Test
    public void wechatTargetFieldBlocksUnsupportedNonBlankInput() throws IOException {
        String binder = read("src/main/java/com/dpis/module/WechatTargetFieldSheetBinder.java");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(binder.contains("WechatTargetFieldSupport.current(dialogView.getContext())"));
        assertTrue(binder.contains("WechatTargetFieldSupport.current(inputLayout.getContext())"));
        assertTrue(binder.contains("if (!support.supported)"));
        assertTrue(binder.contains("return raw.isBlank();"));
        assertTrue(binder.contains("R.string.dialog_wechat_target_field_unsupported"));
        assertTrue(binder.contains("dialog_wechat_target_field_help_button"));
        assertTrue(binder.contains("HapticFeedbackConstants.VIRTUAL_KEY"));
        assertTrue(binder.contains("MaterialAlertDialogBuilder"));
        assertTrue(binder.contains("R.string.dialog_wechat_target_field_help_title"));
        assertTrue(binder.contains("R.string.dialog_wechat_target_field_help_message"));
        assertTrue(binder.contains("DialogWindowSizer.applyStandardWidth(dialog, anchor.getContext())"));
        assertTrue(binder.contains("setHelperText(supported ? null"));
        assertTrue(binder.contains("setError(inputLayout.getContext().getString("));
        assertTrue(strings.contains("WeChat independent route 200-1200"));
        assertTrue(strings.contains("WeChat-specific route"));
        assertTrue(strings.contains("Mini Programs are not supported yet."));
        assertTrue(strings.contains("dialog_wechat_target_field_unsupported"));
        assertTrue(zhStrings.contains("微信独立链路 200-1200"));
        assertTrue(zhStrings.contains("暂不支持小程序"));
        assertTrue(zhStrings.contains("未适配当前微信版本"));
    }

    @Test
    public void appConfigSheetUsesUnsavedBadgeInsteadOfPreviewIndicator() throws IOException {
        String binderSource = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String layout = read("src/main/res/layout/dialog_app_config.xml");

        assertTrue(layout.contains("@layout/view_sheet_unsaved_badge_handle"));
        assertFalse(layout.contains("dialog_preview_status"));
        assertFalse(layout.contains("dialog_global_prefill_preview_status"));
        assertTrue(binderSource.contains("state.captureSavedDraft(views, item != null && item.previewFromGlobalPrefill);"));
        assertTrue(binderSource.contains("SheetUnsavedBadgeBinder.bind("));
        assertTrue(binderSource.contains("bindDpisToggleButton(views.dpisToggleButton, state.dpisEnabled"));
        assertTrue(binderSource.contains("dpisToggleButton.setEnabled(!previewFromGlobalPrefill);"));
        assertTrue(binderSource.contains("dpisToggleButton.setAlpha(previewFromGlobalPrefill ? 0.6f : 1f);"));
    }

    @Test
    public void previewDpisToggleReturnsBeforeWritingPackageConfig() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigSheetActionBinder.java");
        int toggleStart = source.indexOf("views.dpisToggleButton.setOnClickListener");
        int toggleEnd = source.indexOf("views.fontHookDomainsButton.setOnClickListener", toggleStart);
        String toggleBlock = source.substring(toggleStart, toggleEnd);

        int previewGuard = toggleBlock.indexOf("if (state.previewFromGlobalPrefill)");
        int storeWrite = toggleBlock.indexOf("host.setDpisEnabled(item.packageName, nextEnabled)");
        assertTrue(previewGuard > 0);
        assertTrue(storeWrite > previewGuard);
        assertTrue(toggleBlock.indexOf("return;", previewGuard) < storeWrite);
    }

    @Test
    public void saveSuccessRequestsKnownMissingScopeOnce() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
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
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
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
        assertTrue(source.contains("DialogWindowSizer.applyLargeWidth(dialogHolder[0], activity)"));
        assertTrue(source.contains("R.string.dialog_typeface_tab_system"));
        assertTrue(source.contains("R.string.dialog_typeface_tab_imported"));
        assertTrue(source.contains("host.openTypefaceLibrary"));
        assertTrue(selectorLayout.contains("@string/dialog_typeface_manage_action"));
        assertTrue(selectorLayout.contains("@string/dialog_typeface_done_action"));
        assertTrue(selectorLayout.contains("@dimen/dialog_surface_padding_horizontal"));
        assertTrue(selectorLayout.contains("@dimen/dialog_typeface_list_height"));
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
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        int activeStart = source.indexOf("private static boolean hasActiveDialogConfig");
        int activeEnd = source.indexOf("private static void setSaveAndResetButtonsEnabled", activeStart);
        String activeBlock = source.substring(activeStart, activeEnd);

        assertTrue(activeBlock.contains("state.selectedTypefaceId"));
    }

    @Test
    public void typefaceSelectorKeepsMissingCurrentChoiceChecked() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
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
        String binderSource = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String source = read("src/main/java/com/dpis/module/AppConfigSheetModeValidationBinder.java")
                + read("src/main/java/com/dpis/module/AppConfigSheetActionBinder.java");

        assertTrue(source.contains(
                "views.viewportInputView.addTextChangedListener(viewportValidationWatcher)"));
        assertTrue(source.contains(
                "views.fontInputView.addTextChangedListener(fontValidationWatcher)"));
        assertTrue(source.contains("AppConfigDialogBinder.updateSaveButtonState("));
        assertTrue(source.contains("refreshDialogState(views, state, style, systemHooksEnabled, item);"));
        assertTrue(binderSource.contains("AppStatusFormatter.formatCompact("));
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
        String binderSource = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String source = read("src/main/java/com/dpis/module/AppConfigSheetActionBinder.java");
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
        assertTrue(binderSource.contains("boolean previewFromGlobalPrefill,"));
        assertTrue(binderSource.contains("String previewFontHookDomainsRaw);"));
        assertTrue(source.contains("views.fontHookDomainsButton.setOnClickListener"));
        assertTrue(source.contains("host.showFontHookDomains(item, state,"));
        assertTrue(binderSource.contains("item, previewFromGlobalPrefill, previewFontHookDomainsRaw"));
        assertTrue(source.contains("state.previewFromGlobalPrefill"));
        assertTrue(source.contains("state.previewFontHookDomainsRaw"));
    }

    @Test
    public void fontHookDomainDialogUsesImmediateEditorLayout() throws IOException {
        String source = read("src/main/java/com/dpis/module/FontHookDomainDialog.java");
        String dialogLayout = read("src/main/res/layout/dialog_font_hook_domains.xml");
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
        assertTrue(dialogLayout.contains("@dimen/font_hook_domains_dialog_padding_horizontal"));
        assertTrue(dialogLayout.contains("@dimen/font_hook_domains_tabs_spacing_bottom"));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_title"));
        assertTrue(itemLayout.contains("@+id/font_hook_domain_subtitle"));
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
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java")
                + read("src/main/java/com/dpis/module/AppConfigSheetInteractions.java")
                + read("src/main/java/com/dpis/module/AppConfigSheetModeValidationBinder.java");
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
        String source = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");

        assertTrue(source.contains("String initialViewportType = initialViewportTargetType(item.viewportTargetSpec)"));
        assertTrue(source.contains("bindViewportModeToggle(views.viewportModeToggle, initialViewportType, false)"));
        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, initialViewportType)"));
        assertTrue(source.contains("bindFontModeToggle(views.fontModeToggle, initialFontMode(item.fontMode), false)"));
        assertTrue(source.contains("private static String initialViewportTargetType(ViewportTargetSpec spec)"));
        assertTrue(source.contains("AppConfigInputValidation.initialFontMode(fontMode)"));
        assertTrue(source.contains("AppConfigInputValidation.initialViewportTargetType(spec)"));
    }

    @Test
    public void viewportModeSwitchKeepsSeparateInputValues() throws IOException {
        String binderSource = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String source = binderSource
                + read("src/main/java/com/dpis/module/AppConfigSheetInteractions.java")
                + read("src/main/java/com/dpis/module/AppConfigSheetModeValidationBinder.java");
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
        String coordinator = read("src/main/java/com/dpis/module/AppConfigDialogCoordinator.java");
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
        String source = read("src/main/java/com/dpis/module/AppConfigSheetActionBinder.java");
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
        assertTrue(source.contains("ViewportDraftValue.invalid()"));
        assertFalse(source.contains("INVALID_DRAFT"));
        assertFalse(source.contains("Integer.MIN_VALUE"));
    }

    @Test
    public void previewViewportApplyModeUsesMutableSheetStateForStatusAndSave() throws IOException {
        String binderSource = read("src/main/java/com/dpis/module/AppConfigDialogBinder.java");
        String actionSource = read("src/main/java/com/dpis/module/AppConfigSheetActionBinder.java");
        String saveSource = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");

        assertTrue(binderSource.contains("String viewportApplyMode;"));
        assertTrue(binderSource.contains("this.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode);"));
        assertTrue(binderSource.contains("state.viewportApplyMode"));
        assertTrue(actionSource.contains("state.viewportApplyMode"));
        assertTrue(saveSource.contains("String currentViewportApplyMode"));
        assertTrue(saveSource.contains("store, item.packageName, currentViewportApplyMode, viewportTargetSpec"));
    }

    @Test
    public void savingEmptyFontScaleClearsOnlyFontScaleRuntimeTargets() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");
        int clearStart = source.indexOf("if (fontScalePercent == null)");
        int configuredStart = source.indexOf("} else {", clearStart);
        String clearBlock = source.substring(clearStart, configuredStart);

        assertTrue(clearBlock.contains("FontRuntimePropertySyncer.clearFontScaleTargetAsync(item.packageName)"));
        assertFalse(clearBlock.contains("FontRuntimePropertySyncer.clearTargetAsync(item.packageName)"));
        assertFalse(clearBlock.contains("FontHookDomainPropertySyncer.clearTargetAsync(item.packageName)"));
    }

    @Test
    public void savingPreviewHookDomainsIsIndependentFromFontScaleBranch() throws IOException {
        String source = read("src/main/java/com/dpis/module/AppConfigSaveHandler.java");
        int persistCall = source.indexOf("persistPreviewOnlyConfig(store, item, previewFontHookDomainsRaw)");
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
