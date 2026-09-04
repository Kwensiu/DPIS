package com.dpis.module.templates.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

internal typealias TemplateEditorOverlay = @Composable () -> Unit

/** Root overlay bridge for the template editor; the workspace remains the state owner. */
internal class TemplateEditorOverlayHost {
    val content: MutableState<TemplateEditorOverlay?> = mutableStateOf(null)

    fun clear(expected: TemplateEditorOverlay?) {
        if (content.value === expected) content.value = null
    }
}
