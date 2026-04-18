package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppStatusFormatterTest {
    @Test
    public void formatsOutOfScopeDisabledState() {
        assertEquals("未注入 · 未启用",
                AppStatusFormatter.format(false, null, null, FontApplyMode.OFF));
    }

    @Test
    public void formatsInScopeEnabledState() {
        assertEquals("已注入 · 320dp · 字体伪装115%",
                AppStatusFormatter.format(true, 320, 115, FontApplyMode.SYSTEM_EMULATION));
    }

    @Test
    public void formatsFontOnlyState() {
        assertEquals("未注入 · 未启用 · 字段替换110%",
                AppStatusFormatter.format(false, null, 110, FontApplyMode.FIELD_REWRITE));
    }
}
