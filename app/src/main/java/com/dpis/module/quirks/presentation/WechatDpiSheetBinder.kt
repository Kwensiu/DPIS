package com.dpis.module.quirks.presentation

import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import com.dpis.module.DpisApplication
import com.dpis.module.R
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.quirks.WechatDpiEditor
import com.dpis.module.ui.DialogWindowSizer
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/** View adapter for the WeChat DPI editor row. Persistence lives in [WechatDpiEditor]. */
object WechatDpiSheetBinder {
    @JvmStatic
    fun bind(dialogView: View?, packageName: String?, onValidationChanged: Runnable?) {
        if (dialogView == null) return
        val row = row(dialogView)
        val inputLayout = inputLayout(dialogView)
        val inputView = inputView(dialogView)
        if (row == null || inputLayout == null || inputView == null) return
        if (!WechatDpiConfig.appliesTo(packageName)) {
            row.visibility = View.GONE
            return
        }
        row.visibility = View.VISIBLE
        helpButton(dialogView)?.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            showHelpDialog(view)
        }
        val store = DpisApplication.getActiveHookConfigStore(dialogView.context)
        val initial = store?.getWechatDpi(packageName)
        inputView.setText(if (initial != null) initial.toString() else "")
        inputView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateValidationState(inputLayout, inputView)
                onValidationChanged?.run()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        updateValidationState(inputLayout, inputView)
    }

    @JvmStatic
    fun isInputValid(dialogView: View?): Boolean {
        val inputView = inputView(dialogView) ?: return true
        val text = inputView.text ?: return true
        return WechatDpiEditor.isInputValid(text.toString())
    }

    @JvmStatic
    fun bindDoneAction(listener: TextView.OnEditorActionListener?, dialogView: View?) {
        inputView(dialogView)?.setOnEditorActionListener(listener)
    }

    @JvmStatic
    fun inputViewForFocus(dialogView: View?): TextInputEditText? = inputView(dialogView)

    @JvmStatic
    fun save(
        dialogView: View?,
        packageName: String?,
        dpisEnabled: Boolean,
        store: DpisConfigStore?,
    ): Boolean {
        val inputView = inputView(dialogView)
        val rawValue = inputView?.text?.toString()
        return WechatDpiEditor.save(rawValue, packageName, dpisEnabled, store)
    }

    @JvmStatic
    fun clearDraft(dialogView: View?) {
        inputView(dialogView)?.setText("")
    }

    @JvmStatic
    fun captureDraft(dialogView: View?): String? {
        val inputView = inputView(dialogView) ?: return null
        return inputView.text?.toString()
    }

    @JvmStatic
    fun applyDraft(dialogView: View?, rawValue: String?) {
        if (rawValue == null) return
        val inputView = inputView(dialogView) ?: return
        inputView.setText(rawValue)
        updateValidationState(inputLayout(dialogView), inputView)
    }

    private fun updateValidationState(inputLayout: TextInputLayout?, inputView: TextInputEditText?) {
        if (inputLayout == null || inputView == null) return
        val raw = inputView.text?.toString().orEmpty()
        val valid = WechatDpiEditor.isInputValid(raw)
        val defaultStrokeColor = MaterialColors.getColor(
            inputLayout,
            com.google.android.material.R.attr.colorOutline,
        )
        val errorStrokeColor = MaterialColors.getColor(
            inputLayout,
            androidx.appcompat.R.attr.colorError,
        )
        inputLayout.error = null
        inputLayout.isErrorEnabled = false
        inputLayout.boxStrokeColor = if (valid) defaultStrokeColor else errorStrokeColor
    }

    private fun row(dialogView: View?) = dialogView?.findViewById<View>(R.id.dialog_wechat_dpi_row)
    private fun inputLayout(dialogView: View?) =
        dialogView?.findViewById<TextInputLayout>(R.id.dialog_wechat_dpi_input_layout)
    private fun inputView(dialogView: View?) =
        dialogView?.findViewById<TextInputEditText>(R.id.dialog_wechat_dpi_input)
    private fun helpButton(dialogView: View?) =
        dialogView?.findViewById<View>(R.id.dialog_wechat_dpi_help_button)

    private fun showHelpDialog(anchor: View?) {
        if (anchor == null) return
        val dialog = MaterialAlertDialogBuilder(anchor.context)
            .setTitle(R.string.dialog_wechat_dpi_help_title)
            .setMessage(R.string.dialog_wechat_dpi_help_message)
            .setPositiveButton(R.string.dialog_close_button, null)
            .create()
        dialog.setOnShowListener {
            DialogWindowSizer.applyStandardWidth(dialog, anchor.context)
        }
        dialog.show()
    }
}
