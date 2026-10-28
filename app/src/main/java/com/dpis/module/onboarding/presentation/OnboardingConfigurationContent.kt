package com.dpis.module.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.dpis.module.R
import com.dpis.module.onboarding.presentation.OnboardingConfigurationPresenter.RestoreStatus
import com.dpis.module.onboarding.presentation.OnboardingConfigurationPresenter.ScopeStatus
import com.dpis.module.settings.presentation.ThemeAppearanceSettingsSection
import com.dpis.module.settings.presentation.ThemeSegmentedSurfaceRow
import com.dpis.module.settings.presentation.ThemeSettingsSection
import com.dpis.module.ui.presentation.design.LocalSpacing
import com.dpis.module.ui.presentation.workspace.AnimatedConditionalItem

@Composable
internal fun ConfigurationStep(
    configurationState: OnboardingConfigurationPresenter.State?,
    onImport: () -> Unit,
    onRequestScope: () -> Unit,
    mode: String,
    dynamicColorEnabled: Boolean,
    themeColor: String,
    paletteStyle: String,
    colorSpecification: String,
    interfaceScalePercent: Int,
    onModeSelected: (String) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onThemeColorSelected: (String) -> Unit,
    onPaletteStyleSelected: (String) -> Unit,
    onColorSpecificationSelected: (String) -> Unit,
    onInterfaceScaleChanged: (Int) -> Unit,
) {
    val spacing = LocalSpacing.current
    val restoreStatus = configurationState?.restoreStatus ?: RestoreStatus.IDLE
    val scopeStatus = configurationState?.scopeStatus ?: ScopeStatus.NONE
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.onboarding_configuration_title),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_configuration_message),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        ThemeSettingsSection(R.string.onboarding_configuration_backup_section) {
            val scopeAvailable = configurationState?.restoredScope?.isNotEmpty() == true
            BackupImportCard(
                status = restoreStatus,
                index = 0,
                total = if (scopeAvailable) 2 else 1,
                onImport = onImport,
            )
            AnimatedConditionalItem(visible = scopeAvailable) {
                ThemeSegmentedSurfaceRow(
                    onClick = onRequestScope,
                    index = 1,
                    total = 2,
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_sparkles_24),
                            contentDescription = stringResource(R.string.restore_scope_prompt_fab),
                        )
                    },
                    content = {
                        Column {
                            Text(
                                stringResource(R.string.onboarding_configuration_scope_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.onboarding_configuration_scope_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    trailingContent = {
                        Text(
                            stringResource(scopeActionLabel(scopeStatus)),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                )
            }
        }
        ThemeAppearanceSettingsSection(
            mode = mode,
            dynamicColorEnabled = dynamicColorEnabled,
            themeColor = themeColor,
            paletteStyle = paletteStyle,
            colorSpecification = colorSpecification,
            interfaceScalePercent = interfaceScalePercent,
            onModeSelected = onModeSelected,
            onDynamicColorChanged = onDynamicColorChanged,
            onThemeColorSelected = onThemeColorSelected,
            onPaletteStyleSelected = onPaletteStyleSelected,
            onColorSpecificationSelected = onColorSpecificationSelected,
            onInterfaceScaleChanged = onInterfaceScaleChanged,
        )
    }
}

@Composable
private fun BackupImportCard(
    status: RestoreStatus,
    index: Int,
    total: Int,
    onImport: () -> Unit,
) {
    ThemeSegmentedSurfaceRow(
        onClick = onImport,
        index = index,
        total = total,
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_upload_file_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        content = {
            Column {
                Text(
                    stringResource(R.string.onboarding_configuration_import_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(restoreStatusLabel(status)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    )
}

private fun restoreStatusLabel(status: RestoreStatus): Int = when (status) {
    RestoreStatus.IDLE -> R.string.onboarding_configuration_import_hint
    RestoreStatus.IMPORTING -> R.string.config_backup_import_in_progress
    RestoreStatus.SUCCEEDED -> R.string.config_backup_import_success
    RestoreStatus.FAILED -> R.string.config_backup_import_failed
}

private fun scopeStatusLabel(status: ScopeStatus): Int = when (status) {
    ScopeStatus.AVAILABLE -> R.string.onboarding_configuration_scope_available
    ScopeStatus.REQUESTING -> R.string.onboarding_configuration_scope_requesting
    ScopeStatus.APPLIED -> R.string.onboarding_configuration_scope_applied
    ScopeStatus.MANUAL -> R.string.onboarding_configuration_scope_manual
    ScopeStatus.PENDING -> R.string.onboarding_configuration_scope_pending
    ScopeStatus.NONE -> R.string.onboarding_configuration_scope_available
}

private fun scopeActionLabel(status: ScopeStatus): Int = when (status) {
    ScopeStatus.AVAILABLE,
    ScopeStatus.PENDING,
        -> R.string.onboarding_configuration_scope_action

    else -> R.string.onboarding_configuration_scope_done
}
