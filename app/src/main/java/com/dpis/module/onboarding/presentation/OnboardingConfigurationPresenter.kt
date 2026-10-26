package com.dpis.module.onboarding.presentation

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpis.module.DpisApplication
import com.dpis.module.backup.presentation.ConfigBackupHost
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.delivery.RuntimeConfigDelivery
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.settings.ThemeModeStore
import com.dpis.module.settings.presentation.interfaceScaleStore
import com.dpis.module.templates.BatchScopeRequestCoordinator

class OnboardingConfigurationPresenter(
    private val activity: LocalizedActivity,
) {
    enum class RestoreStatus { IDLE, IMPORTING, SUCCEEDED, FAILED }
    enum class ScopeStatus { NONE, AVAILABLE, REQUESTING, APPLIED, MANUAL, PENDING }

    data class State(
        val restoreStatus: RestoreStatus = RestoreStatus.IDLE,
        val scopeStatus: ScopeStatus = ScopeStatus.NONE,
        val restoredScope: List<String> = emptyList(),
    )

    var state by mutableStateOf(State())
        private set

    data class AppearanceState(
        val mode: String,
        val dynamicColorEnabled: Boolean,
        val themeColor: String,
        val paletteStyle: String,
        val colorSpecification: String,
        val interfaceScalePercent: Int,
    ) {
        companion object {
            val DEFAULT = AppearanceState(
                mode = ThemeModeStore.FOLLOW_SYSTEM,
                dynamicColorEnabled = false,
                themeColor = ThemeModeStore.DEFAULT_STATIC_THEME_COLOR,
                paletteStyle = ThemeModeStore.STYLE_TONAL_SPOT,
                colorSpecification = ThemeModeStore.SPEC_2025,
                interfaceScalePercent = 100,
            )

            fun read(activity: LocalizedActivity): AppearanceState = AppearanceState(
                mode = ThemeModeStore.getMode(activity),
                dynamicColorEnabled = ThemeModeStore.isDynamicColorEnabled(activity),
                themeColor = ThemeModeStore.getThemeColor(activity),
                paletteStyle = ThemeModeStore.getPaletteStyle(activity),
                colorSpecification = ThemeModeStore.getColorSpecification(activity),
                interfaceScalePercent = interfaceScaleStore(activity).percent,
            )
        }
    }

    var appearanceState by mutableStateOf(AppearanceState.DEFAULT)
        private set

    fun refreshAppearance() {
        appearanceState = AppearanceState.read(activity)
    }


    private val backupHost = ConfigBackupHost(
        activity,
        object : ConfigBackupHost.Port {
            override fun configStore(): DpisConfigStore? =
                runCatching { DpisApplication.getActiveHookConfigStore(activity) }
                    .getOrNull()

            override fun showToast(messageResId: Int) {
                Toast.makeText(activity, activity.getString(messageResId), Toast.LENGTH_SHORT)
                    .show()
            }

            override fun publishPresentationState() = Unit

            override fun onRestoreStarted() {
                state = state.copy(restoreStatus = RestoreStatus.IMPORTING)
            }

            override fun onRestoreFailed() {
                state = state.copy(restoreStatus = RestoreStatus.FAILED)
            }

            override fun onRestoreSucceeded(moduleScope: List<String>) {
                RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
                val missingScope = missingScope(moduleScope)
                state = State(
                    restoreStatus = RestoreStatus.SUCCEEDED,
                    scopeStatus = if (missingScope.isEmpty()) {
                        ScopeStatus.NONE
                    } else {
                        ScopeStatus.AVAILABLE
                    },
                    restoredScope = missingScope,
                )
            }

            override fun allowRestoreBeforeRuntimeReady(): Boolean = true

            override fun runOnUiThread(action: Runnable) {
                activity.runOnUiThread(action)
            }
        },
    )

    fun launchImportPicker() = backupHost.launchImportPicker()

    fun selectMode(value: String) {
        ThemeModeStore.setMode(activity, value)
        appearanceState = appearanceState.copy(mode = value)
    }


    fun setDynamicColorEnabled(enabled: Boolean) {
        ThemeModeStore.setDynamicColorEnabled(activity, enabled)
        appearanceState = appearanceState.copy(dynamicColorEnabled = enabled)
    }

    fun selectThemeColor(value: String) {
        ThemeModeStore.setThemeColor(activity, value)
        appearanceState = appearanceState.copy(themeColor = value)
    }

    fun selectPaletteStyle(value: String) {
        ThemeModeStore.setPaletteStyle(activity, value)
        appearanceState = appearanceState.copy(paletteStyle = value)
    }

    fun selectColorSpecification(value: String) {
        ThemeModeStore.setColorSpecification(activity, value)
        appearanceState = appearanceState.copy(colorSpecification = value)
    }

    fun setInterfaceScalePercent(percent: Int): Boolean {
        val normalized = AppUiScaleManager.normalizeScalePercent(percent)
        val store = interfaceScaleStore(activity)
        if (!store.setPercent(normalized)) return false
        activity.markInterfaceScaleAppliedInPlace()
        appearanceState = appearanceState.copy(interfaceScalePercent = normalized)
        return true
    }


    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return backupHost.onActivityResult(requestCode, resultCode, data)
    }

    fun requestRestoreScope() {
        val packages = state.restoredScope
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        if (packages.isEmpty()) return
        val result = BatchScopeRequestCoordinator(
            object : BatchScopeRequestCoordinator.Host {
                override fun showToast(messageResId: Int, vararg formatArgs: Any?) {
                    Toast.makeText(
                        activity,
                        activity.getString(messageResId, *formatArgs),
                        Toast.LENGTH_SHORT,
                    ).show()
                }

                override fun requestAppsLoad() = Unit

                override fun runOnUiThread(runnable: Runnable) = activity.runOnUiThread(runnable)

                override fun onScopeRequestFinished(approved: Boolean, count: Int) {
                    state = state.copy(
                        scopeStatus = if (approved) {
                            ScopeStatus.APPLIED
                        } else {
                            ScopeStatus.MANUAL
                        },
                    )
                }
            },
        ).requestMissingScope(
            packages,
            BatchScopeRequestCoordinator.RESTORE_BACKUP_SCOPE_SOURCE,
        )
        state = state.copy(
            scopeStatus = when {
                result.requestStarted -> ScopeStatus.REQUESTING
                result.manualRequired -> ScopeStatus.MANUAL
                result.affectedPackageCount > 0 -> ScopeStatus.PENDING
                else -> ScopeStatus.APPLIED
            },
        )
    }

    private fun missingScope(moduleScope: List<String>): List<String> {
        val currentScope = runCatching { DpisApplication.xposedService?.scope.orEmpty().toSet() }
            .getOrDefault(emptySet())
        return moduleScope
            .map(String::trim)
            .filter(String::isNotEmpty)
            .filterNot(currentScope::contains)
            .distinct()
    }

}
