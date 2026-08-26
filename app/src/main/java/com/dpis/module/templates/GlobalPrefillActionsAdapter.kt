package com.dpis.module.templates

/** Adapts the Activity-owned global-prefill editor action to the workspace binder. */
class GlobalPrefillActionsAdapter(
    private val host: Host
) : TemplateWorkspaceBinder.GlobalPrefillActions {
    interface Host {
        fun edit()
    }

    override fun edit() = host.edit()
}
