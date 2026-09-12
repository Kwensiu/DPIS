package com.dpis.module.ui.compose

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class PageChromeTest {
    @Test
    fun searchCardFitsTheCompactChromeSlot() {
        assertEquals(
            PageChromeTokens.CompactSlotHeight,
            PageChromeTokens.SearchVerticalPadding * 2
                + PageChromeTokens.SearchCardHeight,
        )
        assertEquals(64.dp, PageChromeTokens.CompactSlotHeight)
        assertEquals(52.dp, PageChromeTokens.SearchCardHeight)
        assertEquals(6.dp, PageChromeTokens.SearchVerticalPadding)
    }

    @Test
    fun titlesAndSectionLabelsShareOneStartInsetBeyondTheListGutter() {
        assertEquals(16.dp, PageChromeTokens.ContentInset)
        assertEquals(12.dp, PageChromeTokens.TitleInset)
        assertEquals(12.dp, PageChromeTokens.ItemSpacing)
        assertEquals(8.dp, PageChromeTokens.SectionLabelTopGap)
        assertEquals(16.dp, PageChromeTokens.SectionLabelToItemGap)
        assertEquals(16.dp, PageChromeTokens.SectionBlockGap)
    }
}
