package com.dpis.module.ui.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpis.module.R

@Composable
internal fun SupportCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright), border = CardDefaults.outlinedCardBorder()) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
internal fun DonationQrCard(@DrawableRes imageRes: Int, @StringRes titleRes: Int, @StringRes descriptionRes: Int) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright), border = CardDefaults.outlinedCardBorder()) {
        Box {
            Image(painterResource(imageRes), stringResource(descriptionRes), Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface))
            Text(stringResource(titleRes), Modifier.padding(12.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun SupportersSheet() {
    val supporters = listOf(
        R.string.donate_supporter_nickyoung_name to R.string.donate_supporter_nickyoung_amount,
        R.string.donate_supporter_tadow_name to R.string.donate_supporter_tadow_amount,
        R.string.donate_supporter_han_name to R.string.donate_supporter_han_amount,
        R.string.donate_supporter_spine_name to R.string.donate_supporter_spine_amount,
        R.string.donate_supporter_anonymous_name to R.string.donate_supporter_anonymous_amount
    )
    Column(Modifier.fillMaxWidth().heightIn(min = 512.dp).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
        Text(stringResource(R.string.donate_supporters_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.donate_supporters_summary), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        supporters.forEachIndexed { index, (nameRes, amountRes) ->
            Card(Modifier.fillMaxWidth().padding(top = if (index == 0) 12.dp else 8.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(nameRes), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(amountRes), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(stringResource(R.string.donate_supporters_sheet_note), Modifier.fillMaxWidth().padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun GuideSection(@StringRes titleRes: Int, topPadding: Boolean = false) {
    Text(stringResource(titleRes), Modifier.padding(top = if (topPadding) 6.dp else 0.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
internal fun GuideCard(@StringRes titleRes: Int, @StringRes badgeRes: Int?, @StringRes summaryRes: Int, @StringRes pointsRes: Int) {
    SupportCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(titleRes), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            badgeRes?.let { Text(stringResource(it), Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
        }
        Text(stringResource(summaryRes), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(pointsRes), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun FontHooksGuideCard() {
    val routes = listOf(
        R.string.help_tutorial_font_hook_resources_title to R.string.help_tutorial_font_hook_resources_desc,
        R.string.help_tutorial_font_hook_textview_sp_title to R.string.help_tutorial_font_hook_textview_sp_desc,
        R.string.help_tutorial_font_hook_textview_absolute_title to R.string.help_tutorial_font_hook_textview_absolute_desc,
        R.string.help_tutorial_font_hook_textview_current_title to R.string.help_tutorial_font_hook_textview_current_desc,
        R.string.help_tutorial_font_hook_paint_title to R.string.help_tutorial_font_hook_paint_desc,
        R.string.help_tutorial_font_hook_webview_title to R.string.help_tutorial_font_hook_webview_desc,
        R.string.help_tutorial_font_hook_flutter_title to R.string.help_tutorial_font_hook_flutter_desc,
        R.string.help_tutorial_font_hook_hyperos_title to R.string.help_tutorial_font_hook_hyperos_desc
    )
    SupportCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.help_tutorial_font_hooks_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.help_tutorial_font_hooks_badge), Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Text(stringResource(R.string.help_tutorial_font_hooks_summary), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium)
        routes.forEach { (titleRes, descriptionRes) ->
            Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(descriptionRes), Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
