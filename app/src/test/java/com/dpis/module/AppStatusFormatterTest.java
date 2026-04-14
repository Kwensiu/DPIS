package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppStatusFormatterTest {
    @Test
    public void formatsOutOfScopeDisabledState() {
        assertEquals("未注入 · 未启用",
                AppStatusFormatter.format(false, null));
    }

    @Test
    public void formatsInScopeEnabledState() {
        assertEquals("已注入 · 320dp",
                AppStatusFormatter.format(true, 320));
    }
}
