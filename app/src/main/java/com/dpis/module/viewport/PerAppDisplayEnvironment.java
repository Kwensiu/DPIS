package com.dpis.module.viewport;

public final class PerAppDisplayEnvironment {
    public final int widthDp;
    public final int heightDp;
    public final int smallestWidthDp;
    public final int densityDpi;
    public final int widthPx;
    public final int heightPx;

    public PerAppDisplayEnvironment(int widthDp, int heightDp, int smallestWidthDp, int densityDpi,
                             int widthPx, int heightPx) {
        this.widthDp = widthDp;
        this.heightDp = heightDp;
        this.smallestWidthDp = smallestWidthDp;
        this.densityDpi = densityDpi;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
    }
}
