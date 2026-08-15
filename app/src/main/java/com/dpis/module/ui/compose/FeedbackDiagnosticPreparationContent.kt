package com.dpis.module.ui.compose

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dpis.module.R

private const val MIN_DIAGNOSTIC_DURATION_SECONDS = 1
private const val MAX_DIAGNOSTIC_DURATION_SECONDS = 86_400
private const val DEFAULT_DIAGNOSTIC_DURATION_SECONDS = 30
private const val LSPOSED_CHECKING = 0
private const val LSPOSED_NO_PERMISSION = 1
private const val LSPOSED_NO_LOGS = 2
private const val LSPOSED_NO_VALID_LOGS = 3
private const val LSPOSED_AVAILABLE = 4

class FeedbackDiagnosticPreparationPresentation(
    initialState: State,
    private val onBack: () -> Unit,
    private val onStart: () -> Unit,
    private val onSave: () -> Unit,
    private val onShare: () -> Unit,
    private val onRefreshRootPermission: () -> Unit,
    private val onRefreshLsposedAvailability: () -> Unit,
    private val onExplainLsposedUnavailable: () -> Unit,
    private val onCopyPackagePath: (String) -> Unit,
) {
    var state: State by mutableStateOf(initialState)
        private set

    fun show(state: State) {
        this.state = state
    }

    fun updateEnvironment(
        rootStatus: String,
        logStatus: String,
        lsposedStatus: String,
        lsposedAvailabilityCode: Int,
        lsposedExplanation: String,
        startEnabled: Boolean,
    ) {
        state = state.copy(
            rootStatus = rootStatus,
            logStatus = logStatus,
            lsposedStatus = lsposedStatus,
            lsposedAvailabilityCode = lsposedAvailabilityCode,
            lsposedExplanation = lsposedExplanation,
            startEnabled = startEnabled,
        )
    }

    fun back() = onBack()

    fun start() = onStart()

    fun save() = onSave()

    fun share() = onShare()

    fun refreshRootPermission() = onRefreshRootPermission()

    fun refreshLsposedAvailability() = onRefreshLsposedAvailability()

    fun explainLsposedAvailability() = onExplainLsposedUnavailable()

    fun lsposedAvailabilityCode(): Int = state.lsposedAvailabilityCode

    fun lsposedExplanation(): String = state.lsposedExplanation

    fun lsposedStatus(): String = state.lsposedStatus

    fun rootStatus(): String = state.rootStatus

    fun logStatus(): String = state.logStatus

    fun isStartEnabled(): Boolean = state.startEnabled

    fun copyPackagePath(path: String) = onCopyPackagePath(path)

    fun isDurationEnabled(): Boolean = state.durationEnabled

    fun selectedDurationSeconds(): Int = state.durationSeconds

    fun setDurationEnabled(enabled: Boolean) {
        state = state.copy(durationEnabled = enabled)
    }

    fun selectDuration(seconds: Int) {
        if (seconds in MIN_DIAGNOSTIC_DURATION_SECONDS..MAX_DIAGNOSTIC_DURATION_SECONDS) {
            state = state.copy(durationSeconds = seconds)
        }
    }

    fun markRecording() {
        state = state.copy(phase = Phase.RECORDING)
    }

    fun markStartFailed() {
        state = state.copy(phase = Phase.PREPARING)
    }

    fun markPackaging() {
        state = state.copy(phase = Phase.PACKAGING)
    }

    fun showReady(
        fileName: String,
        packagePath: String,
        metadata: String,
        outputEntries: List<OutputEntry>,
    ) {
        state = state.copy(
            phase = Phase.READY,
            packageFileName = fileName,
            packagePath = packagePath,
            packageMetadata = metadata,
            outputEntries = outputEntries,
        )
    }

    fun showPackagingFailed() {
        state = state.copy(phase = Phase.PACKAGING_FAILED)
    }

    data class State(
        val appLabel: String,
        val packageName: String,
        val appIcon: Drawable?,
        val versionName: String,
        val rootStatus: String,
        val logStatus: String,
        val lsposedStatus: String,
        val lsposedAvailabilityCode: Int,
        val lsposedExplanation: String,
        val startEnabled: Boolean,
        val phase: Phase = Phase.PREPARING,
        val packageFileName: String = "",
        val packagePath: String = "",
        val packageMetadata: String = "",
        val outputEntries: List<OutputEntry> = emptyList(),
        val durationEnabled: Boolean = false,
        val durationSeconds: Int = DEFAULT_DIAGNOSTIC_DURATION_SECONDS,
    )

    data class OutputEntry(
        val fileName: String,
        val metadata: String,
    )

    enum class Phase {
        PREPARING,
        RECORDING,
        PACKAGING,
        READY,
        PACKAGING_FAILED,
    }
}

@Composable
fun FeedbackDiagnosticPreparationContent(
    presentation: FeedbackDiagnosticPreparationPresentation,
) {
    val state = presentation.state
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        SecondaryPageScaffold(
            titleRes = R.string.feedback_diagnostic_action,
            onBack = presentation::back,
            bottomBar = {
                when (state.phase) {
                    FeedbackDiagnosticPreparationPresentation.Phase.PREPARING -> Button(
                        onClick = presentation::start,
                        enabled = state.startEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Text(stringResource(R.string.feedback_diagnostic_save_and_start_button))
                    }
                    FeedbackDiagnosticPreparationPresentation.Phase.READY -> {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(
                                onClick = presentation::save,
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.feedback_diagnostic_save_action)) }
                            Button(
                                onClick = presentation::share,
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.feedback_diagnostic_share_action)) }
                        }
                    }
                    else -> Unit
                }
            },
        ) { padding ->
            DiagnosticPage(state, presentation, padding)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiagnosticPage(
    state: FeedbackDiagnosticPreparationPresentation.State,
    presentation: FeedbackDiagnosticPreparationPresentation,
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = padding.calculateTopPadding() + 16.dp,
            end = 20.dp,
            bottom = padding.calculateBottomPadding() + 76.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.feedback_diagnostic_preparation_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { DiagnosticSection(R.string.feedback_diagnostic_target_section) { TargetRow(state) } }
        item { EnvironmentSection(state, presentation) }
        item { DiagnosticSessionSection(state, presentation) }
        item { DiagnosticPhaseSection(state, presentation) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DiagnosticSection(
    titleRes: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TargetRow(
    state: FeedbackDiagnosticPreparationPresentation.State,
) {
    val appIcon = rememberInstalledAppIcon(state.packageName, state.appIcon)
    SegmentedListItem(
        onClick = {},
        shapes = dpisSegmentedShapes(0, 1),
        colors = diagnosticItemColors(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { DiagnosticTargetAppIcon(appIcon) },
        content = { Text(state.appLabel, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Column {
                if (state.versionName.isNotBlank()) {
                    Text("v${state.versionName}")
                }
                Text(state.packageName)
            }
        },
    )
}

@Composable
private fun DiagnosticTargetAppIcon(appIcon: Drawable?) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            // A container is only visible while the package icon has not resolved.
            .then(
                if (appIcon == null) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (appIcon != null) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
                },
                update = { it.setImageDrawable(appIcon) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(painterResource(R.drawable.ic_android_24), null)
        }
    }
}

@Composable
private fun EnvironmentSection(
    state: FeedbackDiagnosticPreparationPresentation.State,
    presentation: FeedbackDiagnosticPreparationPresentation,
) {
    DiagnosticSection(R.string.feedback_diagnostic_environment_section) {
        DiagnosticRootPermissionRow(state, presentation)
        DiagnosticLogOutputRow(state.logStatus)
        DiagnosticLsposedRow(state, presentation)
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DiagnosticRootPermissionRow(
    state: FeedbackDiagnosticPreparationPresentation.State,
    presentation: FeedbackDiagnosticPreparationPresentation,
) {
    val enabled = state.phase == FeedbackDiagnosticPreparationPresentation.Phase.PREPARING
    SegmentedListItem(
        onClick = presentation::refreshRootPermission,
        enabled = enabled,
        shapes = dpisSegmentedShapes(0, 3),
        colors = diagnosticItemColors(enabled = enabled),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { Icon(painterResource(R.drawable.ic_shield_24), null) },
        content = { Text(stringResource(R.string.feedback_diagnostic_root_status)) },
        supportingContent = { Text(state.rootStatus) },
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DiagnosticLogOutputRow(status: String) {
    SegmentedListItem(
        onClick = {},
        shapes = dpisSegmentedShapes(1, 3),
        colors = diagnosticItemColors(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { Icon(painterResource(R.drawable.ic_view_kanban_24), null) },
        content = { Text(stringResource(R.string.feedback_diagnostic_log_status)) },
        supportingContent = { Text(status) },
        // This switch represents the session policy. It is visible but not editable here.
        trailingContent = {
            Switch(
                checked = true,
                onCheckedChange = null,
                enabled = false,
                colors = SwitchDefaults.colors(
                    disabledCheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledCheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DiagnosticLsposedRow(
    state: FeedbackDiagnosticPreparationPresentation.State,
    presentation: FeedbackDiagnosticPreparationPresentation,
) {
    val enabled = state.phase == FeedbackDiagnosticPreparationPresentation.Phase.PREPARING
    SegmentedListItem(
        onClick = presentation::refreshLsposedAvailability,
        enabled = enabled,
        shapes = dpisSegmentedShapes(2, 3),
        colors = diagnosticItemColors(enabled = enabled),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { Icon(painterResource(R.drawable.ic_healing_24), null) },
        content = { Text(stringResource(R.string.feedback_diagnostic_lsposed_status)) },
        supportingContent = { Text(state.lsposedStatus) },
        trailingContent = {
            IconButton(
                onClick = presentation::explainLsposedAvailability,
                enabled = enabled,
            ) {
                Icon(
                    painter = painterResource(
                        if (state.lsposedAvailabilityCode == LSPOSED_AVAILABLE) {
                            R.drawable.ic_done_all_24
                        } else {
                            R.drawable.ic_warning_24
                        }
                    ),
                    contentDescription = stringResource(
                        R.string.feedback_diagnostic_lsposed_info_action
                    ),
                )
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DiagnosticSessionSection(
    state: FeedbackDiagnosticPreparationPresentation.State,
    presentation: FeedbackDiagnosticPreparationPresentation,
) {
    val durationItemCount = if (state.durationEnabled) 2 else 1
    val durationControlsEnabled = state.phase == FeedbackDiagnosticPreparationPresentation.Phase.PREPARING
    DiagnosticSection(R.string.feedback_diagnostic_duration_section) {
        SegmentedListItem(
            onClick = {
                if (durationControlsEnabled) {
                    presentation.setDurationEnabled(!state.durationEnabled)
                }
            },
            enabled = durationControlsEnabled,
            shapes = dpisSegmentedShapes(0, durationItemCount),
            colors = diagnosticItemColors(enabled = durationControlsEnabled),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = { Icon(painterResource(R.drawable.ic_hourglass_check_24), null) },
            content = { Text(stringResource(R.string.feedback_diagnostic_duration_toggle_title)) },
            supportingContent = { Text(stringResource(R.string.feedback_diagnostic_duration_toggle_hint)) },
            trailingContent = {
                Switch(
                    checked = state.durationEnabled,
                    onCheckedChange = if (
                        durationControlsEnabled
                    ) presentation::setDurationEnabled else null,
                    enabled = durationControlsEnabled,
                )
            },
        )
        AnimatedConditionalItem(visible = state.durationEnabled) {
            SegmentedListItem(
                onClick = {},
                enabled = durationControlsEnabled,
                shapes = dpisSegmentedShapes(1, durationItemCount),
                colors = diagnosticItemColors(enabled = durationControlsEnabled),
                content = {
                    DurationChipSelector(
                        selectedSeconds = state.durationSeconds,
                        enabled = durationControlsEnabled,
                        onSelect = presentation::selectDuration,
                    )
                },
            )
        }
    }
}

@Composable
private fun DurationChipSelector(
    selectedSeconds: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    var customDialogVisible by rememberSaveable { mutableStateOf(false) }
    val presets = listOf(
        10 to stringResource(R.string.feedback_diagnostic_duration_option, "10s"),
        30 to stringResource(R.string.feedback_diagnostic_duration_option, "30s"),
        60 to stringResource(R.string.feedback_diagnostic_duration_option, "1min"),
        300 to stringResource(R.string.feedback_diagnostic_duration_option, "5min"),
    )
    val customSelected = selectedSeconds !in presets.map { it.first }

    DpisHorizontalScrollWithEdgeFade(
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
            FilterChip(
                selected = customSelected,
                onClick = { customDialogVisible = true },
                enabled = enabled,
                label = {
                    Text(
                        if (customSelected) {
                            "${selectedSeconds}s"
                        } else {
                            stringResource(R.string.feedback_diagnostic_duration_custom)
                        },
                    )
                },
            )
            presets.forEach { (seconds, label) ->
                FilterChip(
                    selected = selectedSeconds == seconds,
                    onClick = { onSelect(seconds) },
                    enabled = enabled,
                    label = { Text(label) },
                )
            }
    }

    if (customDialogVisible) {
        CustomDurationDialog(
            initialSeconds = if (customSelected) selectedSeconds else null,
            onDismiss = { customDialogVisible = false },
            onConfirm = {
                onSelect(it)
                customDialogVisible = false
            },
        )
    }
}

@Composable
private fun CustomDurationDialog(
    initialSeconds: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var input by rememberSaveable(initialSeconds) {
        mutableStateOf(initialSeconds?.toString().orEmpty())
    }
    val seconds = input.toIntOrNull()
    val valid = seconds != null &&
        seconds in MIN_DIAGNOSTIC_DURATION_SECONDS..MAX_DIAGNOSTIC_DURATION_SECONDS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feedback_diagnostic_duration_custom_title)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { value -> input = value.filter(Char::isDigit) },
                label = { Text(stringResource(R.string.feedback_diagnostic_duration_custom_label)) },
                supportingText = {
                    Text(
                        if (input.isNotEmpty() && !valid) {
                            stringResource(R.string.feedback_diagnostic_duration_custom_error)
                        } else {
                            stringResource(R.string.feedback_diagnostic_duration_custom_range)
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(seconds!!) }, enabled = valid) {
                Text(stringResource(R.string.feedback_diagnostic_duration_custom_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.feedback_diagnostic_duration_custom_cancel))
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DiagnosticPhaseSection(
    state: FeedbackDiagnosticPreparationPresentation.State,
    presentation: FeedbackDiagnosticPreparationPresentation,
) {
    DiagnosticSection(R.string.feedback_diagnostic_status_section) {
        when (state.phase) {
            FeedbackDiagnosticPreparationPresentation.Phase.PREPARING -> {
                // Keep the final-result card footprint present from first entry. Only this slot
                // changes state; the introduction and preparation controls never disappear.
                DiagnosticResultPlaceholder()
            }
            FeedbackDiagnosticPreparationPresentation.Phase.RECORDING -> {
                SegmentedListItem(
                    onClick = {},
                    shapes = dpisSegmentedShapes(0, 1),
                    colors = diagnosticItemColors(),
                    content = { Text(stringResource(R.string.feedback_diagnostic_recording_title)) },
                )
            }
            FeedbackDiagnosticPreparationPresentation.Phase.PACKAGING -> {
                SegmentedListItem(
                    onClick = {},
                    shapes = dpisSegmentedShapes(0, 1),
                    colors = diagnosticItemColors(),
                    leadingContent = { Icon(painterResource(R.drawable.ic_overview_24), null) },
                    content = { Text(stringResource(R.string.feedback_diagnostic_packaging_message)) },
                    trailingContent = {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.28f))
                    },
                )
            }
            FeedbackDiagnosticPreparationPresentation.Phase.PACKAGING_FAILED -> {
                DiagnosticStatusRow(
                    R.drawable.ic_bug_report_24,
                    R.string.feedback_diagnostic_save_failed,
                    stringResource(R.string.feedback_diagnostic_save_failed),
                    0,
                    1,
                )
            }
            FeedbackDiagnosticPreparationPresentation.Phase.READY -> {
                DiagnosticOutputDetails(state, presentation)
            }
        }
    }
}

@Composable
private fun DiagnosticOutputDetails(
    state: FeedbackDiagnosticPreparationPresentation.State,
    presentation: FeedbackDiagnosticPreparationPresentation,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.feedback_diagnostic_ready_message),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = state.packageFileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { presentation.copyPackagePath(state.packagePath) }
                    .basicMarquee(animationMode = MarqueeAnimationMode.Immediately),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            Text(
                text = state.packageMetadata,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.outputEntries.forEach { entry ->
                DiagnosticOutputFileCard(entry)
            }
        }
    }
}

@Composable
private fun DiagnosticOutputFileCard(
    entry: FeedbackDiagnosticPreparationPresentation.OutputEntry,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = entry.fileName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = entry.metadata,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DiagnosticOutputFileBackdrop() {
    // The outer output card owns every readable detail. This inset remains a file-card visual
    // anchor only, so it never duplicates the surrounding file names or metadata.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {}
}

@Composable
private fun DiagnosticResultPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.feedback_diagnostic_output_pending),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            DiagnosticOutputFileBackdrop()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DiagnosticStatusRow(
    iconRes: Int,
    titleRes: Int,
    value: String,
    index: Int,
    total: Int,
) {
    SegmentedListItem(
        onClick = {},
        shapes = dpisSegmentedShapes(index, total),
        colors = diagnosticItemColors(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { Icon(painterResource(iconRes), null) },
        content = { Text(stringResource(titleRes)) },
        supportingContent = { Text(value) },
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun diagnosticItemColors(enabled: Boolean = true) = ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface,
    leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledContainerColor = if (enabled) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    },
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLeadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
