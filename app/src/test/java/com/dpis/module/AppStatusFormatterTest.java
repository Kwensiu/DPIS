package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppStatusFormatterTest {
    @Test
    public void formatsOutOfScopeDisabledState() {
        assertEquals("未注入 | 未启用",
                AppStatusFormatter.format(false, null, null, null, FontApplyMode.OFF, true));
    }

    @Test
    public void formatsInScopeEnabledState() {
        assertEquals("已注入 | 320dp(伪装) | 字体115%(伪装)",
                AppStatusFormatter.format(
                        true,
                        320,
                        ViewportApplyMode.SYSTEM_EMULATION,
                        115,
                        FontApplyMode.SYSTEM_EMULATION,
                        true));
    }

    @Test
    public void formatsFontOnlyState() {
        assertEquals("未注入 | 未启用 | 字体110%(替换)",
                AppStatusFormatter.format(
                        false,
                        null,
                        ViewportApplyMode.OFF,
                        110,
                        FontApplyMode.FIELD_REWRITE,
                        true));
    }

    @Test
    public void formatsDpisDisabledState() {
        assertEquals("已注入 | 已禁用",
                AppStatusFormatter.format(
                        true,
                        360,
                        ViewportApplyMode.SYSTEM_EMULATION,
                        120,
                        FontApplyMode.SYSTEM_EMULATION,
                        false));
    }
}
