package com.dpis.module.onboarding.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.ui.presentation.design.LocalSemanticColors
import com.dpis.module.ui.presentation.design.LocalSpacing

internal enum class PermissionKind { ROOT, APP_LIST, USAGE }

@Composable
internal fun RootStep(
    status: RootAccessProbe.Status,
    provider: String,
    checking: Boolean,
    appListAccess: Boolean,
    usageAccess: Boolean,
    onPermissionAction: (PermissionKind) -> Unit,
) {
    val spacing = LocalSpacing.current
    val available = status == RootAccessProbe.Status.AVAILABLE
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.onboarding_permissions_title),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            PermissionCard(
                kind = PermissionKind.ROOT,
                title = stringResource(R.string.onboarding_permission_root_title),
                status = when {
                    checking -> stringResource(R.string.onboarding_permission_checking)
                    available -> stringResource(R.string.onboarding_permission_available, provider)
                    else -> stringResource(R.string.onboarding_permission_unavailable)
                },
                iconRes = if (available) R.drawable.ic_check_circle_24 else R.drawable.ic_build_24,
                authorized = available,
                onAction = { onPermissionAction(PermissionKind.ROOT) },
            )
            PermissionCard(
                kind = PermissionKind.APP_LIST,
                title = stringResource(R.string.onboarding_permission_app_list_title),
                status = permissionStatus(appListAccess),
                iconRes = R.drawable.ic_apps_24,
                authorized = appListAccess,
                onAction = { onPermissionAction(PermissionKind.APP_LIST) },
            )
            PermissionCard(
                kind = PermissionKind.USAGE,
                title = stringResource(R.string.onboarding_permission_usage_title),
                status = permissionStatus(usageAccess),
                optionalLabel = stringResource(R.string.onboarding_permission_optional),
                iconRes = R.drawable.ic_hourglass_check_24,
                authorized = usageAccess,
                onAction = { onPermissionAction(PermissionKind.USAGE) },
            )
        }
    }
}

@Composable
private fun permissionStatus(available: Boolean): String = stringResource(
    if (available) R.string.onboarding_permission_available_short
    else R.string.onboarding_permission_unavailable,
)

@Composable
private fun PermissionCard(
    kind: PermissionKind,
    title: String,
    status: String,
    iconRes: Int,
    authorized: Boolean,
    optionalLabel: String? = null,
    onAction: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val semantic = LocalSemanticColors.current
    val contentColor = if (authorized) semantic.onSuccessContainer
    else MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = if (authorized) semantic.onSuccessContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        color = if (authorized) semantic.successContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding-${kind.name.lowercase()}-card")
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onAction),
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = if (authorized) semantic.onSuccessContainer
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor,
                        )
                        optionalLabel?.let { label ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(
                                        horizontal = spacing.xs,
                                        vertical = 1.dp
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryContentColor,
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right_24),
                    contentDescription = null,
                    tint = secondaryContentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
