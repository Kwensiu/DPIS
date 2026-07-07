package com.dpis.module.templates;

import com.dpis.module.templates.QuickTemplateApplyConfirmationMessage;

public final class QuickTemplateApplyConfirmationMessage {
    public interface Strings {
        String plain(int targetCount);

        String overwrite(int targetCount, int overwriteCount);

        String scopeNote();
    }

    private QuickTemplateApplyConfirmationMessage() {
    }

    public static String format(int targetCount, int overwriteCount, Strings strings) {
        String base = overwriteCount > 0
                ? strings.overwrite(targetCount, overwriteCount)
                : strings.plain(targetCount);
        String scopeNote = strings.scopeNote();
        if (scopeNote == null || scopeNote.isBlank()) {
            return base;
        }
        return base + "\n\n" + scopeNote;
    }
}
