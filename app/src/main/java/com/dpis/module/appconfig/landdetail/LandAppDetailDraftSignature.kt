package com.dpis.module.appconfig

import android.view.View
import com.dpis.module.R
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportTargetType
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

/** Persistable-looking landscape draft identity used to decide Unsaved / Save enablement. */
internal object LandAppDetailDraftSignature {
    fun empty(): String = listOf(
        "",
        ViewportTargetType.RELATIVE_SCALE,
        "",
        FontApplyMode.SYSTEM_EMULATION,
        "",
        "",
    ).joinToString("|")

    fun of(root: View): String = listOf(
        normalize(inputText(root.findViewById(R.id.land_detail_viewport_input))),
        AppConfigDialogBinder.resolveViewportMode(LandAppDetailModeToggles.viewport(root)),
        normalize(inputText(root.findViewById(R.id.land_detail_font_scale_input))),
        AppConfigDialogBinder.resolveFontMode(LandAppDetailModeToggles.font(root)),
        normalize(labelText(root.findViewById(R.id.land_detail_typeface_value))),
        normalize(labelText(root.findViewById(R.id.land_detail_hook_chain_value))),
    ).joinToString("|")

    fun rememberClean(root: View?, signature: String?) {
        root?.setTag(R.id.land_detail_save_button, signature.orEmpty())
    }

    fun cleanOf(root: View?): String {
        val tag = root?.getTag(R.id.land_detail_save_button)
        return tag as? String ?: ""
    }

    fun hasUnsavedChanges(root: View): Boolean = cleanOf(root) != of(root)

    fun inputText(input: TextInputEditText?): String = input?.text?.toString().orEmpty()

    private fun labelText(textView: MaterialTextView?): String =
        textView?.text?.toString().orEmpty()

    private fun normalize(value: String?): String = value?.trim().orEmpty()
}
