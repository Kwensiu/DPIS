package com.dpis.module.templates;

/**
 * Navigation contract for the manifest-owned quick-template target picker.
 */
public final class QuickTemplateTargetSelectionContract {
    public static final String EXTRA_TEMPLATE_ID = "quick_template_targets.template_id";
    public static final String EXTRA_CLOSE_REASON = "quick_template_targets.close_reason";

    public static final String CLOSE_REASON_ORIENTATION_MIGRATION = "orientation_migration";
    public static final String CLOSE_REASON_USER_BACK = "user_back";
    public static final String CLOSE_REASON_SAVED = "saved";
    public static final String CLOSE_REASON_MISSING_TEMPLATE = "missing_template";

    private QuickTemplateTargetSelectionContract() {
    }

    public static QuickTemplateTargetCarrierState.CloseReason closeReasonFrom(String reason) {
        if (CLOSE_REASON_ORIENTATION_MIGRATION.equals(reason)) {
            return QuickTemplateTargetCarrierState.CloseReason.ORIENTATION_MIGRATION;
        }
        if (CLOSE_REASON_USER_BACK.equals(reason)) {
            return QuickTemplateTargetCarrierState.CloseReason.USER_BACK;
        }
        if (CLOSE_REASON_SAVED.equals(reason)) {
            return QuickTemplateTargetCarrierState.CloseReason.SAVED;
        }
        if (CLOSE_REASON_MISSING_TEMPLATE.equals(reason)) {
            return QuickTemplateTargetCarrierState.CloseReason.MISSING_TEMPLATE;
        }
        return QuickTemplateTargetCarrierState.CloseReason.UNKNOWN;
    }
}
