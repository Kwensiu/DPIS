package com.dpis.module.templates

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.dpis.module.R
import com.dpis.module.ui.WindowInsetsBinder

/** Owns the legacy landscape template-detail view and its target-picker lifetime. */
class TemplateDetailPaneController(
    private val activity: Activity,
    private val content: FrameLayout?,
    private val emptyView: View?,
    private val host: QuickTemplateTargetsBinder.Host,
    private val onMissingTemplate: Runnable,
) {
    private var activeTargetsBinder: QuickTemplateTargetsBinder? = null

    fun hasContent() = (content?.childCount ?: 0) > 0

    fun show(selection: TemplateDetailSelection): Boolean {
        val detailContent = content ?: return false
        if (selection.kind != TemplateDetailKind.QUICK_TEMPLATE_TARGETS) return false
        disposeActiveBinder()
        detailContent.removeAllViews()
        val detailView = LayoutInflater.from(activity).inflate(
            R.layout.view_land_quick_template_targets_detail, detailContent, false
        )
        WindowInsetsBinder.applySafeDrawingPadding(detailView, false, true, false, true)
        val binder = QuickTemplateTargetsBinder(activity, detailView, host)
        activeTargetsBinder = binder
        if (!binder.bind(selection.templateId ?: "")) {
            detailContent.removeAllViews()
            activeTargetsBinder = null
            onMissingTemplate.run()
            return false
        }
        detailContent.addView(detailView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))
        ViewCompat.requestApplyInsets(detailView)
        return true
    }

    fun clear() {
        disposeActiveBinder()
        content?.removeAllViews()
    }

    fun dispose() {
        disposeActiveBinder()
    }

    private fun disposeActiveBinder() {
        activeTargetsBinder?.dispose()
        activeTargetsBinder = null
    }
}
