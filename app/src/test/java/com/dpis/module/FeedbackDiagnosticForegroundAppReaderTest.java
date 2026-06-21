package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class FeedbackDiagnosticForegroundAppReaderTest {
    @Test
    public void parsePackageReadsResumedActivityComponent() {
        String output = "mResumedActivity: ActivityRecord{"
                + " u0 com.example.target/.MainActivity t42}";

        assertEquals(
                "com.example.target",
                FeedbackDiagnosticForegroundAppReader.parsePackage(output)
        );
    }

    @Test
    public void parsePackageReadsFocusedWindowComponent() {
        String output = "mCurrentFocus=Window{"
                + " u0 com.dpis.module/com.dpis.module.MainActivity}";

        assertEquals(
                "com.dpis.module",
                FeedbackDiagnosticForegroundAppReader.parsePackage(output)
        );
    }

    @Test
    public void parsePackageReturnsEmptyForBlankOutput() {
        assertEquals("", FeedbackDiagnosticForegroundAppReader.parsePackage(""));
    }
}
