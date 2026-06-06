package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QuickTemplateApplyConfirmationMessageTest {
    @Test
    public void appendsScopeDisclosureToPlainConfirmation() {
        String message = QuickTemplateApplyConfirmationMessage.format(
                3,
                0,
                strings());

        assertEquals("Apply to 3 apps.\n\nScope may be requested.", message);
    }

    @Test
    public void appendsScopeDisclosureToOverwriteConfirmation() {
        String message = QuickTemplateApplyConfirmationMessage.format(
                5,
                2,
                strings());

        assertEquals(
                "Apply to 5 apps. 2 existing configs will be overwritten.\n\nScope may be requested.",
                message);
    }

    private static QuickTemplateApplyConfirmationMessage.Strings strings() {
        return new QuickTemplateApplyConfirmationMessage.Strings() {
            @Override
            public String plain(int targetCount) {
                return "Apply to " + targetCount + " apps.";
            }

            @Override
            public String overwrite(int targetCount, int overwriteCount) {
                return "Apply to " + targetCount + " apps. "
                        + overwriteCount + " existing configs will be overwritten.";
            }

            @Override
            public String scopeNote() {
                return "Scope may be requested.";
            }
        };
    }
}
