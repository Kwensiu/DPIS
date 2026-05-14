package com.dpis.displaytool;

public final class ComposeRunFields {
    final float composeDensity;
    final float composeFontScale;
    final float composeTextSp;
    final float composeTextPx;
    final int composeLineCount;
    final int composeLayoutW;
    final int composeLayoutH;
    final float composeRenderedScale;
    final int itemIndex;
    final int lazyFirstVisibleIndex;
    final String styleSource;
    final String container;

    public ComposeRunFields(
            float composeDensity,
            float composeFontScale,
            float composeTextSp,
            float composeTextPx,
            int composeLineCount,
            int composeLayoutW,
            int composeLayoutH,
            float composeRenderedScale,
            int itemIndex,
            int lazyFirstVisibleIndex,
            String styleSource,
            String container
    ) {
        this.composeDensity = composeDensity;
        this.composeFontScale = composeFontScale;
        this.composeTextSp = composeTextSp;
        this.composeTextPx = composeTextPx;
        this.composeLineCount = composeLineCount;
        this.composeLayoutW = composeLayoutW;
        this.composeLayoutH = composeLayoutH;
        this.composeRenderedScale = composeRenderedScale;
        this.itemIndex = itemIndex;
        this.lazyFirstVisibleIndex = lazyFirstVisibleIndex;
        this.styleSource = styleSource;
        this.container = container;
    }
}
