package com.dpis.module.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dpis.module.R

/** Shared title, body, and action layout for content hosted by [ModalDialog]. */
@Composable
internal fun DialogColumn(
    title: @Composable () -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.9f)
        .coerceAtLeast(240f)
        .dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .padding(
                start = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
                top = dimensionResource(R.dimen.dialog_surface_padding_top),
                end = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
                bottom = dimensionResource(R.dimen.dialog_surface_padding_bottom),
            ),
    ) {
        title()
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            content = content,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_footer_spacing_top)))
        actions()
    }
}

@Composable
internal fun DialogTitle(text: String, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = textAlign,
    )
}
