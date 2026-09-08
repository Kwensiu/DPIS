package com.dpis.module.templates.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.dpis.module.R

/** Page-centered empty copy for the template list, including search-no-match. */
@Composable
internal fun LazyItemScope.TemplateListEmptyCopy(searching: Boolean) {
    Box(
        modifier = if (searching) {
            Modifier.fillParentMaxSize()
        } else {
            Modifier
                .fillParentMaxWidth()
                .fillParentMaxHeight(TemplateUiTokens.EMPTY_STATE_VIEWPORT_FRACTION)
                .padding(bottom = TemplateUiTokens.EmptyStateBottomBias)
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(
                if (searching) R.string.quick_template_search_empty
                else R.string.template_workspace_quick_templates_empty
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
