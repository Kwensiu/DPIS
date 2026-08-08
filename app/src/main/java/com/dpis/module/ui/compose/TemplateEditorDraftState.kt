package com.dpis.module.ui.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import com.dpis.module.templates.TemplateEditorForm

/**
 * Saveable Compose owner for a [TemplateEditorForm].
 *
 * The Java form keeps template-domain rules in one place; this holder supplies the Compose
 * invalidation and saved-instance serialization needed when a sheet becomes a landscape detail.
 */
@Stable
class TemplateEditorDraftState internal constructor(val form: TemplateEditorForm) {
    private var revision by mutableIntStateOf(0)

    fun changed() {
        revision++
    }

    /**
     * Returns the mutable draft revision so a Compose subcomposition can invalidate even though
     * the Java form instance itself remains stable across edits.
     */
    fun observe(): Int {
        return revision
    }

    companion object {
        val Saver = listSaver(
            save = { state ->
                val form = state.form
                listOf(
                    form.quickTemplate,
                    form.templateId,
                    form.newTemplate,
                    form.nameInput,
                    form.viewportInput,
                    form.viewportMode,
                    form.viewportApplyMode,
                    form.viewportScaleInput,
                    form.viewportAbsoluteInput,
                    form.fontInput,
                    form.fontMode,
                    form.selectedTypefaceId,
                    form.fontHookDomainsRaw,
                    form.initialSignature()
                )
            },
            restore = { values ->
                TemplateEditorDraftState(
                    TemplateEditorForm.restore(
                        values[0] as Boolean,
                        values[1] as String?,
                        values[2] as Boolean,
                        values[3] as String?,
                        values[4] as String?,
                        values[5] as String?,
                        values[6] as String?,
                        values[7] as String?,
                        values[8] as String?,
                        values[9] as String?,
                        values[10] as String?,
                        values[11] as String?,
                        values[12] as String?,
                        values[13] as String?
                    )
                )
            }
        )
    }
}

@Composable
fun rememberTemplateEditorDraftState(
    key: String,
    initial: () -> TemplateEditorForm
): TemplateEditorDraftState = rememberSaveable(key, saver = TemplateEditorDraftState.Saver) {
    TemplateEditorDraftState(initial())
}
