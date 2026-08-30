package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dpis.module.R

/**
 * Standalone empty-state page kept separate from SettingsWorkspaceContent because
 * experimental settings still launch through their own manifest Activity contract.
 */
@Composable
fun ExperimentalSettingsContent(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecondaryPageScaffold(
        titleRes = R.string.settings_experimental_title,
        onBack = onBack,
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(modifier)
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.settings_experimental_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
