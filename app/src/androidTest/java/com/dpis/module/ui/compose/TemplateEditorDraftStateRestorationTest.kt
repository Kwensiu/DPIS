package com.dpis.module.ui.compose

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.templates.TemplateEditorForm
import com.dpis.module.templates.presentation.TemplateEditorDraftState
import com.dpis.module.templates.presentation.rememberTemplateEditorDraftState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateEditorDraftStateRestorationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun savedInstanceRestoreRetainsTemplateDraftAndDirtyBaseline() {
        val restorationTester = StateRestorationTester(composeRule)
        lateinit var draftState: TemplateEditorDraftState
        restorationTester.setContent {
            draftState = rememberTemplateEditorDraftState("global-prefill") {
                TemplateEditorForm.global(TemplateConfigValue.EMPTY)
            }
            draftState.observe()
            BasicTextField(
                value = draftState.form.viewportInput,
                onValueChange = {
                    draftState.form.viewportInput = it
                    draftState.form.updateActiveViewportDraft()
                    draftState.changed()
                },
                modifier = Modifier.testTag(ViewportInputTag)
            )
        }

        composeRule.onNodeWithTag(ViewportInputTag).performTextInput("125")
        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag(ViewportInputTag).assertTextEquals("125")
        composeRule.runOnIdle { assertTrue(draftState.form.isDirty) }
    }

    private companion object {
        const val ViewportInputTag = "template-viewport-input"
    }
}
