package com.dpis.module.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.presentation.design.ComposeDesignSystem
import com.dpis.module.ui.presentation.design.LocalSpacing
import com.dpis.module.ui.presentation.design.Spacing

@Composable
internal fun TermsStep() {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.onboarding_terms_title),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_terms_lead),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TermsPrincipleCard(
            title = stringResource(R.string.onboarding_terms_risk_title),
            body = stringResource(R.string.onboarding_terms_risk_body),
            spacing = spacing,
        )
        TermsPrincipleCard(
            title = stringResource(R.string.onboarding_terms_privacy_title),
            body = stringResource(R.string.onboarding_terms_privacy_body),
            spacing = spacing,
        )
        TermsPrincipleCard(
            title = stringResource(R.string.onboarding_terms_open_source_title),
            body = stringResource(R.string.onboarding_terms_open_source_body),
            spacing = spacing,
        )
        TermsPermissionCard(spacing)
    }
}

@Composable
private fun TermsPrincipleCard(
    title: String,
    body: String,
    spacing: Spacing,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TermsPermissionCard(spacing: Spacing) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column {
            Column(
                modifier = Modifier.padding(spacing.lg),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_terms_permissions_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            TermsPermissionRow(
                title = stringResource(R.string.onboarding_terms_root_runtime_title),
                body = stringResource(R.string.onboarding_terms_root_runtime_body),
                spacing = spacing,
            )
            TermsPermissionRow(
                title = stringResource(R.string.onboarding_terms_root_process_title),
                body = stringResource(R.string.onboarding_terms_root_process_body),
                spacing = spacing,
            )
            TermsPermissionRow(
                title = stringResource(R.string.onboarding_terms_root_diagnostics_title),
                body = stringResource(R.string.onboarding_terms_root_diagnostics_body),
                spacing = spacing,
            )
            TermsPermissionRow(
                title = stringResource(R.string.onboarding_terms_root_scope_title),
                body = stringResource(R.string.onboarding_terms_root_scope_body),
                spacing = spacing,
            )
            TermsPermissionRow(
                title = stringResource(R.string.onboarding_terms_root_font_proxy_title),
                body = stringResource(R.string.onboarding_terms_root_font_proxy_body),
                spacing = spacing,
            )
            TermsPermissionRow(
                title = stringResource(R.string.onboarding_terms_app_list_title),
                body = stringResource(R.string.onboarding_terms_app_list_body),
                spacing = spacing,
            )
            TermsPermissionRow(
                title = stringResource(R.string.onboarding_terms_usage_title),
                body = stringResource(R.string.onboarding_terms_usage_body),
                spacing = spacing,
            )
        }
    }
}

@Composable
private fun TermsPermissionRow(
    title: String,
    body: String,
    spacing: Spacing,
) {
    Column(
        modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TermsPrincipleCardPreview() {
    ComposeDesignSystem(darkTheme = false) {
        TermsPrincipleCard(
            title = stringResource(R.string.onboarding_terms_risk_title),
            body = stringResource(R.string.onboarding_terms_risk_body),
            spacing = LocalSpacing.current,
        )
    }
}
