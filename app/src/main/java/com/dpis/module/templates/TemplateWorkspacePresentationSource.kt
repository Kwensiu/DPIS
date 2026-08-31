package com.dpis.module.templates

import com.dpis.module.ConfigEditorDestination

/**
 * The complete Compose/Wear interaction surface for the template workspace.
 *
 * Keeping this as one source prevents the shell from learning template session transitions.
 * The shell merely renders the state and forwards UI intent back through this interface.
 */
interface TemplateWorkspacePresentationSource {
    fun state(): TemplateWorkspacePresentation.State
    fun changeQuery(query: String)
    fun openEditor(quickTemplate: Boolean, templateId: String?)
    fun updateEditor(form: TemplateEditorForm)
    fun updateEditorDestination(destination: ConfigEditorDestination)
    fun closeEditor()
}
