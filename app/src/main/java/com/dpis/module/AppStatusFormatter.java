package com.dpis.module;

final class AppStatusFormatter {
    private AppStatusFormatter() {
    }

    static String format(boolean inScope, Integer viewportWidthDp) {
        String scopeText = inScope ? "已注入" : "未注入";
        String widthText = viewportWidthDp != null ? viewportWidthDp + "dp" : "未启用";
        return scopeText + " · " + widthText;
    }
}
