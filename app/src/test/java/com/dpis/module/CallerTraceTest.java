package com.dpis.module;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CallerTraceTest {
    @Test
    public void prefersTargetPackageCaller() {
        String caller = CallerTrace.findRelevantCaller(List.of(
                new StackTraceElement("android.view.Display", "getMetrics", "Display.java", 1),
                new StackTraceElement("com.max.xiaoheihe.ui.HomeActivity", "onCreate",
                        "HomeActivity.java", 42)
        ), "com.max.xiaoheihe");

        assertEquals("com.max.xiaoheihe.ui.HomeActivity#onCreate:42", caller);
    }

    @Test
    public void skipsModuleAndFrameworkFrames() {
        String caller = CallerTrace.findRelevantCaller(List.of(
                new StackTraceElement("com.dpis.module.DisplayHookInstaller", "applyDisplayMetrics",
                        "DisplayHookInstaller.java", 10),
                new StackTraceElement("android.view.Display", "getMetrics", "Display.java", 1),
                new StackTraceElement("java.lang.reflect.Method", "invoke", "Method.java", 2)
        ), "com.max.xiaoheihe");

        assertNull(caller);
    }
}
