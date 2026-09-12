package com.dpis.module.settings.presentation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import com.dpis.module.R
import com.dpis.module.settings.SystemFontScaleSettingsGateway
import com.dpis.module.settings.SystemFontScaleToolState
import com.dpis.module.settings.SystemFontScaleWriter
import com.dpis.module.ui.WatchUiMode
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.textview.MaterialTextView

internal class SystemFontScaleToolBinder(
    private val activity: Activity,
    private val workspaceView: View,
    private val host: ToolsWorkspaceBinder.Host,
    private val settingsGateway: SystemFontScaleSettingsGateway = SystemFontScaleSettingsGateway(),
) {
    private var card: MaterialCardView? = null
    private var operationGroup: View? = null
    private var permissionOverlay: View? = null
    private var unavailableOverlay: View? = null
    private var badgeView: MaterialTextView? = null
    private var pendingValueView: MaterialTextView? = null
    private var previewTitleView: MaterialTextView? = null
    private var previewBodyView: MaterialTextView? = null
    private var applyButton: AppCompatImageButton? = null
    private var decrementButton: AppCompatImageButton? = null
    private var incrementButton: AppCompatImageButton? = null
    private var slider: Slider? = null
    private var restoreButton: MaterialButton? = null
    private var expanded = false
    private var updatingSlider = false
    private var state: SystemFontScaleToolState? = null

    fun bind() {
        card = workspaceView.findViewById(R.id.system_font_scale_card)
        operationGroup = workspaceView.findViewById(R.id.system_font_scale_operation_group)
        permissionOverlay = workspaceView.findViewById(R.id.system_font_scale_permission_overlay)
        unavailableOverlay = workspaceView.findViewById(R.id.system_font_scale_unavailable_overlay)
        badgeView = workspaceView.findViewById(R.id.system_font_scale_badge)
        pendingValueView = workspaceView.findViewById(R.id.system_font_scale_pending_value)
        previewTitleView = workspaceView.findViewById(R.id.system_font_scale_preview_title)
        previewBodyView = workspaceView.findViewById(R.id.system_font_scale_preview_body)
        applyButton = workspaceView.findViewById(R.id.system_font_scale_apply_button)
        decrementButton = workspaceView.findViewById(R.id.system_font_scale_decrement_button)
        incrementButton = workspaceView.findViewById(R.id.system_font_scale_increment_button)
        slider = workspaceView.findViewById(R.id.system_font_scale_slider)
        restoreButton = workspaceView.findViewById(R.id.system_font_scale_restore_button)

        slider?.let { sliderView ->
            sliderView.valueFrom = SystemFontScaleToolState.MIN_PERCENT.toFloat()
            sliderView.valueTo = SystemFontScaleToolState.MAX_PERCENT.toFloat()
            sliderView.stepSize = 1f
            sliderView.addOnChangeListener { _, value, fromUser ->
                if (!fromUser || updatingSlider || state == null) return@addOnChangeListener
                setPendingPercent(Math.round(value))
            }
        }

        host.bindPressHaptic(card)
        host.bindPressHaptic(applyButton)
        host.bindPressHaptic(decrementButton)
        host.bindPressHaptic(incrementButton)
        host.bindPressHaptic(restoreButton)

        card?.setOnClickListener {
            expanded = !expanded
            render()
            if (expanded) revealExpandedPanel()
        }
        applyButton?.setOnClickListener { applyPending() }
        decrementButton?.setOnClickListener {
            val current = state
            if (current != null && current.canDecrement()) {
                setPendingPercent(current.pendingPercent - 1)
            }
        }
        incrementButton?.setOnClickListener {
            val current = state
            if (current != null && current.canIncrement()) {
                setPendingPercent(current.pendingPercent + 1)
            }
        }
        restoreButton?.setOnClickListener { restoreDefault() }
        host.bindPressHaptic(permissionOverlay)
        permissionOverlay?.setOnClickListener { openWriteSettingsPermission() }

        refreshFromSystem()
    }

    fun refreshFromSystem() {
        val canWrite = settingsGateway.canWrite(activity)
        val currentPercent = settingsGateway.readPercent(activity)
        val unavailable = currentPercent == null
        val current = state
        val pendingPercent = if (current != null && current.userSelectedPending) {
            current.pendingPercent
        } else {
            SystemFontScaleToolState.initialPendingPercent(currentPercent)
        }
        state = SystemFontScaleToolState(
            canWrite,
            currentPercent,
            pendingPercent,
            current != null && current.userSelectedPending,
            unavailable,
        )
        render()
    }

    fun collapseAndRefreshFromSystem() {
        expanded = false
        refreshFromSystem()
    }

    private fun setPendingPercent(percent: Int) {
        val current = state ?: return
        state = SystemFontScaleToolState(
            current.canWrite,
            current.currentPercent,
            SystemFontScaleToolState.clampPercent(percent),
            true,
            current.unavailable,
        )
        render()
    }

    private fun applyPending() {
        val current = state
        if (current == null || !current.canApply()) return
        writeScale(current.pendingPercent)
    }

    private fun restoreDefault() {
        val current = state
        if (current == null || !current.canRestore()) return
        if (current.shouldRestorePendingOnly()) {
            setPendingPercent(SystemFontScaleToolState.DEFAULT_PERCENT)
            return
        }
        writeScale(SystemFontScaleToolState.DEFAULT_PERCENT)
    }

    private fun writeScale(percent: Int) {
        SystemFontScaleWriter.write(object : SystemFontScaleWriter.Host {
            override fun writePercent(value: Int): Boolean =
                settingsGateway.writePercent(activity, value)

            override fun onWriteSucceeded(value: Int) {
                val current = state
                state = SystemFontScaleToolState(
                    current?.canWrite ?: false,
                    value,
                    value,
                    false,
                    false,
                )
                refreshFromSystem()
            }

            override fun onWriteFailed() {
                showWriteFailed()
            }
        }, percent)
    }

    private fun showWriteFailed() {
        Toast.makeText(
            activity,
            R.string.system_font_scale_write_failed,
            Toast.LENGTH_SHORT,
        ).show()
        refreshFromSystem()
    }

    private fun openWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = Uri.parse("package:" + activity.packageName)
        activity.startActivity(intent)
    }

    @SuppressLint("SetTextI18n")
    private fun render() {
        val current = state ?: return
        setVisible(operationGroup, expanded && (current.canWrite || current.unavailable))
        bindBadge()
        bindValues()
        bindControls()
        bindPreview()
        setVisible(permissionOverlay, expanded && !current.canWrite && !current.unavailable)
        setVisible(unavailableOverlay, expanded && current.unavailable)
    }

    private fun bindBadge() {
        val badge = badgeView ?: return
        val current = state ?: return
        val textRes = when (current.badge()) {
            SystemFontScaleToolState.Badge.UNAVAILABLE ->
                R.string.system_font_scale_badge_unavailable
            SystemFontScaleToolState.Badge.PERMISSION_REQUIRED -> {
                if (WatchUiMode.shouldUseCompactUi(activity)) {
                    badge.visibility = View.GONE
                    return
                }
                R.string.system_font_scale_badge_permission_required
            }
            SystemFontScaleToolState.Badge.OUT_OF_RANGE ->
                R.string.system_font_scale_badge_out_of_range
            SystemFontScaleToolState.Badge.MODIFIED ->
                R.string.system_font_scale_badge_modified
            SystemFontScaleToolState.Badge.NONE -> 0
        }
        if (textRes == 0) {
            badge.visibility = View.GONE
            return
        }
        badge.visibility = View.VISIBLE
        badge.setText(textRes)
    }

    private fun bindValues() {
        val current = state ?: return
        pendingValueView?.text = activity.getString(
            R.string.system_font_scale_pending_value,
            current.pendingPercent,
        )
    }

    private fun bindControls() {
        val current = state ?: return
        val controlsEnabled = current.canWrite && !current.unavailable
        slider?.let { sliderView ->
            updatingSlider = true
            sliderView.value = SystemFontScaleToolState.clampPercent(current.pendingPercent).toFloat()
            updatingSlider = false
            sliderView.isEnabled = controlsEnabled
        }
        bindIconButton(applyButton, current.canApply())
        bindIconButton(decrementButton, controlsEnabled && current.canDecrement())
        bindIconButton(incrementButton, controlsEnabled && current.canIncrement())
        restoreButton?.isEnabled = current.canRestore()
    }

    private fun bindIconButton(button: View?, enabled: Boolean) {
        if (button == null) return
        button.isEnabled = enabled
        button.alpha = if (enabled) 1f else DISABLED_ACTION_ALPHA
    }

    private fun bindPreview() {
        val current = state ?: return
        val scale = current.pendingPercent / 100f
        bindPreviewText(previewTitleView, PREVIEW_TITLE_SP, scale, true)
        bindPreviewText(previewBodyView, PREVIEW_BODY_SP, scale, false)
    }

    private fun bindPreviewText(view: TextView?, baseSp: Int, scale: Float, bold: Boolean) {
        if (view == null) return
        val density = activity.resources.displayMetrics.density
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, baseSp * density * scale)
        view.setTypeface(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun revealExpandedPanel() {
        val target = visibleExpandedPanel() ?: return
        target.post {
            val bounds = Rect(0, 0, target.width, target.height)
            target.requestRectangleOnScreen(bounds, true)
        }
    }

    private fun visibleExpandedPanel(): View? {
        val permission = permissionOverlay
        if (permission != null && permission.visibility == View.VISIBLE) return permission
        val operations = operationGroup
        if (operations != null && operations.visibility == View.VISIBLE) return operations
        return null
    }

    companion object {
        private const val DISABLED_ACTION_ALPHA = 0.45f
        private const val PREVIEW_TITLE_SP = 18
        private const val PREVIEW_BODY_SP = 14

        private fun setVisible(view: View?, visible: Boolean) {
            if (view != null) {
                view.visibility = if (visible) View.VISIBLE else View.GONE
            }
        }
    }
}
