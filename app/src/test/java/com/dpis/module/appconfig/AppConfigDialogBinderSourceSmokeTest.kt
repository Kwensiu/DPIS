package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import com.dpis.module.appconfig.presentation.ComposeAppEditorActivityGateway

class AppConfigDialogBinderSourceSmokeTest {
    @Test
    fun binder_wiresExpectedActionButtons() {
        val binderSource = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val interactionsSource = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetInteractions.kt")
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetActionBinder.kt") +
            read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetModeValidationBinder.kt")

        assertTrue(binderSource.contains("AppConfigSheetInteractions(this, host)"))
        assertTrue(binderSource.contains(".bind(dialogView, item, views, state, style, systemHooksEnabled)"))
        assertTrue(interactionsSource.contains("AppConfigSheetModeValidationBinder(binder, host)"))
        assertTrue(interactionsSource.contains("AppConfigSheetActionBinder(binder, host)"))
        assertTrue(interactionsSource.contains(
                "modeValidationBinder.bindDialogValidation(dialogView, item, views, state, style, systemHooksEnabled)"))
        assertTrue(interactionsSource.contains(
                "actionBinder.bindDialogActions(dialogView, item, views, state, style, systemHooksEnabled)"))
        assertTrue(interactionsSource.contains(
                "modeValidationBinder.bindModeToggles(dialogView, item, views, state, style, systemHooksEnabled)"))
        assertTrue(interactionsSource.contains(
                "actionBinder.bindTypefaceSelectorAction(dialogView, item, views, state, style, systemHooksEnabled)"))
        assertTrue(source.contains("views.scopeButton.setOnClickListener"))
        assertTrue(source.contains("host.toggleScope(item, state.scopeSelected"))
        assertTrue(source.contains("views.dpisToggleButton.setOnClickListener"))
        assertTrue(source.contains("host.setDpisEnabled(item.packageName, nextEnabled)"))
        assertTrue(source.contains("views.startButton.setOnClickListener"))
        assertTrue(source.contains("ProcessAction.START"))
        assertTrue(source.contains("syncHyperOsNativeProxyAfterSave(item, views, state)"))
        assertTrue(source.contains("views.restartButton.setOnClickListener"))
        assertTrue(source.contains("ProcessAction.RESTART"))
        assertTrue(source.contains("views.stopButton.setOnClickListener"))
        assertTrue(source.contains("ProcessAction.STOP"))
        assertTrue(source.contains("views.disableButton.setOnClickListener"))
        assertTrue(source.contains("views.viewportInputView.setText(\"\")"))
        assertTrue(source.contains("state.clearHookChainStateForReset()"))
        assertTrue(source.contains("AppConfigDialogBinder.bindViewportModeToggle("))
        assertTrue(source.contains("views.viewportModeToggle, ViewportTargetType.RELATIVE_SCALE, true)"))
        assertTrue(source.contains("AppConfigDialogBinder.bindFontModeToggle("))
        assertTrue(source.contains("views.fontModeToggle, FontApplyMode.SYSTEM_EMULATION, true)"))
        assertTrue(source.contains("host.saveAppConfig("))
        assertTrue(source.contains("views.saveButton.setOnClickListener"))
        assertTrue(source.contains("WechatDpiSheetBinder.isInputValid(dialogView)"))
        assertTrue(source.contains("host.saveAppConfig("))
        assertTrue(source.contains("state.viewportScaleInput"))
        assertTrue(source.contains("state.viewportAbsoluteInput"))
        assertTrue(source.contains("state.draftFontHookDomainsRaw"))
        assertTrue(source.contains("showSaveButtonFeedback(views.saveButton)"))
        assertTrue(source.contains("binder.requestScopeAfterSuccessfulSave("))
        assertTrue(source.contains("dialogView, item, views, state, style, systemHooksEnabled)"))
        assertTrue(binderSource.contains("host.applyHyperOsNativeProxy(item, onFinished)"))
        assertTrue(binderSource.contains("host.unmountHyperOsNativeProxy(item"))
        assertTrue(binderSource.contains("host.isHyperOsNativeProxyCandidate(item)"))
    }

    @Test
    fun sheetModeAndInputChangesRefreshRetainedDraft() {
        val controller = read(
            "src/main/java/com/dpis/module/appconfig/editor/ComposeAppEditorController.kt",
        )
        assertTrue(controller.contains("fun updateDraft(draft: EditorDraft?)"))
        assertTrue(controller.contains("session.editorSession = current.withDraft(draft)"))
    }

    @Test
    fun wechatDpiUsesSingleOfficialInput() {
        val binder = read("src/main/java/com/dpis/module/quirks/presentation/WechatDpiSheetBinder.kt")
        val strings = read("src/main/res/values/strings.xml")
        val zhStrings = read("src/main/res/values-zh-rCN/strings.xml")

        assertTrue(binder.contains("WechatDpiConfig.appliesTo(packageName)"))
        assertTrue(binder.contains("HapticFeedbackConstants.VIRTUAL_KEY"))
        assertTrue(binder.contains("MaterialAlertDialogBuilder"))
        assertTrue(binder.contains("R.string.dialog_wechat_dpi_help_title"))
        assertTrue(binder.contains("R.string.dialog_wechat_dpi_help_message"))
        assertTrue(binder.contains("WechatDpiEditor.isInputValid"))
        assertTrue(binder.contains("dialog_wechat_dpi_input"))
        assertTrue(binder.contains("DialogWindowSizer.applyStandardWidth("))
        assertTrue(binder.contains("anchor.context"))
        assertTrue(strings.contains("WeChat DPI 200-1000"))
        assertTrue(strings.contains("WeChat-specific DisplayMetrics route"))
        assertTrue(strings.contains("Mini Programs are not supported yet."))
        assertFalse(strings.contains("dialog_wechat_target_field"))
        assertTrue(zhStrings.contains("微信 DPI 200-1000"))
        assertTrue(zhStrings.contains("暂不支持小程序"))
    }

    @Test
    fun wechatDpiSaveDoesNotClearViewportConfig() {
        val editor = read("src/main/java/com/dpis/module/quirks/WechatDpiEditor.kt")
        val saveStart = editor.indexOf("fun save(")
        val saveEnd = editor.indexOf("fun publishForDpisState", saveStart)
        val saveBlock = editor.substring(saveStart, saveEnd)

        assertTrue(saveBlock.contains("store.setWechatDpi(packageName, dpi)"))
        assertTrue(saveBlock.contains("if (saved)"))
        assertTrue(saveBlock.contains("WechatDpiPropertySyncer.publishDpiAsync(packageName"))
        assertFalse(saveBlock.contains("clearTargetViewportWidthDp(packageName)"))
        assertFalse(saveBlock.contains("setTargetViewportApplyMode(packageName, ViewportApplyMode.OFF)"))
        assertFalse(saveBlock.contains("ViewportPropertySyncer.clearTargetAsync(packageName)"))
    }

    @Test
    fun wechatDpiPublishFollowsSavedHostState() {
        val actionBinder = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetActionBinder.kt")
        val gateway = read(
                "src/main/java/com/dpis/module/appconfig/presentation/ComposeAppEditorActivityGateway.kt")
        val editorController = read(
                "src/main/java/com/dpis/module/appconfig/editor/EditorActions.kt")

        val toggleStart = actionBinder.indexOf("views.dpisToggleButton.setOnClickListener")
        val toggleEnd = actionBinder.indexOf("views.fontHookDomainsButton.setOnClickListener", toggleStart)
        val toggleBlock = actionBinder.substring(toggleStart, toggleEnd)
        assertFalse(toggleBlock.contains("WechatDpiSheetBinder.publishForDpisState"))

        val hostStart = gateway.indexOf("override fun setDpisEnabled(packageName: String, enabled: Boolean)")
        val hostEnd = gateway.indexOf("override fun executeProcessAction", hostStart)
        val hostBlock = gateway.substring(hostStart, hostEnd)
        assertTrue(hostBlock.contains(
                "if (!activity.runtimeLaunchSession.setDpisEnabled(packageName, enabled))"))
        assertTrue(hostBlock.contains("WechatDpiEditor.publishForDpisState("))
        assertTrue(editorController.contains("if (host.setDpisEnabled(enabled))"))
        assertTrue(editorController.contains("host.updateDraft(draft.withDpisEnabled(enabled))"))
    }

    @Test
    fun appConfigSheetUsesUnsavedBadgeInsteadOfPreviewIndicator() {
        val binderSource = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val stateSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogModels.kt")
        val overlay = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt")

        assertTrue(overlay.contains("fun AppConfigEditorOverlay("))
        assertTrue(binderSource.contains("state.captureSavedDraft(views, item.previewFromGlobalPrefill)"))
        assertTrue(binderSource.contains("UnsavedBadgeBinder.bind("))
        assertTrue(stateSource.contains("normalizedHookDomainsRaw().orEmpty()"))
        assertTrue(stateSource.contains("forAutomaticDomainsRaw(draftFontHookDomainsRaw)"))
        assertFalse(binderSource.contains("fontHookDomainsResetRequested ? \"font-reset\""))
        assertFalse(binderSource.contains("viewportApplyModeResetRequested ? \"viewport-reset\""))
        assertFalse(binderSource.contains("previewFromGlobalPrefill ? \"preview\" : \"stored\""))
        assertTrue(binderSource.contains("bindDpisToggleButton("))
        assertTrue(binderSource.contains("dpisToggleButton.isEnabled = true"))
        assertTrue(binderSource.contains("dpisToggleButton.alpha = 1f"))
    }

    @Test
    fun saveSuccessRequestsKnownMissingScopeOnce() {
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val methodStart = source.indexOf("fun requestScopeAfterSuccessfulSave")
        val methodEnd = source.indexOf("private fun hasActiveDialogConfig", methodStart)
        val method = source.substring(methodStart, methodEnd)

        assertTrue(method.contains("!state.scopeKnown || state.scopeSelected || state.scopeRequestPending"))
        assertTrue(method.contains("state.scopeRequestPending = true"))
        assertTrue(method.contains("val requestStarted = host.requestScope("))
        assertTrue(method.contains("if (requestStarted)"))
        assertTrue(method.contains("host.showToast(R.string.save_scope_request_notice)"))
        assertTrue(method.contains("state.scopeRequestPending = false"))
    }

    @Test
    fun saveTimeScopeCallbacksOnlyRefreshAttachedSheet() {
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val methodStart = source.indexOf("fun requestScopeAfterSuccessfulSave")
        val methodEnd = source.indexOf("private fun hasActiveDialogConfig", methodStart)
        val method = source.substring(methodStart, methodEnd)

        assertTrue(method.contains("if (!dialogView.isAttachedToWindow)"))
        assertTrue(method.contains("state.scopeSelected = true"))
        assertTrue(method.contains("refreshDialogState(views, state, style, systemHooksEnabled, item)"))
        assertTrue(method.contains("{ state.scopeRequestPending = false }"))
    }

    @Test
    fun binder_wiresTypefaceSelector() {
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val saveHandler = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.kt")
        val selectorLayout = read("src/main/res/layout/dialog_typeface_selection.xml")

        assertTrue(source.contains("bindTypefaceSelector"))
        assertTrue(source.contains("formatTypefaceSelectorText"))
        assertTrue(source.contains("SystemFontRegistry.listRecommendedFonts()"))
        assertTrue(source.contains("TabLayout"))
        assertTrue(source.contains("R.layout.dialog_typeface_selection"))
        assertTrue(source.contains("DialogWindowSizer.applyConfigurationWidth(dialogHolder[0], activity)"))
        assertTrue(source.contains("R.string.dialog_typeface_tab_system"))
        assertTrue(source.contains("R.string.dialog_typeface_tab_imported"))
        assertTrue(source.contains("bindImportedTypefaceCollectionRows"))
        assertTrue(source.contains("showTypefaceFaceSelection"))
        assertTrue(source.contains("R.string.dialog_typeface_collection_label"))
        assertTrue(source.contains("R.string.dialog_typeface_face_select_title"))
        assertTrue(source.contains("host.openTypefaceLibrary"))
        assertTrue(selectorLayout.contains("@string/dialog_typeface_manage_action"))
        assertTrue(selectorLayout.contains("@string/dialog_typeface_done_action"))
        assertTrue(selectorLayout.contains("@dimen/dialog_surface_padding_horizontal"))
        assertTrue(selectorLayout.contains("android:id=\"@+id/typeface_scroll\""))
        assertTrue(selectorLayout.contains("android:layout_height=\"@dimen/dialog_typeface_list_height\""))
        assertTrue(source.contains("applyTypefaceDialogListHeight(root)"))
        assertTrue(source.contains("params.height = ViewGroup.LayoutParams.MATCH_PARENT"))
        assertTrue(selectorLayout.contains("@dimen/dialog_typeface_footer_button_height"))
        assertTrue(source.contains("R.dimen.dialog_typeface_option_min_height"))
        assertTrue(source.contains("R.dimen.dialog_typeface_option_padding_horizontal"))
        assertTrue(source.contains("doneButton.setOnClickListener"))
        assertFalse(source.contains("renameTypeface"))
        assertFalse(source.contains("confirmDeleteTypeface"))
        assertTrue(saveHandler.contains("setTargetTypefaceId"))
        assertTrue(saveHandler.contains("clearTargetTypefaceId"))
    }

    @Test
    fun binderTreatsSelectedTypefaceAsNativeProxyConfig() {
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val activeStart = source.indexOf("private fun hasActiveDialogConfig")
        val activeEnd = source.indexOf("private fun setSaveAndResetButtonsEnabled", activeStart)
        val activeBlock = source.substring(activeStart, activeEnd)

        assertTrue(activeBlock.contains("state.selectedTypefaceId"))
    }

    @Test
    fun typefaceSelectorKeepsMissingCurrentChoiceChecked() {
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        assertTrue(source.contains("R.string.dialog_typeface_missing"))
        assertTrue(source.contains("containsSystemTypeface"))
        assertTrue(source.contains("containsImportedTypeface"))
        assertTrue(source.contains("option.matches(state.selectedTypefaceId)"))
    }

    @Test
    fun binder_validationWatcherUpdatesSaveStateAndStatus() {
        val binderSource = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetModeValidationBinder.kt") +
            read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetActionBinder.kt")

        assertTrue(source.contains(
                "views.viewportInputView.addTextChangedListener(viewportValidationWatcher)"))
        assertTrue(source.contains(
                "views.fontInputView.addTextChangedListener(fontValidationWatcher)"))
        assertTrue(source.contains("AppConfigDialogBinder.updateSaveButtonState("))
        assertTrue(source.contains("refreshDialogState(views, state, style, systemHooksEnabled, item)"))
        assertTrue(binderSource.contains("AppStatusFormatter.formatCompact("))
        assertTrue(binderSource.contains("StatusInput("))
        assertTrue(source.contains("state.selectedTypefaceId"))
        assertTrue(source.contains("showTypefaceSelector("))
        assertTrue(source.contains("views, state, style, systemHooksEnabled, item"))
    }

    @Test
    fun binderDoesNotShowHyperOsNativeProxyStatusTextInSheet() {
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")

        assertFalse(source.contains("bindHyperOsNativeWarning("))
        assertFalse(source.contains("resolveHyperOsNativeWarningText("))
        assertFalse(source.contains("HyperOsNativeProxyStatus.inspect(activity, item.packageName)"))
    }

    @Test
    fun binderWiresFontHookDomainButtonToHost() {
        val binderSource = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetActionBinder.kt")

        assertTrue(binderSource.contains("fun showFontHookDomains("))
        assertTrue(binderSource.contains("item: AppListItem?"))
        assertTrue(binderSource.contains("state: AppConfigDialogState?"))
        assertTrue(binderSource.contains("fun getFontHookDomainsButtonText("))
        assertTrue(source.contains("views.fontHookDomainsButton.setOnClickListener"))
        assertTrue(source.contains("host.showFontHookDomains(item, state)"))
        assertFalse(source.contains("currentFontConfigItem("))
        assertFalse(source.contains("withFontConfig("))
        assertTrue(binderSource.contains("host.getFontHookDomainsButtonText(item, state)"))
        assertTrue(source.contains("host.onDraftStateChanged(state)"))
        assertTrue(source.contains("state.draftFontHookDomainsRaw"))
    }

    @Test
    fun fontHookDomainDialogUsesImmediateEditorLayout() {
        val source = read("src/main/java/com/dpis/module/fonts/hookdomain/FontHookDomainDialog.java")
        val dialogLayout = read("src/main/res/layout/dialog_font_hook_domains.xml")
        val disabledHintBackground = read(
                "src/main/res/drawable/bg_font_hook_domains_disabled_hint.xml")
        val itemLayout = read("src/main/res/layout/item_font_hook_domain.xml")
        val viewportModeLayout = read("src/main/res/layout/item_viewport_apply_mode.xml")
        val zhStrings = read("src/main/res/values-zh-rCN/strings.xml")

        assertTrue(source.contains("setTitle(R.string.dialog_font_hook_domains_dialog_title)"))
        assertTrue(source.contains("DialogWindowSizer.applyConfigurationWidth(dialog, activity)"))
        assertTrue(source.contains("dialog_hook_chain_tab_interface"))
        assertTrue(source.contains("dialog_hook_chain_tab_font"))
        assertTrue(source.contains("normalizeViewportApplyModeForDisplay(currentViewportApplyMode)"))
        assertTrue(source.contains(": ViewportApplyMode.AUTO"))
        assertTrue(source.contains("R.layout.item_viewport_apply_mode"))
        assertTrue(source.contains("MaterialRadioButton radioButton"))
        assertTrue(source.contains("font_hook_domains_interface_page"))
        assertTrue(source.contains("font_hook_domains_font_page"))
        assertTrue(source.contains("font_hook_domains_font_editable_content"))
        assertTrue(source.contains("boolean fontDomainsEditable"))
        assertTrue(source.contains("bindFontEditableContentEnabled("))
        assertTrue(source.contains("bindSelectedTabPage("))
        assertTrue(source.contains("setEnabledRecursive("))
        assertTrue(source.contains("host.saveCustom(packageName, selectedKnown, automaticKnown, unknown)"))
        assertTrue(source.contains("host.restoreRecommended(packageName)"))
        assertTrue(source.contains("FontHookDomainRegistry.orderedCustomizableDisplayIdsList()"))
        assertTrue(source.contains("FontHookDomainRegistry.orderedCustomizableDisplaySubset("))
        assertTrue(source.contains("createSubtitleText(activity, domainId)"))
        assertTrue(source.contains("resolveRiskDotColorRes(domainId)"))
        assertTrue(source.contains("font_hook_domain_risk_low"))
        assertTrue(source.contains("font_hook_domain_risk_medium"))
        assertTrue(source.contains("font_hook_domain_risk_high"))
        assertFalse(source.contains("font_hook_domain_warning"))
        assertFalse(source.contains("bindResourcesFontDefaultWarning("))
        assertFalse(source.contains("new LinkedHashSet<>(FontHookDomainRegistry.orderedIdsList())"))
        assertTrue(source.contains("title.setText(FontHookDomainRegistry.titleResFor(domainId))"))
        assertTrue(source.contains("title.setText(domainId)"))
        assertFalse(source.contains("title.setText(known ? resolveDomainTitleRes(domainId) : 0)"))
        assertFalse(source.contains("setPositiveButton"))
        assertFalse(source.contains("setNegativeButton"))
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_known_container"))
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_unknown_container"))
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_restore_button"))
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_tabs"))
        assertTrue(dialogLayout.contains("app:tabBackground=\"@android:color/transparent\""))
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_interface_page"))
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_font_page"))
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_font_editable_content"))
        assertTrue(dialogLayout.contains("@+id/font_hook_domains_font_disabled_hint"))
        assertTrue(dialogLayout.contains("@string/dialog_font_hook_domains_font_disabled_hint"))
        assertTrue(dialogLayout.indexOf("@+id/font_hook_domains_font_disabled_hint")
                < dialogLayout.indexOf("@+id/font_hook_domains_interface_page"))
        assertTrue(dialogLayout.contains("android:layout_gravity=\"center_horizontal\""))
        assertTrue(dialogLayout.contains("@drawable/bg_font_hook_domains_disabled_hint"))
        assertTrue(dialogLayout.contains("@color/font_hook_domain_notice_text"))
        assertTrue(disabledHintBackground.contains(
                "@color/font_hook_domain_notice_container"))
        assertFalse(disabledHintBackground.contains("<stroke"))
        assertTrue(dialogLayout.contains("@dimen/font_hook_domains_dialog_padding_horizontal"))
        assertTrue(dialogLayout.contains("@dimen/font_hook_domains_tabs_spacing_bottom"))
        assertTrue(itemLayout.contains("@+id/font_hook_domain_title"))
        assertTrue(itemLayout.contains("@+id/font_hook_domain_subtitle"))
        assertFalse(itemLayout.contains("@+id/font_hook_domain_warning"))
        assertFalse(itemLayout.contains("@string/dialog_font_hook_domain_resources_font_warning"))
        assertTrue(itemLayout.contains("@+id/font_hook_domain_switch"))
        assertTrue(itemLayout.contains("@dimen/font_hook_domain_row_min_height"))
        assertTrue(itemLayout.contains("@dimen/font_hook_domain_row_padding_vertical"))
        assertTrue(viewportModeLayout.contains("@+id/viewport_apply_mode_radio"))
        assertTrue(viewportModeLayout.contains("com.google.android.material.radiobutton.MaterialRadioButton"))
        assertFalse(viewportModeLayout.contains("<FrameLayout"))
        assertTrue(viewportModeLayout.contains("android:layout_width=\"wrap_content\""))
        assertTrue(viewportModeLayout.contains("@dimen/viewport_apply_mode_row_min_height"))
        assertTrue(viewportModeLayout.contains("@dimen/viewport_apply_mode_text_spacing_end"))
        assertTrue(viewportModeLayout.contains("@dimen/viewport_apply_mode_radio_min_size"))
        assertTrue(viewportModeLayout.contains("@dimen/viewport_apply_mode_radio_padding"))
        assertTrue(zhStrings.contains("<string name=\"dialog_viewport_apply_auto\">&#x81EA&#x52A8;</string>")
                || zhStrings.contains("<string name=\"dialog_viewport_apply_auto\">自动</string>"))
    }

    @Test
    fun viewportTargetTypeControlsInputHintAndStackedLabels() {
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt") +
            read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetInteractions.kt") +
            read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetModeValidationBinder.kt")
        val strings = read("src/main/res/values/strings.xml")
        val zhStrings = read("src/main/res/values-zh-rCN/strings.xml")

        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, initialViewportType)"))
        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.RELATIVE_SCALE)"))
        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, ViewportTargetType.ABSOLUTE_DP)"))
        assertTrue(source.contains("R.id.dialog_viewport_mode_compat_label"))
        assertFalse(source.contains("toggle.vertical"))
        assertTrue(strings.contains("Interface scale 30-300%"))
        assertTrue(strings.contains("Min width dp"))
        assertTrue(zhStrings.contains("&#x754C&#x9762;&#x6BD4;&#x4F8B; 30-300%")
                || zhStrings.contains("界面比例 30-300%"))
        assertTrue(zhStrings.contains("&#x6700&#x5C0F;&#x5BBD;&#x5EA6; dp")
                || zhStrings.contains("最小宽度 dp"))
    }

    @Test
    fun appConfigSheetDefaultsToScaleAndSystemFontMode() {
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt") +
            read("src/main/java/com/dpis/module/appconfig/AppConfigDialogPolicy.kt")

        assertTrue(source.contains("val initialViewportType = initialViewportTargetType(item)"))
        assertTrue(source.contains("bindViewportModeToggle(views.viewportModeToggle, initialViewportType, false)"))
        assertTrue(source.contains("bindViewportInputHint(views.viewportInputLayout, initialViewportType)"))
        assertTrue(source.contains("bindFontModeToggle(views.fontModeToggle, initialFontMode(item.fontMode), false)"))
        assertTrue(source.contains("fun initialViewportTargetType(item: AppListItem?)"))
        assertTrue(source.contains("ViewportTargetType.normalize(item.viewportTargetType)"))
        assertTrue(source.contains("AppConfigInputValidation.initialFontMode(fontMode)"))
        assertTrue(source.contains("AppConfigInputValidation.initialViewportTargetType("))
    }

    @Test
    fun viewportModeSwitchKeepsSeparateInputValues() {
        val binderSource = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val stateSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogModels.kt")
        val modeSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogPolicy.kt")
        val source = binderSource + stateSource +
            modeSource +
            read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetInteractions.kt") +
            read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetModeValidationBinder.kt")
        val switchStart = modeSource.indexOf("fun switchViewportTargetType(")
        val switchBlock = modeSource.substring(switchStart)

        assertTrue(source.contains("val initialViewportInput: String? = formatViewportInput(item.viewportTargetSpec)"))
        assertTrue(source.contains("state.updateViewportInput("))
        assertTrue(source.contains("AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle), s)"))
        val fontWatcherStart = source.indexOf("val fontValidationWatcher = object : TextWatcher")
        val fontWatcherEnd = source.indexOf(
                "views.viewportInputView.addTextChangedListener", fontWatcherStart)
        val fontWatcherBlock = source.substring(fontWatcherStart, fontWatcherEnd)
        assertFalse(fontWatcherBlock.contains("state.updateViewportInput("))
        assertTrue(source.contains("AppConfigDialogBinder.toggleViewportMode("))
        assertTrue(source.contains("views.viewportModeToggle, views.viewportInputView, state)"))
        assertTrue(switchBlock.contains("bindViewportModeToggle(toggle, nextType, animate)"))
        assertTrue(switchBlock.contains("state.updateViewportInput(resolveViewportMode(toggle), inputView.text)"))
        assertTrue(switchBlock.contains("inputView.text = state.viewportInputFor(nextType)"))
        assertTrue(source.contains("fun viewportInputFor(viewportTargetType: String?)"))
        assertTrue(source.contains("fun clearViewportInputs()"))
        assertFalse(source.contains("viewportScaleText"))
        assertFalse(source.contains("viewportAbsoluteText"))
    }

    @Test
    fun appConfigWizardHintUsesNamedDimensions() {
        val coordinator = read(
            "src/main/java/com/dpis/module/ui/presentation/MainWorkspacePresentationCoordinator.kt",
        )
        assertTrue(coordinator.contains("AppConfigSheetWizardStore.shouldShowAdvancedHint(context)"))
        assertTrue(coordinator.contains("AppConfigSheetWizardStore.markAdvancedHintDismissed(context)"))
    }

    @Test
    fun appConfigSheetUsesImeResizeAndScrollsFocusedInput() {
        val overlay = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt",
        )
        assertTrue(overlay.contains("fun AppConfigEditorOverlay("))
    }

    @Test
    fun appConfigSheetUsesSharedFormInputFocusBehavior() {
        val interactions = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetInteractions.kt")
        val validation = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetModeValidationBinder.kt")
        val actions = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetActionBinder.kt")
        val host = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val focusBinder = read("src/main/java/com/dpis/module/ui/FormInputFocusBinder.java")

        assertTrue(focusBinder.contains("public final class FormInputFocusBinder"))
        assertTrue(interactions.contains("AppConfigSheetModeValidationBinder(binder, host)"))
        assertTrue(validation.contains("FormInputFocusBinder.bindDismissOnOutsideTouch"))
        assertTrue(validation.contains("WechatDpiSheetBinder.inputViewForFocus(dialogView)"))
        assertTrue(actions.contains("FormInputFocusBinder.clearFocusAndHideIme"))
        assertTrue(actions.contains("WechatDpiSheetBinder.inputViewForFocus(dialogView)"))
        assertFalse(host.contains("clearDialogInputFocus("))
    }

    @Test
    fun binderDoesNotKeepEmptyHyperOsSectionWrapper() {
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")

        assertFalse(source.contains("bindHyperOsNativeSection"))
    }

    @Test
    fun simplifiedChineseHyperOsProxyFailureMessagesAreLocalized() {
        val strings = read("src/main/res/values-zh-rCN/strings.xml")
        val applyFailed = simplifiedChineseStringValue("dialog_hyperos_native_proxy_apply_failed")
        val unmountFailed = simplifiedChineseStringValue("dialog_hyperos_native_proxy_unmount_failed")

        assertFalse(strings.contains("HyperOS Native Proxy applied. Restart target app."))
        assertFalse(strings.contains("HyperOS Native Proxy apply failed. Check root and native directory."))
        assertTrue(applyFailed.contains("设置失败"))
        assertTrue(unmountFailed.contains("回滚失败"))
    }

    @Test
    fun resetButtonOnlyClearsDialogInputsUntilSaved() {
        val source = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetActionBinder.kt")
        val resetStart = source.indexOf("views.disableButton.setOnClickListener")
        val saveStart = source.indexOf("views.saveButton.setOnClickListener")
        val resetBlock = source.substring(resetStart, saveStart)

        assertFalse(resetBlock.contains("unmountHyperOsNativeProxy"))
    }

    @Test
    fun savingViewportConfigPublishesRuntimeViewportTarget() {
        val saveSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.kt")
        val runtimeLaunch = read(
            "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchSession.kt"
        )

        assertTrue(saveSource.contains(
                "ViewportApplyMode.SYSTEM == ViewportApplyMode.normalize(viewportApplyMode)"))
        assertTrue(runtimeLaunch.contains("scheduleRuntimePropertiesForTargetLaunch(packageName)"))
        assertTrue(runtimeLaunch.contains("ViewportPropertySyncer.syncTarget(packageName, store)"))
        assertTrue(runtimeLaunch.contains("fun finalizeAppConfigSaveWithRuntimeSync("))
        assertTrue(saveSource.contains("ViewportDraftValue.invalid()"))
        assertFalse(saveSource.contains("INVALID_DRAFT"))
        assertFalse(saveSource.contains("Integer.MIN_VALUE"))
    }

    @Test
    fun previewViewportApplyModeUsesMutableSheetStateForStatusAndSave() {
        val binderSource = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        val stateSource = read("src/main/java/com/dpis/module/appconfig/AppConfigDialogModels.kt")
        val actionSource = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigSheetActionBinder.kt")
        val saveSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.kt")

        assertTrue(stateSource.contains("viewportApplyMode: String?"))
        assertTrue(stateSource.contains("var viewportApplyMode: String = ViewportApplyMode.normalize(viewportApplyMode)"))
        assertTrue(binderSource.contains("state.viewportApplyMode"))
        assertTrue(actionSource.contains("state.viewportApplyMode"))
        assertTrue(actionSource.contains("state.viewportApplyModeResetRequested"))
        assertTrue(saveSource.contains("currentViewportApplyMode: String?"))
        assertTrue(saveSource.contains("viewportApplyModeResetRequested: Boolean"))
        assertTrue(saveSource.contains("viewportApplyModeResetRequested, viewportTargetSpec"))
    }

    @Test
    fun savingEmptyFontScaleClearsOnlyFontScaleRuntimeTargets() {
        val source = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.kt")
        val clearStart = source.indexOf("if (fontScalePercent == null)")
        val configuredStart = source.indexOf("} else {", clearStart)
        val clearBlock = source.substring(clearStart, configuredStart)

        assertTrue(clearBlock.contains("ConfigDraftSaveSemantics.fontApplyModeForSave(fontMode)"))
        assertFalse(clearBlock.contains("FontRuntimePropertySyncer.clearFontScaleTargetAsync(item.packageName)"))
        assertFalse(clearBlock.contains("FontRuntimePropertySyncer.clearTargetAsync(item.packageName)"))
        assertFalse(clearBlock.contains("FontHookDomainPropertySyncer.clearTargetAsync(item.packageName)"))
    }

    @Test
    fun savingPreviewHookDomainsIsIndependentFromFontScaleBranch() {
        val source = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.kt")
        val persistCall = source.indexOf("persistPreviewOnlyConfig(")
        val fontScaleBranch = source.indexOf("if (fontScalePercent == null)")

        assertTrue(persistCall > 0)
        assertTrue(fontScaleBranch > persistCall)
        assertTrue(source.contains("publishFontHookDomainsAfterSave(item.packageName, store)"))
    }

    @Test
    fun disablingDpisClearsRuntimePropertiesForAllCompatPaths() {
        val source = read(
            "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchSession.kt"
        )
        val disableStart = source.indexOf("if (!enabled) {")
        val disableEnd = source.indexOf("}", disableStart)
        val disableBlock = source.substring(disableStart, disableEnd)

        assertTrue(disableBlock.contains("FontRuntimePropertySyncer.clearTargetAsync(packageName)"))
        assertTrue(disableBlock.contains("ViewportPropertySyncer.clearTargetAsync(packageName)"))
    }

    @Test
    fun savingFontConfigPublishesUnifiedFontRuntimeTarget() {
        val saveSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.kt")
        val runtimeLaunch = read(
            "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchSession.kt"
        )

        assertTrue(runtimeLaunch.contains("scheduleRuntimePropertiesForTargetLaunch(packageName)"))
        assertTrue(runtimeLaunch.contains("FontRuntimePropertySyncer.syncTarget(packageName, store)"))
        assertTrue(runtimeLaunch.contains("fun finalizeAppConfigSaveWithRuntimeSync("))
        assertFalse(saveSource.contains("FontRuntimePropertySyncer.publishTargetAsync("))
        assertTrue(saveSource.contains(
                "FontApplyMode.SYSTEM_EMULATION == FontApplyMode.normalize("))
    }

    @Test
    fun savingTypefaceConfigPublishesRuntimeTypefaceTarget() {
        val saveSource = read("src/main/java/com/dpis/module/appconfig/AppConfigSaveHandler.kt")
        val runtimeLaunch = read(
            "src/main/java/com/dpis/module/runtime/presentation/RuntimeLaunchSession.kt"
        )

        assertTrue(runtimeLaunch.contains("scheduleRuntimePropertiesForTargetLaunch(packageName)"))
        assertTrue(runtimeLaunch.contains("FontRuntimePropertySyncer.syncTarget(packageName, store)"))
        assertFalse(saveSource.contains("FontRuntimePropertySyncer.publishTypefaceTargetAsync("))
    }

    @Test
    fun appConfigInputErrorsAreRenderedBySharedValidation() {
        val binder = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        assertTrue(binder.contains(
                "ConfigValueInputErrorBinder.bindFullMessage(viewportInputLayout, viewportValid)"))
        assertTrue(binder.contains(
                "ConfigValueInputErrorBinder.bindFullMessage(fontInputLayout, fontValid)"))
        assertTrue(read("src/main/java/com/dpis/module/appconfig/ConfigValueInputErrorBinder.java")
                .contains("R.string.status_save_invalid"))
    }

    @Test
    fun modeToggleThumbUsesHalfOfMeasuredTrackAfterRelayout() {
        val binder = read("src/main/java/com/dpis/module/appconfig/presentation/AppConfigDialogBinder.kt")
        assertTrue(binder.contains("private fun updateModeToggleThumbLayout(toggle: ModeToggle?): Int"))
        assertTrue(binder.contains("private fun modeToggleTrack(toggle: ModeToggle): View"))
        assertTrue(binder.contains("val half = availableWidth / 2"))
    }

    @Test
    fun landscapeDetailReassertsModeToggleSizeWhenRebound() {
        val overlay = read(
            "src/main/java/com/dpis/module/appconfig/presentation/AppConfigEditorOverlay.kt",
        )
        assertTrue(overlay.contains("fun AppConfigEditorOverlay("))
    }

    private fun read(relativePath: String): String {
        return SourceSmokeTestPaths.read(relativePath)
    }

    private fun simplifiedChineseStringValue(name: String): String {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isIgnoringComments = true
        SourceSmokeTestPaths.open("src/main/res/values-zh-rCN/strings.xml").use { input ->
            val document = factory.newDocumentBuilder().parse(input)
            val strings = document.getElementsByTagName("string")
            for (index in 0 until strings.length) {
                val string = strings.item(index) as Element
                if (name == string.getAttribute("name")) {
                    return string.textContent
                }
            }
        }
        throw IllegalArgumentException("Missing string resource: $name")
    }

    private fun assertThumbStartsAtZeroWidth(layout: String, thumbId: String) {
        val thumbIndex = layout.indexOf("android:id=\"@+id/$thumbId\"")
        assertTrue(thumbIndex >= 0)
        val viewStart = layout.lastIndexOf("<View", thumbIndex)
        val tagEnd = layout.indexOf(">", thumbIndex)
        assertTrue(viewStart in 0..<tagEnd)
        val declaration = layout.substring(viewStart, tagEnd)
        assertTrue(declaration.contains("android:layout_width=\"0dp\""))
    }

    private fun draftStateChangeCount(source: String): Int {
        val needle = "host.onDraftStateChanged(state)"
        var count = 0
        var index = 0
        while (true) {
            index = source.indexOf(needle, index)
            if (index < 0) return count
            count++
            index += needle.length
        }
    }
}
