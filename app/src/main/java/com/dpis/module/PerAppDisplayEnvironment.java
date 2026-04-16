package com.dpis.module;

final class PerAppDisplayEnvironment {
    final int widthDp;
    final int heightDp;
    final int smallestWidthDp;
    final int densityDpi;
    final int widthPx;
    final int heightPx;

    PerAppDisplayEnvironment(int widthDp, int heightDp, int smallestWidthDp, int densityDpi,
                             int widthPx, int heightPx) {
        this.widthDp = widthDp;
        this.heightDp = heightDp;
        this.smallestWidthDp = smallestWidthDp;
        this.densityDpi = densityDpi;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
    }
}
