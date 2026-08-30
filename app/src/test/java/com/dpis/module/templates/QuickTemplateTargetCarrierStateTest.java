package com.dpis.module;

import com.dpis.module.templates.QuickTemplateTargetCarrierState;
import com.dpis.module.templates.QuickTemplateTargetSelectionContract;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QuickTemplateTargetCarrierStateTest {
    @Test
    public void portraitPendingTargetsStartsActivityOnce() {
        assertTrue(QuickTemplateTargetCarrierState.shouldStartPortraitActivity(
                false,
                true,
                false
        ));
        assertFalse(QuickTemplateTargetCarrierState.shouldStartPortraitActivity(
                false,
                true,
                true
        ));
    }

    @Test
    public void landscapePendingTargetsDoesNotStartPortraitActivity() {
        assertFalse(QuickTemplateTargetCarrierState.shouldStartPortraitActivity(
                true,
                true,
                false
        ));
    }

    @Test
    public void orientationMigrationResultKeepsPendingTargets() {
        assertFalse(QuickTemplateTargetCarrierState.shouldClearPendingAfterResult(
                false,
                true,
                QuickTemplateTargetCarrierState.CloseReason.ORIENTATION_MIGRATION
        ));
    }

    @Test
    public void userCloseResultsClearPendingTargetsInPortrait() {
        assertTrue(QuickTemplateTargetCarrierState.shouldClearPendingAfterResult(
                false,
                true,
                QuickTemplateTargetCarrierState.CloseReason.USER_BACK
        ));
        assertTrue(QuickTemplateTargetCarrierState.shouldClearPendingAfterResult(
                false,
                true,
                QuickTemplateTargetCarrierState.CloseReason.SAVED
        ));
        assertTrue(QuickTemplateTargetCarrierState.shouldClearPendingAfterResult(
                false,
                true,
                QuickTemplateTargetCarrierState.CloseReason.MISSING_TEMPLATE
        ));
    }

    @Test
    public void landscapeResultDoesNotClearPendingTargets() {
        assertFalse(QuickTemplateTargetCarrierState.shouldClearPendingAfterResult(
                true,
                true,
                QuickTemplateTargetCarrierState.CloseReason.USER_BACK
        ));
    }

    @Test
    public void targetSelectionContractMapsResultReasonsToCarrierReasons() {
        assertSame(
                QuickTemplateTargetCarrierState.CloseReason.ORIENTATION_MIGRATION,
                QuickTemplateTargetSelectionContract.closeReasonFrom(
                        QuickTemplateTargetSelectionContract.CLOSE_REASON_ORIENTATION_MIGRATION));
        assertSame(
                QuickTemplateTargetCarrierState.CloseReason.USER_BACK,
                QuickTemplateTargetSelectionContract.closeReasonFrom(
                        QuickTemplateTargetSelectionContract.CLOSE_REASON_USER_BACK));
        assertSame(
                QuickTemplateTargetCarrierState.CloseReason.SAVED,
                QuickTemplateTargetSelectionContract.closeReasonFrom(
                        QuickTemplateTargetSelectionContract.CLOSE_REASON_SAVED));
        assertSame(
                QuickTemplateTargetCarrierState.CloseReason.MISSING_TEMPLATE,
                QuickTemplateTargetSelectionContract.closeReasonFrom(
                        QuickTemplateTargetSelectionContract.CLOSE_REASON_MISSING_TEMPLATE));
        assertSame(
                QuickTemplateTargetCarrierState.CloseReason.UNKNOWN,
                QuickTemplateTargetSelectionContract.closeReasonFrom(null));
    }
}
