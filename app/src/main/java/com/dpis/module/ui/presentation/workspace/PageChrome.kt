package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Page chrome rhythm. Lists sit on [ContentInset]. Titles and section labels share
 * [TitleInset] beyond that gutter, matching InstallerX's `padding(start = 12.dp)`
 * on the large title.
 */
internal object PageChromeTokens {
    val CompactSlotHeight = 64.dp
    val SearchCardHeight = 52.dp
    val SearchVerticalPadding = 6.dp

    val ContentInset = 16.dp
    val TitleInset = 12.dp
    val ItemSpacing = 12.dp
    val SectionLabelTopGap = 8.dp
    val SectionLabelToItemGap = 16.dp
    val SectionBlockGap = 16.dp
}

internal fun Modifier.pageTitleStart(): Modifier =
    padding(start = PageChromeTokens.TitleInset)

/** Status bars plus display cutout. One definition for chrome and page content. */
@Composable
internal fun pageSafeDrawingInsets(): WindowInsets =
    WindowInsets.systemBars.union(WindowInsets.displayCutout)

@Composable
internal fun pageHorizontalSafeInsets(): WindowInsets =
    pageSafeDrawingInsets().only(WindowInsetsSides.Horizontal)

@Composable
internal fun pageHorizontalSafePadding(enabled: Boolean): PaddingValues =
    if (enabled) pageHorizontalSafeInsets().asPaddingValues() else PaddingValues()

/**
 * Top status inset always. Secondary chrome also takes the correct-side cutout/status
 * inset, then [PageChromeTokens.TitleInset] so the back button sits inset like InstallerX.
 * That extra inset is not repeated on the title slot.
 */
@Composable
internal fun pageTopBarWindowInsets(includeHorizontalSafeInsets: Boolean): WindowInsets {
    val top = WindowInsets.statusBars.only(WindowInsetsSides.Top)
    if (!includeHorizontalSafeInsets) return top
    val extraStart = PageChromeTokens.TitleInset
    val extra = if (LocalLayoutDirection.current == LayoutDirection.Ltr) {
        WindowInsets(left = extraStart)
    } else {
        WindowInsets(right = extraStart)
    }
    return top.union(pageHorizontalSafeInsets()).add(extra)
}

@Composable
internal fun pageCompactTitleStyle(): TextStyle =
    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)

@Composable
internal fun pageExpandedSubtitleStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)

@Composable
internal fun PageSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .pageTitleStart()
            .padding(
                top = PageChromeTokens.SectionLabelTopGap,
                bottom = PageChromeTokens.SectionLabelToItemGap,
            )
            .semantics { heading() },
    )
}
