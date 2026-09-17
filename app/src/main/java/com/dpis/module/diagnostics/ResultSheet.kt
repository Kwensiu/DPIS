package com.dpis.module.diagnostics

import android.app.Activity
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.dpis.module.R
import com.dpis.module.ui.compose.ComposeDesignSystem
import com.dpis.module.ui.compose.LocalSpacing
import com.dpis.module.ui.dialog.ComposeOverlay
import com.dpis.module.ui.dialog.ConfirmDialogUiTokens
import com.dpis.module.ui.dialog.DialogChrome
import com.dpis.module.ui.dialog.ModalDialog
import com.dpis.module.ui.dialog.ModalSheet

/** Compose-owned diagnostic result sheet; package creation and file actions remain host-owned. */
class ResultSheet(
    private val activity: Activity?,
    private val host: Host?
) {
    interface Host {
        fun shareFeedbackDiagnostic(
            diagnosticPackage: ExportBuilder.DiagnosticPackage
        )

        fun saveFeedbackDiagnostic(
            diagnosticPackage: ExportBuilder.DiagnosticPackage
        )
    }
    fun show(diagnosticPackage: ExportBuilder.DiagnosticPackage?) {
        val activity = activity ?: return
        val host = host ?: return
        val result = diagnosticPackage?.result ?: return
        ComposeOverlay.show(activity) { dismiss ->
            ModalSheet(onDismissRequest = dismiss) {
                FeedbackDiagnosticResultContent(
                    title = activity.getString(
                        R.string.feedback_diagnostic_result_title,
                        result.request.label
                    ),
                    packageLine = activity.getString(
                        R.string.feedback_diagnostic_result_package_line,
                        valueOrUnknown(result.request.packageName)
                    ),
                    versionLine = activity.getString(
                        R.string.feedback_diagnostic_result_version_line,
                        valueOrUnknown(result.request.versionName)
                    ),
                    entries = diagnosticPackage.entries.map {
                        DiagnosticEntryUi(
                            it.name,
                            if (it.hasLineCount) {
                                activity.getString(
                                    R.string.feedback_diagnostic_result_entry_meta,
                                    it.lineCount,
                                    Formatter.formatFileSize(activity, it.byteCount.toLong())
                                )
                            } else {
                                Formatter.formatFileSize(activity, it.byteCount.toLong())
                            }
                        )
                    },
                    onSave = {
                        dismiss()
                        host.saveFeedbackDiagnostic(diagnosticPackage)
                    },
                    onShare = {
                        dismiss()
                        host.shareFeedbackDiagnostic(diagnosticPackage)
                    }
                )
            }
        }
    }

    private fun valueOrUnknown(value: String?): String {
        return value?.trim()?.takeIf(String::isNotEmpty)
            ?: activity!!.getString(R.string.feedback_diagnostic_result_unknown)
    }
}

internal data class DiagnosticEntryUi(val name: String, val metadata: String)

@Composable
internal fun FeedbackDiagnosticResultContent(
    title: String,
    packageLine: String,
    versionLine: String,
    entries: List<DiagnosticEntryUi>,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(
            start = DialogChrome.HorizontalPadding,
            top = 14.dp,
            end = DialogChrome.HorizontalPadding,
            bottom = spacing.xl,
        )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = packageLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = versionLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEach { entry ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = entry.metadata,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.feedback_diagnostic_result_privacy_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSave,
                modifier = Modifier.weight(1f).height(ConfirmDialogUiTokens.ActionHeight),
                shape = ConfirmDialogUiTokens.ActionShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(stringResource(R.string.feedback_diagnostic_save_action))
            }
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f).height(ConfirmDialogUiTokens.ActionHeight),
                shape = ConfirmDialogUiTokens.ActionShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(stringResource(R.string.feedback_diagnostic_share_action))
            }
        }
    }
}

/** Shared Compose progress dialog used while the diagnostic ZIP is being built. */
object PackagingDialog {
    fun show(activity: Activity): ComposeOverlay = ComposeOverlay.show(activity) { _ ->
        ModalDialog(
            onDismissRequest = {},
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        ) {
            FeedbackDiagnosticPackagingContent()
        }
    }
}

@Composable
internal fun FeedbackDiagnosticPackagingContent() {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(DialogChrome.SurfacePadding)
    ) {
        Text(
            text = stringResource(R.string.feedback_diagnostic_packaging_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(spacing.md))
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Spacer(Modifier.height(spacing.md))
        Text(
            text = stringResource(R.string.feedback_diagnostic_packaging_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackDiagnosticResultContentPreview() {
    ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
        FeedbackDiagnosticResultContent(
            title = "Diagnostic package ready: Demo",
            packageLine = "Package: example.app",
            versionLine = "Version: 1.0",
            entries = listOf(
                DiagnosticEntryUi("diagnostic.txt", "42 lines - 2048 bytes"),
                DiagnosticEntryUi("dpis-log.txt", "18 lines - 900 bytes")
            ),
            onSave = {},
            onShare = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun FeedbackDiagnosticPackagingContentDarkPreview() {
    ComposeDesignSystem(darkTheme = true, dynamicColor = false) {
        FeedbackDiagnosticPackagingContent()
    }
}
