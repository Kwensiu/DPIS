package com.dpis.module.ui.compose

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.DialogWindowEdgeToEdge
import com.dpis.module.ui.DialogWindowSizer
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun interface FilterSelectionListener {
    fun onChanged(showSystem: Boolean, injectedOnly: Boolean, widthOnly: Boolean, fontOnly: Boolean)
}

object AppFilterComposeSheet {
    @JvmStatic
    fun show(activity: Activity, showSystem: Boolean, injectedOnly: Boolean,
        widthOnly: Boolean, fontOnly: Boolean, listener: FilterSelectionListener): BottomSheetDialog {
        val view = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        val dialog = BottomSheetDialog(activity)
        view.setContent {
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
                AppFilterContent(showSystem, injectedOnly, widthOnly, fontOnly, listener::onChanged)
            }
        }
        dialog.setContentView(view)
        dialog.show()
        DialogWindowEdgeToEdge.apply(dialog)
        return dialog
    }
}

@Composable
internal fun AppFilterContent(showSystemInitial: Boolean, injectedInitial: Boolean,
    widthInitial: Boolean, fontInitial: Boolean,
    onChanged: (Boolean, Boolean, Boolean, Boolean) -> Unit) {
    var showSystem by remember { mutableStateOf(showSystemInitial) }
    var injected by remember { mutableStateOf(injectedInitial) }
    var width by remember { mutableStateOf(widthInitial) }
    var font by remember { mutableStateOf(fontInitial) }
    fun publish() = onChanged(showSystem, injected, width, font)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(
        horizontal = dimensionResource(R.dimen.filter_sheet_padding_horizontal),
        vertical = dimensionResource(R.dimen.filter_sheet_padding_vertical))) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.size(dimensionResource(R.dimen.filter_sheet_drag_handle_width),
                dimensionResource(R.dimen.filter_sheet_drag_handle_height)),
                shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.outlineVariant,
                content = {})
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.filter_sheet_title_spacing_top)))
        Text(stringResource(R.string.filter_sheet_title), style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(dimensionResource(R.dimen.filter_sheet_first_switch_spacing_top)))
        FilterSwitch(R.string.filter_show_system_apps, showSystem, "filter_show_system") { showSystem = it; publish() }
        FilterSwitch(R.string.filter_scoped_only, injected) { injected = it; publish() }
        FilterSwitch(R.string.filter_width_only, width) { width = it; publish() }
        FilterSwitch(R.string.filter_font_only, font) { font = it; publish() }
    }
}

@Composable
private fun FilterSwitch(label: Int, checked: Boolean, tag: String = "", onChecked: (Boolean) -> Unit) {
    androidx.compose.material3.ListItem(
        trailingContent = { Switch(checked = checked, onCheckedChange = onChecked,
            modifier = if (tag.isEmpty()) Modifier else Modifier.testTag(tag)) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(label))
    }
}

object ModuleRuntimeReloadComposeDialog {
    fun show(activity: Activity, onDismissed: Runnable): AlertDialog {
        val view = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        val dialog = MaterialAlertDialogBuilder(activity).setView(view).create()
        view.setContent {
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
                RuntimeReloadNoticeContent(dialog::dismiss)
            }
        }
        dialog.setOnDismissListener { onDismissed.run() }
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        DialogWindowEdgeToEdge.apply(dialog)
        DialogWindowSizer.applyStandardWidth(dialog, activity)
        return dialog
    }
}

@Composable
internal fun RuntimeReloadNoticeContent(onAcknowledge: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(
        horizontal = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
        vertical = dimensionResource(R.dimen.dialog_surface_padding_top)),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(painterResource(R.drawable.ic_error_outline_24), null,
                Modifier.padding(dimensionResource(R.dimen.dialog_status_icon_padding)).size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_body_spacing)))
        Text(stringResource(R.string.module_runtime_reload_title),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_body_spacing)))
        Text(stringResource(R.string.module_runtime_reload_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
        Button(onClick = rememberClickAction(onAcknowledge), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.module_runtime_reload_ack_button))
        }
    }
}
