package com.dpis.module;

final class AppStatusFormatter {
    private AppStatusFormatter() {
    }

    static String format(boolean inScope,
                         Integer viewportWidthDp,
                         Integer fontScalePercent,
                         String fontMode) {
        String scopeText = inScope ? "已注入" : "未注入";
        String widthText = viewportWidthDp != null ? viewportWidthDp + "dp" : "未启用";
        String normalizedFontMode = FontApplyMode.normalize(fontMode);
        if (!FontApplyMode.isEnabled(normalizedFontMode) || fontScalePercent == null) {
            return scopeText + " · " + widthText;
        }
        String modeText = FontApplyMode.SYSTEM_EMULATION.equals(normalizedFontMode)
                ? "字体伪装"
                : "字段替换";
        return scopeText + " · " + widthText + " · " + modeText + fontScalePercent + "%";
    }
}
