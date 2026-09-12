package com.dpis.module.home.presentation

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.compose.SecondaryPageContentTokens
import com.dpis.module.ui.compose.SecondaryPageScaffold
import com.dpis.module.ui.compose.edgeToEdgeContentBottomPadding
import com.dpis.module.ui.compose.setFeatureContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeGuidePage(onBack: () -> Unit) {
    SecondaryPageScaffold(
        titleRes = R.string.mode_guide_title,
        onBack = onBack,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + SecondaryPageContentTokens.TitleToContentGap,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                bottom = edgeToEdgeContentBottomPadding(24.dp),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.mode_guide_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { GuideSection(R.string.mode_help_font_routes_title) }
            item {
                GuideCard(
                    R.string.help_tutorial_system_title,
                    R.string.help_tutorial_system_badge,
                    R.string.help_tutorial_system_summary,
                    R.string.help_tutorial_system_points,
                )
            }
            item {
                GuideCard(
                    R.string.help_tutorial_compat_title,
                    R.string.help_tutorial_compat_badge,
                    R.string.help_tutorial_compat_summary,
                    R.string.help_tutorial_compat_points,
                )
            }
            item { GuideSection(R.string.mode_help_viewport_types_title, topPadding = true) }
            item {
                GuideCard(
                    R.string.help_tutorial_scale_title,
                    R.string.help_tutorial_scale_badge,
                    R.string.help_tutorial_scale_summary,
                    R.string.help_tutorial_scale_points,
                )
            }
            item {
                GuideCard(
                    R.string.help_tutorial_width_title,
                    null,
                    R.string.help_tutorial_width_summary,
                    R.string.help_tutorial_width_points,
                )
            }
            item { GuideSection(R.string.mode_help_font_features_title, topPadding = true) }
            item { FontHooksGuideCard() }
            item {
                GuideCard(
                    R.string.help_tutorial_typeface_title,
                    null,
                    R.string.help_tutorial_typeface_summary,
                    R.string.help_tutorial_typeface_points,
                )
            }
        }
    }
}

fun ComponentActivity.installModeGuide() {
    setFeatureContent {
        ModeGuidePage(onBack = ::finish)
    }
}

@Composable
private fun SupportCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceBright),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun GuideSection(@StringRes titleRes: Int, topPadding: Boolean = false) {
    Text(
        stringResource(titleRes),
        Modifier.padding(top = if (topPadding) 6.dp else 0.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun GuideCard(
    @StringRes titleRes: Int,
    @StringRes badgeRes: Int?,
    @StringRes summaryRes: Int,
    @StringRes pointsRes: Int,
) {
    SupportCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(titleRes),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            badgeRes?.let {
                Text(
                    stringResource(it),
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            stringResource(summaryRes),
            Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            stringResource(pointsRes),
            Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FontHooksGuideCard() {
    val routes = listOf(
        R.string.help_tutorial_font_hook_resources_title to R.string.help_tutorial_font_hook_resources_desc,
        R.string.help_tutorial_font_hook_textview_sp_title to R.string.help_tutorial_font_hook_textview_sp_desc,
        R.string.help_tutorial_font_hook_textview_absolute_title to R.string.help_tutorial_font_hook_textview_absolute_desc,
        R.string.help_tutorial_font_hook_textview_current_title to R.string.help_tutorial_font_hook_textview_current_desc,
        R.string.help_tutorial_font_hook_paint_title to R.string.help_tutorial_font_hook_paint_desc,
        R.string.help_tutorial_font_hook_webview_title to R.string.help_tutorial_font_hook_webview_desc,
        R.string.help_tutorial_font_hook_flutter_title to R.string.help_tutorial_font_hook_flutter_desc,
        R.string.help_tutorial_font_hook_hyperos_title to R.string.help_tutorial_font_hook_hyperos_desc,
    )
    SupportCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.help_tutorial_font_hooks_title),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.help_tutorial_font_hooks_badge),
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            stringResource(R.string.help_tutorial_font_hooks_summary),
            Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        routes.forEach { (titleRes, descriptionRes) ->
            Column(
                Modifier.fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(descriptionRes),
                    Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
