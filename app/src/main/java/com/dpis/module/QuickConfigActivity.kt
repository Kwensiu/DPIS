package com.dpis.module

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.dpis.module.appconfig.AppConfigDialogBinder
import com.dpis.module.appconfig.AppConfigDialogBinder.AppConfigDialogState
import com.dpis.module.appconfig.AppConfigDialogBinder.Companion.captureDialogActionStyle
import com.dpis.module.appconfig.AppConfigDialogBinder.Companion.resolveFontMode
import com.dpis.module.appconfig.AppConfigDialogBinder.Companion.resolveViewportMode
import com.dpis.module.appconfig.AppConfigDialogBinder.Companion.showSaveButtonFeedback
import com.dpis.module.appconfig.AppConfigDialogBinder.Companion.stateFor
import com.dpis.module.appconfig.AppConfigDialogBinder.Companion.updateSaveButtonState
import com.dpis.module.appconfig.AppConfigDialogBinder.Companion.viewsFor
import com.dpis.module.appconfig.AppConfigDialogBinder.ProcessAction
import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.appconfig.AppConfigPrefillPreview.resolveForEditor
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.AppConfigSaveHandler.Result.Companion.failure
import com.dpis.module.appconfig.EditorActions
import com.dpis.module.appconfig.EditorActions.create
import com.dpis.module.appconfig.EditorDialogStateFactory
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.appconfig.EditorPresentation
import com.dpis.module.appconfig.EditorPresentationFactory.create
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.ForegroundPackageResolver
import com.dpis.module.applist.InstalledAppCatalogCoordinator
import com.dpis.module.diagnostics.AppLauncher
import com.dpis.module.diagnostics.Coordinator
import com.dpis.module.diagnostics.ExportBuilder
import com.dpis.module.diagnostics.ExportBuilder.DiagnosticPackage
import com.dpis.module.diagnostics.LogGate
import com.dpis.module.diagnostics.PackagingDialog.show
import com.dpis.module.diagnostics.ResultSheet
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.fonts.HyperOsNativeAppDetector
import com.dpis.module.fonts.HyperOsNativeProxyBindMounter
import com.dpis.module.fonts.hookdomain.FontHookDomainDialog
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation.Companion.forOverride
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry.automaticCustomizableDomains
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.process.ProcessActionHandler
import com.dpis.module.quickconfig.QuickConfigTargetDecision
import com.dpis.module.quirks.WechatDpiSheetBinder
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.runtime.RuntimeConfigDelivery
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.settings.SystemScopeCoordinator
import com.dpis.module.ui.compose.ComposeMessageDialog.show
import com.dpis.module.ui.compose.QuickConfigPresentation
import com.dpis.module.ui.compose.SupportActivityContent
import com.dpis.module.ui.dialog.ConfirmDialog.showWithLabels
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportPropertySyncer
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QuickConfigActivity : LocalizedActivity() {
    private val appConfigSaveHandler = AppConfigSaveHandler()
    private val processActionHandler = ProcessActionHandler(
        this
    ) { packageName: String? ->
        this.syncRuntimePropertiesForTargetLaunch(
            packageName
        )
    }
    private val systemScopeCoordinator = SystemScopeCoordinator(createSystemScopeHost())
    private val feedbackDiagnosticAppLauncher = AppLauncher(this)
    private val feedbackDiagnosticExportBuilder = ExportBuilder(this)
    private val feedbackDiagnosticExportExecutor
            : ExecutorService = Executors.newSingleThreadExecutor()
    private val appConfigDialogHost: AppConfigDialogBinder.Host = createHost()
    private val feedbackDiagnosticCoordinator = Coordinator(createFeedbackDiagnosticHost())
    private val activeEditorRoot: View? = null
    private var activityResumed = false
    private var pendingFeedbackDiagnosticResult: Coordinator.Result? = null
    private var pendingFeedbackDiagnosticPackage: DiagnosticPackage? = null
    private var activePackagingDialog: AlertDialog? = null
    private var presentation: QuickConfigPresentation? = null
    private var editingItem: AppListItem? = null
    private var editingDraft: EditorDraft? = null
    private var savedEditingDraft: EditorDraft? = null
    private var editingDestination: ConfigEditorDestination? = ConfigEditorDestination.MAIN
    private var editingSaveFeedback = false

    // The framework can retain the authorization prompt while this translucent Activity redraws.
    // Keep one in-flight request per editor session so repeated Save taps do not open duplicates.
    private var editingScopeRequestPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentation = QuickConfigPresentation()
        SupportActivityContent.installQuickConfig(this, presentation!!)

        val retainedSession = lastCustomNonConfigurationInstance as QuickConfigEditorSession?
        if (retainedSession != null) {
            editingItem = retainedSession.item
            editingDraft = retainedSession.draft
            savedEditingDraft = retainedSession.savedDraft
            editingDestination = retainedSession.destination
            refreshComposeEditor()
            return
        }

        val explicitPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val usageAccessGranted = ForegroundPackageResolver.hasUsageAccess(this)
        val targetDecision = QuickConfigTargetDecision.decide(
            explicitPackageName,
            usageAccessGranted,
            if (usageAccessGranted) ForegroundPackageResolver.resolve(this) else null
        )
        if (targetDecision.kind == QuickConfigTargetDecision.Kind.REQUEST_USAGE_ACCESS) {
            openUsageAccessSettings()
            finish()
            return
        }
        val packageName = targetDecision.packageName
        val item = if (packageName != null) createItem(packageName) else null
        if (item == null) {
            Toast.makeText(this, R.string.quick_config_target_unavailable, Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }
        editingItem = resolveForEditor(
            this,
            item,
            this.hookConfigStore
        )
        editingDraft = EditorDraft.fromItem(editingItem!!)
        savedEditingDraft = editingDraft
        refreshComposeEditor()
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? {
        if (editingItem == null || editingDraft == null) {
            return null
        }
        return QuickConfigEditorSession(
            editingItem,
            editingDraft,
            savedEditingDraft,
            editingDestination
        )
    }

    private fun openUsageAccessSettings() {
        val packageSettings = Intent(
            Settings.ACTION_USAGE_ACCESS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        try {
            startActivity(packageSettings)
        } catch (packagePageUnavailable: ActivityNotFoundException) {
            try {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (settingsUnavailable: ActivityNotFoundException) {
                Toast.makeText(this, R.string.quick_config_target_unavailable, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun refreshComposeEditor() {
        val item = editingItem
        val draft = editingDraft
        if (item == null || draft == null || presentation == null) {
            return
        }
        val dialogState = composeDialogState(item, draft)
        presentation!!.show(
            create(
                item,
                resolvePackageVersionName(item.packageName),
                draft,
                AppConfigDialogBinder(this, appConfigDialogHost)
                    .typefaceSelectorText(draft.selectedTypefaceId),
                forOverride(
                    resolveFontHookDomainsForDraft(item, dialogState),
                    automaticCustomizableDomains()
                ).buttonText(this),
                savedEditingDraft,
                editingSaveFeedback,
                this.isSystemHookEnabled,
                automaticCustomizableDomains(),
                editingDestination,
                createComposeActions(item, draft)
            )
        )
    }

    private fun createComposeActions(
        item: AppListItem,
        draft: EditorDraft
    ): EditorPresentation.Actions {
        return create(
            object : EditorActions.Host {
                override fun updateDraft(nextDraft: EditorDraft) {
                    this@QuickConfigActivity.updateDraft(nextDraft)
                }

                override fun showWechatDpiHelp() {
                    show(
                        this@QuickConfigActivity,
                        getString(R.string.dialog_wechat_dpi_help_title),
                        getString(R.string.dialog_wechat_dpi_help_message),
                        getString(R.string.dialog_close_button)
                    )
                }

                override fun navigate(destination: ConfigEditorDestination) {
                    editingDestination = destination
                    refreshComposeEditor()
                }

                override fun toggleScope(
                    currentlySelected: Boolean,
                    onSelected: Runnable,
                    onDeselected: Runnable
                ) {
                    systemScopeCoordinator.toggleScope(
                        item.packageName,
                        item.label,
                        currentlySelected,
                        onSelected,
                        onDeselected
                    )
                }

                override fun setDpisEnabled(enabled: Boolean): Boolean {
                    return appConfigDialogHost.setDpisEnabled(item.packageName, enabled)
                }

                override fun executeProcessAction(
                    action: ProcessAction
                ) {
                    executeDialogProcessAction(item, action)
                }

                override fun startFeedbackDiagnostic(
                    currentDraft: EditorDraft
                ) {
                    this@QuickConfigActivity.startFeedbackDiagnostic(
                        item, composeDialogState(item, currentDraft)
                    )
                }

                override fun save(
                    currentDraft: EditorDraft
                ) {
                    saveComposeEditor(item, currentDraft)
                }

                override fun close() {
                    finish()
                }
            },
            item,
            draft
        )
    }

    private fun updateDraft(draft: EditorDraft) {
        editingDraft = draft
        refreshComposeEditor()
    }

    private fun composeDialogState(
        item: AppListItem?,
        draft: EditorDraft
    ): AppConfigDialogState {
        return EditorDialogStateFactory.create(item, draft)
    }

    private fun saveComposeEditor(item: AppListItem, draft: EditorDraft): Boolean {
        val viewport = AppConfigInputValidation.parseViewportTargetSpec(
            draft.viewportInputFor(draft.viewportMode), draft.viewportMode
        )
        val fontScale = AppConfigInputValidation.parseFontScalePercentOrNull(draft.fontInput)
        var result = appConfigSaveHandler.saveResolved(
            item, viewport, draft.viewportMode, draft.viewportApplyMode,
            draft.viewportApplyModeResetRequested, fontScale, draft.fontMode,
            draft.selectedTypefaceId, draft.draftFontHookDomainsRaw,
            draft.fontHookDomainsResetRequested, draft.viewportScaleInput,
            draft.viewportAbsoluteInput, this.isSystemHookEnabled, this.hookConfigStore, null
        )
        if (result.success && !WechatDpiSheetBinder.save(
                draft.wechatDpiInput, item.packageName, draft.dpisEnabled, this.hookConfigStore
            )
        ) {
            result = failure(
                if (WechatDpiSheetBinder.isInputValid(draft.wechatDpiInput))
                    R.string.system_settings_save_failed
                else
                    R.string.status_save_invalid
            )
        }
        if (result.messageResId != 0) {
            showToast(result.messageResId)
        }
        if (!result.success) {
            return false
        }
        publishAfterSave(item.packageName)
        savedEditingDraft = draft.afterSuccessfulSave()
        editingDraft = savedEditingDraft
        editingSaveFeedback = true
        requestScopeAfterSuccessfulComposeSave(item)
        refreshComposeEditor()
        window.decorView.postDelayed({
            editingSaveFeedback = false
            refreshComposeEditor()
        }, 1500L)
        return true
    }

    /**
     * Compose Quick Config has no legacy dialog View to own the post-save scope request.
     * Request it from the Activity instead, retaining the same Modern-only service boundary as
     * the main Compose editor and reflecting approval in its immutable draft.
     */
    private fun requestScopeAfterSuccessfulComposeSave(item: AppListItem?) {
        val draft = editingDraft
        if (item == null || draft == null || !item.scopeKnown || draft.scopeSelected
            || editingScopeRequestPending || (item.packageName != draft.packageName)
        ) {
            return
        }
        editingScopeRequestPending = true
        val requestStarted = systemScopeCoordinator.requestScope(
            item.packageName,
            item.label,
            { onComposeScopeApproved(item.packageName) },
            { editingScopeRequestPending = false },
            false
        )
        if (requestStarted) {
            showToast(R.string.save_scope_request_notice)
        } else {
            editingScopeRequestPending = false
        }
    }

    private fun onComposeScopeApproved(packageName: String) {
        if (editingDraft != null && packageName == editingDraft!!.packageName) {
            editingDraft = editingDraft!!.withScopeSelected(true)
        }
        if (savedEditingDraft != null && packageName == savedEditingDraft!!.packageName) {
            savedEditingDraft = savedEditingDraft!!.withScopeSelected(true)
        }
        refreshComposeEditor()
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        RootAccessProbe.refreshAsync(null)
        feedbackDiagnosticCoordinator.onDpisResumed()
        maybeShowPendingFeedbackDiagnosticResult()
    }

    protected override fun onStop() {
        activityResumed = false
        super.onStop()
    }

    protected override fun onDestroy() {
        dismissPackagingDialog()
        feedbackDiagnosticCoordinator.shutdown()
        feedbackDiagnosticExportExecutor.shutdownNow()
        super.onDestroy()
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SAVE_FEEDBACK_DIAGNOSTIC && resultCode == RESULT_OK && data != null && data.data != null) {
            saveFeedbackDiagnosticZip(data.data)
        }
    }

    private fun createItem(packageName: String): AppListItem? {
        try {
            val packageManager = getPackageManager()
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val label = packageManager.getApplicationLabel(applicationInfo).toString()
            val icon = applicationInfo.loadIcon(packageManager)
            val systemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    && (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            return InstalledAppCatalogCoordinator.createAppListItem(
                this.hookConfigStore,
                loadScopePackages(),
                DpisApplication.xposedService != null,
                label,
                packageName,
                systemApp,
                HyperOsNativeAppDetector.isNativeProxyCandidate(applicationInfo),
                true,
                icon
            )
        } catch (exception: PackageManager.NameNotFoundException) {
            return null
        } catch (exception: RuntimeException) {
            return null
        }
    }

    private fun loadScopePackages(): Set<String> {
        val service = DpisApplication.xposedService ?: return emptySet()
        return try {
            HashSet(service.scope)
        } catch (exception: RuntimeException) {
            emptySet()
        }
    }

    private fun createHost(): AppConfigDialogBinder.Host {
        return object : AppConfigDialogBinder.Host {
            override fun toggleScope(
                item: AppListItem?,
                currentlyInScope: Boolean,
                onTurnedInScope: Runnable?,
                onTurnedOutScope: Runnable?
            ) {
                val target = item ?: return
                systemScopeCoordinator.toggleScope(
                    target.packageName,
                    target.label,
                    currentlyInScope,
                    onTurnedInScope,
                    onTurnedOutScope
                )
            }

            override fun requestScope(
                item: AppListItem?,
                onTurnedInScope: Runnable?,
                onRequestFinished: Runnable?
            ): Boolean {
                val target = item ?: return false
                return systemScopeCoordinator.requestScope(
                    target.packageName,
                    target.label,
                    onTurnedInScope,
                    onRequestFinished,
                    false
                )
            }

            override fun executeProcessAction(
                item: AppListItem?,
                action: ProcessAction?
            ) {
                if (action != null) executeDialogProcessAction(item, action)
            }

            override fun applyHyperOsNativeProxy(item: AppListItem?, onFinished: Runnable?) {
                if (item != null) executeHyperOsNativeProxyMount(item, true, onFinished)
            }

            override fun unmountHyperOsNativeProxy(item: AppListItem?, onFinished: Runnable?) {
                if (item != null) executeHyperOsNativeProxyMount(item, false, onFinished)
            }

            override fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean {
                val targetPackageName = packageName ?: return false
                val store: DpisConfigStore? = this@QuickConfigActivity.hookConfigStore
                if (store == null || !store.setTargetDpisEnabled(targetPackageName, enabled)) {
                    showToast(R.string.system_settings_save_failed)
                    return false
                }
                if (!enabled) {
                    FontRuntimePropertySyncer.clearTargetAsync(targetPackageName)
                    FontHookDomainPropertySyncer.clearTargetAsync(targetPackageName)
                    ViewportPropertySyncer.clearTargetAsync(targetPackageName)
                }
                showToast(
                    if (enabled)
                        R.string.dialog_dpis_enabled_status
                    else
                        R.string.dialog_dpis_disabled_status
                )
                WechatDpiSheetBinder.publishForDpisState(targetPackageName, enabled)
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
                return true
            }

            override fun showFontHookDomains(
                item: AppListItem?,
                state: AppConfigDialogState?,
                onStateChanged: Runnable?
            ) {
                this@QuickConfigActivity.showFontHookDomains(item, state, onStateChanged)
            }

            override fun getFontHookDomainsButtonText(
                item: AppListItem?,
                state: AppConfigDialogState?
            ): String {
                return forOverride(
                    resolveFontHookDomainsForDraft(item, state),
                    automaticCustomizableDomains()
                )
                    .buttonText(this@QuickConfigActivity)
            }

            override fun openTypefaceLibrary() {
                startActivity(Intent(this@QuickConfigActivity, FontLibraryActivity::class.java))
            }

            override fun startFeedbackDiagnostic(
                item: AppListItem?,
                state: AppConfigDialogState?
            ) {
                this@QuickConfigActivity.startFeedbackDiagnostic(item, state)
            }

            override fun saveAppConfig(
                dialogView: View?,
                item: AppListItem?,
                dpisEnabled: Boolean,
                viewportInput: TextInputEditText?,
                fontScaleInput: TextInputEditText?,
                viewportMode: String?,
                viewportApplyMode: String?,
                viewportApplyModeResetRequested: Boolean,
                fontMode: String?,
                selectedTypefaceId: String?,
                draftFontHookDomainsRaw: String?,
                fontHookDomainsResetRequested: Boolean,
                viewportScaleInput: String?,
                viewportAbsoluteInput: String?
            ): AppConfigSaveHandler.Result? {
                val target = item ?: return null
                val result = appConfigSaveHandler.save(
                    target,
                    viewportInput!!,
                    fontScaleInput!!,
                    viewportMode,
                    viewportApplyMode,
                    viewportApplyModeResetRequested,
                    fontMode,
                    selectedTypefaceId,
                    draftFontHookDomainsRaw,
                    fontHookDomainsResetRequested,
                    viewportScaleInput,
                    viewportAbsoluteInput,
                    this@QuickConfigActivity.isSystemHookEnabled,
                    this@QuickConfigActivity.hookConfigStore,
                    null
                )
                return finalizeSave(result, dialogView, target.packageName, dpisEnabled)
            }

            override val configStore: DpisConfigStore?
                get() = this@QuickConfigActivity.hookConfigStore

            override fun requestAppsLoad() {
            }

            override fun onRuntimeConfigSaved() {
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
            }

            override fun onDraftStateChanged(state: AppConfigDialogState?) {
            }

            override fun showToast(messageResId: Int) {
                this@QuickConfigActivity.showToast(messageResId)
            }
        }
    }

    private fun createFeedbackDiagnosticHost(): Coordinator.Host {
        return object : Coordinator.Host {
            override fun restartTargetAppForDiagnostic(packageName: String?): Boolean {
                syncRuntimePropertiesForTargetLaunch(packageName)
                return feedbackDiagnosticAppLauncher.restartForDiagnostic(packageName)
            }

            override fun dpisPackageName(): String? {
                return packageName
            }

            override fun rootAccess(): RootAccessProbe.Result {
                return RootAccessProbe.cachedResult()
            }

            override fun systemHooksEnabled(): Boolean {
                return this@QuickConfigActivity.isSystemHookEnabled
            }

            override fun currentTimeMillis(): Long {
                return System.currentTimeMillis()
            }

            override fun onFeedbackDiagnosticStarted() {
                showToast(R.string.feedback_diagnostic_started)
            }

            override fun onFeedbackDiagnosticUnavailable() {
                showToast(R.string.feedback_diagnostic_unavailable)
            }

            override fun onFeedbackDiagnosticRootRequired() {
                showToast(R.string.feedback_diagnostic_root_required)
            }

            override fun onFeedbackDiagnosticFinished(result: Coordinator.Result?) {
                pendingFeedbackDiagnosticResult = result
                maybeShowPendingFeedbackDiagnosticResult()
            }
        }
    }

    private fun createSystemScopeHost(): SystemScopeCoordinator.Host {
        return object : SystemScopeCoordinator.Host {
            override fun showToast(messageResId: Int, vararg formatArgs: Any?) {
                Toast.makeText(
                    this@QuickConfigActivity,
                    getString(messageResId, *formatArgs),
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun requestAppsLoad() {
            }

            override fun runOnUiThread(runnable: Runnable) {
                this@QuickConfigActivity.runOnUiThread(runnable)
            }
        }
    }

    private val isSystemHookEnabled: Boolean
        get() {
            val store = this.hookConfigStore
            return store != null && store.isSystemServerHooksEnabled()
        }

    private val hookConfigStore: DpisConfigStore?
        get() = DpisApplication.getActiveHookConfigStore(this)

    private fun publishAfterSave(packageName: String?) {
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        val store = this.hookConfigStore
        ViewportPropertySyncer.syncTarget(packageName, store)
        FontRuntimePropertySyncer.syncTarget(packageName, store)
    }

    private fun syncRuntimePropertiesForTargetLaunch(packageName: String?) {
        publishAfterSave(packageName)
    }

    private fun finalizeSave(
        result: AppConfigSaveHandler.Result?,
        dialogView: View?,
        packageName: String?,
        dpisEnabled: Boolean
    ): AppConfigSaveHandler.Result {
        if (result == null) {
            return failure(R.string.system_settings_save_failed)
        }
        if (!result.success) {
            return result
        }
        val store = this.hookConfigStore
        if (!WechatDpiSheetBinder.save(dialogView, packageName, dpisEnabled, store)) {
            return failure(
                if (WechatDpiSheetBinder.isInputValid(dialogView))
                    R.string.system_settings_save_failed
                else
                    R.string.status_save_invalid
            )
        }
        publishAfterSave(packageName)
        return result
    }

    private fun startFeedbackDiagnostic(
        item: AppListItem?,
        state: AppConfigDialogState?
    ) {
        if (item == null) {
            return
        }
        if (!LogGate.ensureEnabled(
                this,
                { showFeedbackDiagnosticConfirmation(item, state) },
                null
            )
        ) {
            return
        }
        showFeedbackDiagnosticConfirmation(item, state)
    }

    private fun showFeedbackDiagnosticConfirmation(
        item: AppListItem,
        state: AppConfigDialogState?
    ) {
        showWithLabels(
            this,
            getString(R.string.feedback_diagnostic_action),
            getString(
                R.string.feedback_diagnostic_confirm_message,
                item.label
            ),
            getString(android.R.string.cancel),
            getString(R.string.feedback_diagnostic_save_and_start_button),
            {
                val diagnosticItem = saveCurrentConfigForDiagnostic(item)
                if (diagnosticItem != null) {
                    val started = feedbackDiagnosticCoordinator.start(
                        Coordinator.Request.fromPersisted(
                            diagnosticItem,
                            state,
                            resolvePackageVersionName(item.packageName),
                            this.hookConfigStore
                        )
                    )
                    if (!started) {
                        showToast(R.string.feedback_diagnostic_unavailable)
                    }
                }
            },
            {}
        )
    }

    private fun saveCurrentConfigForDiagnostic(item: AppListItem?): AppListItem? {
        if (item == null) {
            return null
        }
        if (editingDraft != null && item.packageName == editingDraft!!.packageName) {
            if (!saveComposeEditor(item, editingDraft!!)) {
                return null
            }
            return item.withWechatDpi(readPersistedWechatDpiForDiagnostic(item.packageName))
        }
        val root = activeEditorRoot
        val views = viewsFor(root)
        val state = stateFor(root)
        if (root == null || views == null || state == null) {
            return item
        }
        if (!updateSaveButtonState(root, views)) {
            showToast(R.string.status_save_invalid)
            return null
        }
        val result = appConfigDialogHost.saveAppConfig(
            root,
            item,
            state.dpisEnabled,
            views.viewportInputView,
            views.fontInputView,
            resolveViewportMode(views.viewportModeToggle),
            state.viewportApplyMode,
            state.viewportApplyModeResetRequested,
            resolveFontMode(views.fontModeToggle),
            state.selectedTypefaceId,
            state.draftFontHookDomainsRaw,
            state.fontHookDomainsResetRequested,
            state.viewportScaleInput,
            state.viewportAbsoluteInput
        )
        if (result!!.messageResId != 0) {
            showToast(result.messageResId)
        }
        if (!result.success) {
            return null
        }
        state.previewFromGlobalPrefill = false
        state.draftFontHookDomainsRaw = null
        state.fontHookDomainsResetRequested = false
        state.viewportApplyModeResetRequested = false
        state.captureSavedDraft(views, false)
        showSaveButtonFeedback(views.saveButton)
        val binder = AppConfigDialogBinder(this, appConfigDialogHost)
        val style = captureDialogActionStyle(views.scopeButton)
        binder.refreshDialogState(views, state, style, this.isSystemHookEnabled, item)
        binder.syncHyperOsNativeProxyAfterSave(item, views, state)
        binder.requestScopeAfterSuccessfulSave(
            root, item, views, state, style,
            this.isSystemHookEnabled
        )
        return item.withWechatDpi(readPersistedWechatDpiForDiagnostic(item.packageName))
    }

    private fun readPersistedWechatDpiForDiagnostic(packageName: String?): Int? {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return null
        }
        val store = this.hookConfigStore
        return store?.getWechatDpi(packageName)
    }

    private fun maybeShowPendingFeedbackDiagnosticResult() {
        val diagnosticPackage = pendingFeedbackDiagnosticPackage
        if (diagnosticPackage != null && activityResumed) {
            pendingFeedbackDiagnosticPackage = null
            dismissPackagingDialog()
            showDiagnosticResultSheet(diagnosticPackage)
            return
        }
        val result = pendingFeedbackDiagnosticResult
        if (result == null || !activityResumed) {
            return
        }
        pendingFeedbackDiagnosticResult = null
        showPackagingDialog()
        feedbackDiagnosticExportExecutor.execute {
            val built: DiagnosticPackage? = try {
                feedbackDiagnosticExportBuilder.buildPackage(result)
            } catch (ignored: IOException) {
                null
            } catch (ignored: RuntimeException) {
                null
            }
            val finalBuilt = built
            runOnUiThread {
                dismissPackagingDialog()
                if (finalBuilt == null) {
                    showToast(R.string.feedback_diagnostic_save_failed)
                } else if (!activityResumed) {
                    pendingFeedbackDiagnosticPackage = finalBuilt
                } else {
                    showDiagnosticResultSheet(finalBuilt)
                }
            }
        }
    }

    private fun showDiagnosticResultSheet(
        diagnosticPackage: DiagnosticPackage?
    ) {
        if (diagnosticPackage == null) {
            return
        }
        ResultSheet(this, object : ResultSheet.Host {
            override fun shareFeedbackDiagnostic(
                diagnosticPackage: DiagnosticPackage
            ) {
                this@QuickConfigActivity.shareFeedbackDiagnostic(diagnosticPackage)
            }

            override fun saveFeedbackDiagnostic(
                diagnosticPackage: DiagnosticPackage
            ) {
                this@QuickConfigActivity.launchSaveFeedbackDiagnosticPicker(diagnosticPackage)
            }
        }).show(diagnosticPackage)
    }

    private fun showPackagingDialog() {
        dismissPackagingDialog()
        activePackagingDialog = show(this)
    }

    private fun dismissPackagingDialog() {
        if (activePackagingDialog != null) {
            activePackagingDialog!!.dismiss()
            activePackagingDialog = null
        }
    }

    @Suppress("deprecation")
    private fun launchSaveFeedbackDiagnosticPicker(
        diagnosticPackage: DiagnosticPackage?
    ) {
        if (diagnosticPackage == null) {
            return
        }
        pendingFeedbackDiagnosticPackage = diagnosticPackage
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(ExportBuilder.MIME_TYPE)
            .putExtra(Intent.EXTRA_TITLE, diagnosticPackage.fileName)
        try {
            startActivityForResult(intent, REQUEST_SAVE_FEEDBACK_DIAGNOSTIC)
        } catch (error: ActivityNotFoundException) {
            pendingFeedbackDiagnosticPackage = null
            showToast(R.string.feedback_diagnostic_save_failed)
        }
    }

    private fun saveFeedbackDiagnosticZip(uri: Uri?) {
        val diagnosticPackage = pendingFeedbackDiagnosticPackage
        pendingFeedbackDiagnosticPackage = null
        if (uri == null || diagnosticPackage == null) {
            showToast(R.string.feedback_diagnostic_save_failed)
            return
        }
        feedbackDiagnosticExportExecutor.execute {
            var success: Boolean
            try {
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream == null) {
                        throw IOException("Unable to open diagnostic output")
                    }
                    outputStream.write(diagnosticPackage.zipBytes)
                    success = true
                }
            } catch (error: IOException) {
                success = false
            } catch (error: RuntimeException) {
                success = false
            }
            val finalSuccess = success
            runOnUiThread {
                showToast(
                    if (finalSuccess)
                        R.string.feedback_diagnostic_save_success
                    else
                        R.string.feedback_diagnostic_save_failed
                )
            }
        }
    }

    private fun shareFeedbackDiagnostic(
        diagnosticPackage: DiagnosticPackage?
    ) {
        if (diagnosticPackage == null) {
            return
        }
        feedbackDiagnosticExportExecutor.execute {
            var uri: Uri? = null
            var success: Boolean
            try {
                val file = writeSharedFeedbackDiagnosticZip(diagnosticPackage)
                uri = FileProvider.getUriForFile(
                    this,
                    "$packageName.fileprovider",
                    file
                )
                success = true
            } catch (error: IOException) {
                success = false
            } catch (error: RuntimeException) {
                success = false
            }
            val finalUri = uri
            val finalSuccess = success
            runOnUiThread {
                if (!finalSuccess || finalUri == null) {
                    showToast(R.string.feedback_diagnostic_share_failed)
                } else {
                    launchFeedbackDiagnosticShareSheet(finalUri)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun writeSharedFeedbackDiagnosticZip(
        diagnosticPackage: DiagnosticPackage
    ): File {
        val directory = File(cacheDir, SHARED_FEEDBACK_DIAGNOSTIC_DIRECTORY_NAME)
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Unable to create diagnostic share directory")
        }
        val file = File(directory, diagnosticPackage.fileName)
        FileOutputStream(file, false).use { outputStream ->
            outputStream.write(diagnosticPackage.zipBytes)
        }
        return file
    }

    private fun launchFeedbackDiagnosticShareSheet(uri: Uri?) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType(ExportBuilder.MIME_TYPE)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(
                Intent.createChooser(
                    intent,
                    getString(R.string.feedback_diagnostic_share_action)
                )
            )
        } catch (error: ActivityNotFoundException) {
            showToast(R.string.feedback_diagnostic_share_failed)
        }
    }

    private fun resolvePackageVersionName(packageName: String?): String? {
        if (packageName.isNullOrBlank()) {
            return ""
        }
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (ignored: PackageManager.NameNotFoundException) {
            ""
        }
    }

    private fun showFontHookDomains(
        item: AppListItem?,
        state: AppConfigDialogState?,
        onStateChanged: Runnable?
    ) {
        if (item == null || item.packageName == null || item.packageName.isBlank()) {
            return
        }
        val store = this.hookConfigStore
        val automaticKnownDomains: MutableSet<String?> = HashSet(automaticCustomizableDomains())
        val currentOverride = resolveFontHookDomainsForDraft(item, state)
        FontHookDomainDialog.show(
            this,
            object : FontHookDomainDialog.Host {
                override fun saveCustom(
                    packageName: String?,
                    selectedKnownDomains: MutableSet<String?>?,
                    automaticKnownDomains: MutableSet<String?>?,
                    unknownDomains: MutableSet<String?>?
                ): Boolean {
                    if (state != null) {
                        state.draftFontHookDomainsRaw = HookDomainOverrideStore.rawValueForSelection(
                            selectedKnownDomains,
                            automaticKnownDomains,
                            unknownDomains
                        )
                        state.fontHookDomainsResetRequested = state.draftFontHookDomainsRaw == null
                    }
                    onStateChanged?.run()
                    return true
                }

                override fun restoreRecommended(packageName: String?): Boolean {
                    if (state != null) {
                        state.draftFontHookDomainsRaw = null
                        state.fontHookDomainsResetRequested = true
                    }
                    onStateChanged?.run()
                    return true
                }

                override fun saveViewportApplyMode(packageName: String?, mode: String?): Boolean {
                    if (state != null) {
                        state.viewportApplyMode = ViewportApplyMode.normalize(mode)
                        state.viewportApplyModeResetRequested = ViewportApplyMode.OFF == state.viewportApplyMode
                    }
                    onStateChanged?.run()
                    return true
                }
            },
            item.packageName,
            automaticKnownDomains,
            currentOverride,
            state?.viewportApplyMode ?: store!!.getTargetViewportApplyMode(item.packageName),
            this.isFontHookDomainEditingEnabled,
            onStateChanged
        )
    }

    private val isFontHookDomainEditingEnabled: Boolean
        get() {
            if (activeEditorRoot == null) {
                return false
            }
            val views =
                viewsFor(activeEditorRoot)
            return views != null && FontApplyMode.FIELD_REWRITE == resolveFontMode(
                views.fontModeToggle
            )
        }

    private fun resolveFontHookDomainsForDraft(
        item: AppListItem?,
        state: AppConfigDialogState?
    ): HookDomainOverride {
        if (state != null && state.fontHookDomainsResetRequested) {
            return HookDomainOverride.automatic()
        }
        if (state != null
            && (state.previewFromGlobalPrefill
                    || state.draftFontHookDomainsRaw != null)
        ) {
            return normalizedFontHookDomainsOverride(
                HookDomainOverrideStore.fromRaw(state.draftFontHookDomainsRaw),
                automaticCustomizableDomains()
            )
        }
        return normalizedFontHookDomainsOverride(
            HookDomainOverrideStore(this.hookConfigStore).read(
                item?.packageName
            ),
            automaticCustomizableDomains()
        )
    }

    private fun normalizedFontHookDomainsOverride(
        override: HookDomainOverride?,
        automaticKnownDomains: Set<String>?
    ): HookDomainOverride {
        return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
            override,
            automaticKnownDomains
        )
    }

    private fun executeHyperOsNativeProxyMount(
        item: AppListItem,
        apply: Boolean,
        onFinished: Runnable?
    ) {
        executeHyperOsNativeProxyMount(
            item,
            apply,
            object : HyperOsNativeProxyMountCallback {
                override fun onFinished(ignored: Boolean) {
                    onFinished?.run()
                }
            }
        )
    }

    private fun executeHyperOsNativeProxyMount(
        item: AppListItem,
        apply: Boolean,
        onFinished: HyperOsNativeProxyMountCallback?
    ) {
        Thread({
            val plan = HyperOsNativeProxyBindMounter.createPlan(this, item.packageName)
            val result = if (apply)
                HyperOsNativeProxyBindMounter.apply(plan)
            else
                HyperOsNativeProxyBindMounter.unmount(plan)
            DpisLog.i(
                ("Quick HyperOS Native Proxy "
                        + (if (apply) "apply" else "rollback")
                        + " package="
                        + item.packageName
                        + " success="
                        + result.success()
                        + " output="
                        + result.output())
            )
            val messageResId = if (apply)
                R.string.dialog_hyperos_native_proxy_apply_failed
            else
                R.string.dialog_hyperos_native_proxy_unmount_failed
            runOnUiThread {
                if (!result.success()) {
                    showToast(messageResId)
                }
                onFinished?.onFinished(result.success())
            }
        }, "DPIS-Quick-HyperOsNativeProxyMount").start()
    }

    private fun executeDialogProcessAction(
        item: AppListItem?,
        action: ProcessAction
    ) {
        if (action == ProcessAction.RESTART
            && shouldPrepareHyperOsNativeProxyForRestart(item)
        ) {
            // Re-prepare before restart because APK updates can stale the bind mount.
            executeHyperOsNativeProxyMount(
                item!!,
                true,
                object : HyperOsNativeProxyMountCallback {
                    override fun onFinished(success: Boolean) {
                        if (success) {
                            executeDialogProcessActionAfterHyperOsProxyReady(item, action)
                        }
                    }
                }
            )
            return
        }
        executeDialogProcessActionAfterHyperOsProxyReady(item, action)
    }

    private fun shouldPrepareHyperOsNativeProxyForRestart(item: AppListItem?): Boolean {
        if (item == null || !item.hyperOsNativeProxyCandidate) {
            return false
        }
        val store = this.hookConfigStore
        return store != null && store.isTargetDpisEnabled(item.packageName)
                && hasActiveStoredConfig(store, item.packageName)
    }

    private fun executeDialogProcessActionAfterHyperOsProxyReady(
        item: AppListItem?,
        action: ProcessAction
    ) {
        val mappedAction = when (action) {
            ProcessAction.START -> ProcessActionHandler.Action.START
            ProcessAction.RESTART -> ProcessActionHandler.Action.RESTART
            ProcessAction.STOP -> ProcessActionHandler.Action.STOP
        }
        processActionHandler.execute(item, mappedAction)
    }

    private interface HyperOsNativeProxyMountCallback {
        fun onFinished(success: Boolean)
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_PACKAGE_NAME = "com.dpis.module.extra.QUICK_CONFIG_PACKAGE"
        private const val REQUEST_SAVE_FEEDBACK_DIAGNOSTIC = 20024
        private const val SHARED_FEEDBACK_DIAGNOSTIC_DIRECTORY_NAME = "shared-feedback-diagnostics"

        @JvmStatic
        fun createIntent(context: Context?, packageName: String?): Intent {
            val intent = Intent(context, QuickConfigActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (!packageName.isNullOrBlank()) {
                intent.putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
            return intent
        }

        private fun hasActiveStoredConfig(store: DpisConfigStore, packageName: String): Boolean {
            val viewportTargetSpec = store.getTargetViewportSpec(packageName)
            val fontScalePercent = store.getTargetFontScalePercent(packageName)
            return viewportTargetSpec.isEnabled
                    || fontScalePercent != null || store.hasTargetAppSpecificConfig(packageName)
        }
    }
}
