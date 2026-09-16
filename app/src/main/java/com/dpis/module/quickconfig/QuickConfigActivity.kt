package com.dpis.module.quickconfig

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.dpis.module.DpisApplication
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigProcessAction
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.ForegroundPackageResolver
import com.dpis.module.applist.InstalledAppCatalogPolicy
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.hyperos.HyperOsNativeAppDetector
import com.dpis.module.runtime.hyperos.HyperOsNativeProxyFacade
import com.dpis.module.process.presentation.ProcessActionHandler
import com.dpis.module.quickconfig.presentation.QuickConfigComposeEditor
import com.dpis.module.quickconfig.presentation.QuickConfigDiagnosticSession
import com.dpis.module.quickconfig.presentation.QuickConfigDialog
import com.dpis.module.quickconfig.presentation.QuickConfigPresentation
import com.dpis.module.quickconfig.presentation.installQuickConfig
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.runtime.delivery.RuntimeConfigDelivery
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.hooks.SystemScopeCoordinator
import com.dpis.module.viewport.ViewportPropertySyncer

class QuickConfigActivity : LocalizedActivity() {
    private val appConfigSaveHandler = AppConfigSaveHandler()
    private val processActionHandler = ProcessActionHandler(
        this,
        { packageName -> syncRuntimePropertiesForTargetLaunch(packageName) },
        { actionLabel, appLabel, onConfirm ->
            presentation?.show(
                QuickConfigDialog.ProcessAction(actionLabel, appLabel) { onConfirm.run() },
            )
        },
    )
    private val systemScopeCoordinator = SystemScopeCoordinator(createSystemScopeHost())
    private val hyperOsNativeProxy = HyperOsNativeProxyFacade(
        this,
        { showToast(it) },
        { runOnUiThread(it) },
    )
    internal var presentation: QuickConfigPresentation? = null
    private lateinit var editor: QuickConfigComposeEditor
    private lateinit var diagnostics: QuickConfigDiagnosticSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presentation = QuickConfigPresentation()
        installQuickConfig(presentation!!)
        diagnostics = QuickConfigDiagnosticSession(
            this,
            persistCurrentConfig = { item -> editor.saveCurrentForDiagnostic(item) },
            syncRuntimeForLaunch = { packageName -> syncRuntimePropertiesForTargetLaunch(packageName) },
        )
        editor = QuickConfigComposeEditor(
            this,
            appConfigSaveHandler,
            systemScopeCoordinator,
            onProcessAction = { item, action -> executeDialogProcessAction(item, action) },
            onStartDiagnostic = { item, draft -> diagnostics.start(item, draft) },
        )

        val retainedSession = lastCustomNonConfigurationInstance as QuickConfigEditorSession?
        if (retainedSession != null) {
            editor.restore(retainedSession)
            return
        }

        val explicitPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        val usageAccessGranted = ForegroundPackageResolver.hasUsageAccess(this)
        val targetDecision = QuickConfigTargetDecision.decide(
            explicitPackageName,
            usageAccessGranted,
            if (usageAccessGranted) ForegroundPackageResolver.resolve(this) else null,
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
        editor.open(item)
    }

    override fun onRetainCustomNonConfigurationInstance(): Any? = editor.retain()

    private fun openUsageAccessSettings() {
        val packageSettings = Intent(
            Settings.ACTION_USAGE_ACCESS_SETTINGS,
            Uri.parse("package:$packageName"),
        )
        try {
            startActivity(packageSettings)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, R.string.quick_config_target_unavailable, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        RootAccessProbe.refreshAsync(null)
        diagnostics.onResume()
    }

    override fun onStop() {
        diagnostics.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        diagnostics.onDestroy()
        super.onDestroy()
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        diagnostics.handleActivityResult(requestCode, resultCode, data)
    }

    private fun createItem(packageName: String): AppListItem? {
        try {
            val packageManager = packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val label = packageManager.getApplicationLabel(applicationInfo).toString()
            val icon = applicationInfo.loadIcon(packageManager)
            val systemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            return InstalledAppCatalogPolicy.createAppListItem(
                hookConfigStore,
                loadScopePackages(),
                DpisApplication.xposedService != null,
                label,
                packageName,
                systemApp,
                HyperOsNativeAppDetector.isNativeProxyCandidate(applicationInfo),
                true,
                icon,
            )
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        } catch (_: RuntimeException) {
            return null
        }
    }

    private fun loadScopePackages(): Set<String> {
        val service = DpisApplication.xposedService ?: return emptySet()
        return try {
            HashSet(service.scope)
        } catch (_: RuntimeException) {
            emptySet()
        }
    }

    private fun createSystemScopeHost(): SystemScopeCoordinator.Host =
        object : SystemScopeCoordinator.Host {
            override fun showToast(messageResId: Int, vararg formatArgs: Any?) {
                Toast.makeText(
                    this@QuickConfigActivity,
                    getString(messageResId, *formatArgs),
                    Toast.LENGTH_SHORT,
                ).show()
            }

            override fun requestAppsLoad() {
            }

            override fun runOnUiThread(runnable: Runnable) {
                this@QuickConfigActivity.runOnUiThread(runnable)
            }
        }

    internal val isSystemHookEnabled: Boolean
        get() {
            val store = hookConfigStore
            return store != null && store.isSystemServerHooksEnabled()
        }

    internal val hookConfigStore: DpisConfigStore?
        get() = DpisApplication.getActiveHookConfigStore(this)

    internal fun publishAfterSave(packageName: String?) {
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        val store = hookConfigStore
        ViewportPropertySyncer.syncTarget(packageName, store)
        FontRuntimePropertySyncer.syncTarget(packageName, store)
    }

    private fun syncRuntimePropertiesForTargetLaunch(packageName: String?) {
        publishAfterSave(packageName)
    }

    internal fun resolvePackageVersionName(packageName: String?): String? {
        if (packageName.isNullOrBlank()) {
            return ""
        }
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    private fun executeDialogProcessAction(
        item: AppListItem?,
        action: AppConfigProcessAction,
    ) {
        // Re-prepare before restart because APK updates can stale the bind mount.
        hyperOsNativeProxy.runAfterOptionalRestartPrepare(
            item,
            hookConfigStore,
            action == AppConfigProcessAction.RESTART,
        ) {
            executeDialogProcessActionAfterHyperOsProxyReady(item, action)
        }
    }

    private fun executeDialogProcessActionAfterHyperOsProxyReady(
        item: AppListItem?,
        action: AppConfigProcessAction,
    ) {
        val mappedAction = when (action) {
            AppConfigProcessAction.START -> ProcessActionHandler.Action.START
            AppConfigProcessAction.RESTART -> ProcessActionHandler.Action.RESTART
            AppConfigProcessAction.STOP -> ProcessActionHandler.Action.STOP
        }
        if (item != null) {
            processActionHandler.execute(item, mappedAction)
        }
    }

    internal fun showToast(messageResId: Int) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_PACKAGE_NAME = "com.dpis.module.extra.QUICK_CONFIG_PACKAGE"

        @JvmStatic
        fun createIntent(context: Context?, packageName: String?): Intent {
            val intent = Intent(context, QuickConfigActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (!packageName.isNullOrBlank()) {
                intent.putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
            return intent
        }
    }
}
