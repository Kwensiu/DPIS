package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dpis.module.templates.TemplateConfigValue;
import com.dpis.module.templates.TemplateEditorForm;
import com.dpis.module.templates.TemplateEditorDraft;
import com.dpis.module.templates.QuickTemplateStore;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

import org.junit.Test;

/** Locks the draft rules shared by the portrait sheet and landscape detail editor. */
public final class TemplateEditorFormTest {
    @Test
    public void switchingViewportTargetRetainsBothDraftInputs() {
        TemplateEditorForm form = TemplateEditorForm.global(TemplateConfigValue.EMPTY);
        form.viewportInput = "125";

        form.switchViewportMode(ViewportTargetType.ABSOLUTE_DP);
        form.viewportInput = "411";
        form.switchViewportMode(ViewportTargetType.RELATIVE_SCALE);

        assertEquals("125", form.viewportInput);
        assertEquals("125", form.viewportScaleInput);
        assertEquals("411", form.viewportAbsoluteInput);
    }

    @Test
    public void resetRestoresPersistedDefaultWithoutDiscardingEditorIdentity() {
        TemplateEditorForm form = TemplateEditorForm.quick(
                new QuickTemplateStore.QuickTemplate(
                        "template-id", "Display", 1L, null, TemplateConfigValue.EMPTY),
                "unused-id");
        form.viewportInput = "120";

        form.reset();

        assertEquals("template-id", form.templateId);
        assertFalse(form.newTemplate);
        assertFalse(form.isDirty());
        assertEquals(ViewportTargetType.RELATIVE_SCALE, form.viewportMode);
        assertEquals("", form.viewportInput);
    }

    @Test
    public void newQuickTemplateRequiresNameBeforeItCanSave() {
        TemplateEditorForm form = TemplateEditorForm.quick(null, "new-id");

        assertTrue(form.newTemplate);
        assertFalse(form.isNameValid());
        assertFalse(form.isValid());

        form.nameInput = "Template";
        assertTrue(form.isNameValid());
        assertTrue(form.isValid());
    }

    @Test
    public void invalidNumericDraftExposesFieldValidityWithoutChangingSaveRules() {
        TemplateEditorForm form = TemplateEditorForm.global(TemplateConfigValue.EMPTY);

        form.viewportInput = "not-a-number";
        assertFalse(form.isViewportInputValid());
        assertFalse(form.isValid());

        form.viewportInput = "";
        form.fontInput = "301";
        assertTrue(form.isViewportInputValid());
        assertFalse(form.isFontInputValid());
        assertFalse(form.isValid());
    }

    @Test
    public void emptySystemFontDraftIsValidAndStoredOffIsPresentedAsSystemMode() {
        TemplateEditorForm form = TemplateEditorForm.global(TemplateConfigValue.EMPTY);

        assertEquals(FontApplyMode.SYSTEM_EMULATION, form.fontMode);
        assertEquals("", form.fontInput);
        assertTrue(form.isValid());
    }

    @Test
    public void markSavedClearsDirtyBaselineAndAdoptsCreatedTemplateId() {
        TemplateEditorForm form = TemplateEditorForm.quick(null, "new-id");
        form.nameInput = "Template";
        form.viewportInput = "120";
        form.updateActiveViewportDraft();

        form.markSaved("generated-id");

        assertEquals("generated-id", form.templateId);
        assertFalse(form.newTemplate);
        assertFalse(form.isDirty());
    }

    @Test
    public void resetAfterSaveRestoresDefaultsAndBecomesDirty() {
        TemplateEditorForm form = TemplateEditorForm.global(TemplateConfigValue.EMPTY);
        form.viewportInput = "120";
        form.updateActiveViewportDraft();
        form.markSaved(null);

        form.viewportInput = "130";
        form.updateActiveViewportDraft();
        form.reset();

        assertTrue(form.isDirty());
        assertEquals(ViewportTargetType.RELATIVE_SCALE, form.viewportMode);
        assertEquals("", form.viewportInput);
        assertEquals("", form.viewportScaleInput);
        assertEquals("", form.viewportAbsoluteInput);
    }

    @Test
    public void restoredDraftRetainsItsOriginalDirtyBaseline() {
        TemplateEditorForm source = TemplateEditorForm.global(TemplateConfigValue.EMPTY);
        source.viewportInput = "125";
        source.updateActiveViewportDraft();

        TemplateEditorForm restored = TemplateEditorForm.restore(
                source.quickTemplate,
                source.templateId,
                source.newTemplate,
                source.nameInput,
                source.viewportInput,
                source.viewportMode,
                source.viewportApplyMode,
                source.viewportScaleInput,
                source.viewportAbsoluteInput,
                source.fontInput,
                source.fontMode,
                source.selectedTypefaceId,
                source.fontHookDomainsRaw,
                source.initialSignature());

        assertTrue(restored.isDirty());
        assertEquals("125", restored.viewportInput);
    }

    @Test
    public void retainedGlobalDraftRestoresBothViewportInputsAndRemainsDirty() {
        TemplateEditorForm form = TemplateEditorForm.global(TemplateConfigValue.EMPTY);
        form.applyDraft(new TemplateEditorDraft(false, "",
                "411", ViewportTargetType.ABSOLUTE_DP, "system", "125", "411",
                "130", FontApplyMode.SYSTEM_EMULATION, null, null));

        assertTrue(form.isDirty());
        assertEquals("411", form.viewportInput);
        assertEquals("125", form.viewportScaleInput);
        assertEquals("411", form.viewportAbsoluteInput);
        assertEquals("130", form.fontInput);
    }

    @Test
    public void retainedQuickDraftRestoresNewTemplateIdentityAndContent() {
        TemplateEditorForm form = TemplateEditorForm.quick(null, "new-id");
        form.applyDraft(new TemplateEditorDraft(true,
                "Draft", "120", ViewportTargetType.RELATIVE_SCALE, "off",
                "120", "411", "", FontApplyMode.SYSTEM_EMULATION, null, null));

        assertTrue(form.newTemplate);
        assertTrue(form.isDirty());
        assertEquals("Draft", form.nameInput);
        assertEquals("411", form.viewportAbsoluteInput);
    }
}
