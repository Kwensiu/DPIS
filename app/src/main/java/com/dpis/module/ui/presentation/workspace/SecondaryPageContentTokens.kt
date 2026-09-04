package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared rhythm for content following a collapsing secondary-page title. */
internal object SecondaryPageContentTokens {
    val TitleToContentGap = 16.dp
    val SectionLabelToFirstItemGap = 8.dp
    val SectionLabelHorizontalInset = 20.dp
}

@Composable
private fun SectionLabelText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
internal fun ColumnScope.PageSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    SectionLabelText(text, modifier)
    Spacer(Modifier.height(SecondaryPageContentTokens.SectionLabelToFirstItemGap))
}
