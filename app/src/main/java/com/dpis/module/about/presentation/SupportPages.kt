package com.dpis.module.ui.compose

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpis.module.R

/**
 * Compose presentation for standalone support activities. Activity classes retain
 * locale/scale wrapping and Intent contracts; this file owns only rendered state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateSupportPage(onBack: () -> Unit) {
    var supportersVisible by remember { mutableStateOf(false) }
    SecondaryPageScaffold(titleRes = R.string.donate_title, onBack = onBack) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SupportCard {
                    Text(stringResource(R.string.donate_message), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.donate_trust_note),
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                SupportCard(
                    modifier = Modifier.dpisClickable(onClick = { supportersVisible = true })
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.donate_supporters_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.donate_supporters_summary),
                                modifier = Modifier.padding(top = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { DonationQrCard(R.drawable.donate_wechat, R.string.donate_wechat_title, R.string.donate_wechat_qr_description) }
            item { DonationQrCard(R.drawable.donate_alipay, R.string.donate_alipay_title, R.string.donate_alipay_qr_description) }
        }
    }
    if (supportersVisible) {
        ModalBottomSheet(
            onDismissRequest = { supportersVisible = false },
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
            ),
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.fillMaxWidth()) {
                DpisSheetVisualChrome()
                SupportersSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeHelpPage(onBack: () -> Unit, onOpenModeGuide: () -> Unit) {
    SecondaryPageScaffold(
        titleRes = R.string.mode_help_title,
        onBack = onBack
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(stringResource(R.string.mode_help_tips_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Spacer(
                        Modifier.width(3.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(stringResource(R.string.mode_help_tip_font_lag_question), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.mode_help_tip_font_lag_steps), modifier = Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.mode_help_tip_font_lag_reason), modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Text(stringResource(R.string.mode_help_more_title), modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().dpisClickable(onClick = onOpenModeGuide),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.mode_help_mode_guide_entry_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.mode_help_mode_guide_entry_summary), modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(painterResource(R.drawable.ic_chevron_right_24), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeGuidePage(onBack: () -> Unit) {
    SecondaryPageScaffold(
        titleRes = R.string.mode_guide_title,
        onBack = onBack
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.mode_guide_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item { GuideSection(R.string.mode_help_font_routes_title) }
            item { GuideCard(R.string.help_tutorial_system_title, R.string.help_tutorial_system_badge, R.string.help_tutorial_system_summary, R.string.help_tutorial_system_points) }
            item { GuideCard(R.string.help_tutorial_compat_title, R.string.help_tutorial_compat_badge, R.string.help_tutorial_compat_summary, R.string.help_tutorial_compat_points) }
            item { GuideSection(R.string.mode_help_viewport_types_title, topPadding = true) }
            item { GuideCard(R.string.help_tutorial_scale_title, R.string.help_tutorial_scale_badge, R.string.help_tutorial_scale_summary, R.string.help_tutorial_scale_points) }
            item { GuideCard(R.string.help_tutorial_width_title, null, R.string.help_tutorial_width_summary, R.string.help_tutorial_width_points) }
            item { GuideSection(R.string.mode_help_font_features_title, topPadding = true) }
            item { FontHooksGuideCard() }
            item { GuideCard(R.string.help_tutorial_typeface_title, null, R.string.help_tutorial_typeface_summary, R.string.help_tutorial_typeface_points) }
        }
    }
}
