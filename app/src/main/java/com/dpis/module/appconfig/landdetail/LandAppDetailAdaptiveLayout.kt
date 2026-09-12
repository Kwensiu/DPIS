package com.dpis.module.appconfig.landdetail

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.dpis.module.R
import com.google.android.material.button.MaterialButton
import kotlin.math.max
import kotlin.math.min

/** Landscape detail-pane measure/layout contracts that do not own editor state. */
internal object LandAppDetailAdaptiveLayout {
    /**
     * Reassert the fixed mode-control contract whenever a landscape detail child is rebound.
     * The detail pane can replace its child after an app switch, so relying only on the XML style
     * leaves a stale/reused LayoutParams width capable of expanding the track to match the row.
     */
    fun stabilizeModeToggleLayout(activity: Activity, container: View?) {
        if (container == null) {
            return
        }
        val params = container.layoutParams ?: return
        val expectedWidth = activity.resources.getDimensionPixelSize(
            R.dimen.dialog_mode_toggle_width,
        )
        val expectedHeight = activity.resources.getDimensionPixelSize(
            R.dimen.dialog_mode_toggle_row_height,
        )
        if (params.width != expectedWidth || params.height != expectedHeight) {
            params.width = expectedWidth
            params.height = expectedHeight
            container.layoutParams = params
        }
        // The detail child is attached after this binder runs. Reassert once after parent measure so
        // a reused landscape row cannot expand the toggle during the app-switch relayout pass.
        container.post {
            val laidOutParams = container.layoutParams
            if (laidOutParams != null &&
                (laidOutParams.width != expectedWidth || laidOutParams.height != expectedHeight)
            ) {
                laidOutParams.width = expectedWidth
                laidOutParams.height = expectedHeight
                container.layoutParams = laidOutParams
            }
        }
    }

    fun bindAdvancedActions(activity: Activity, root: View?) {
        if (root == null) {
            return
        }
        root.post {
            root.viewTreeObserver.addOnGlobalLayoutListener {
                updateAdvancedActionsLayout(activity, root)
            }
            updateAdvancedActionsLayout(activity, root)
        }
    }

    fun bindActionDock(
        activity: Activity,
        root: View?,
        saveButton: MaterialButton?,
    ) {
        if (root == null || saveButton == null) {
            return
        }
        val actionDock = root.findViewById<View>(R.id.land_detail_action_dock)
        root.post {
            root.viewTreeObserver.addOnGlobalLayoutListener {
                updateDockPresentation(activity, root, actionDock, saveButton)
            }
            updateDockPresentation(activity, root, actionDock, saveButton)
        }
    }

    private fun updateAdvancedActionsLayout(activity: Activity, root: View) {
        if (root.width <= 0) {
            return
        }
        val primaryRow = root.findViewById<LinearLayout>(R.id.land_detail_advanced_primary_row)
            ?: return
        val resetRow = root.findViewById<LinearLayout>(R.id.land_detail_advanced_reset_row)
            ?: return
        val resetButton = root.findViewById<MaterialButton>(R.id.land_detail_reset_row)
            ?: return
        // Reset wrapping is a width decision owned by this row. Do not make it while the row is
        // still awaiting its first measure; the global-layout observer will retry after attach.
        if (primaryRow.width <= 0) {
            return
        }
        val buttonMinWidth = activity.resources.getDimensionPixelSize(
            R.dimen.land_app_detail_advanced_button_wrap_min_width,
        )
        val spacing = activity.resources.getDimensionPixelSize(
            R.dimen.land_app_detail_action_button_spacing,
        )
        val threeButtonRequiredWidth = buttonMinWidth * 3 + spacing * 2
        // Measure the row that owns the weighted buttons. Using the detail root here double-counts
        // the outer/card padding and makes a narrow landscape pane wrap Reset too early.
        val wrapReset = primaryRow.width < threeButtonRequiredWidth
        val actionButtonHeight = activity.resources.getDimensionPixelSize(
            R.dimen.land_app_detail_action_button_height,
        )
        applyAdvancedPrimaryButtonLayout(
            root.findViewById(R.id.land_detail_scope_row),
            actionButtonHeight,
            0,
        )
        applyAdvancedPrimaryButtonLayout(
            root.findViewById(R.id.land_detail_dpis_toggle_row),
            actionButtonHeight,
            spacing,
        )
        // Once Reset moves to its own row, the primary row has exactly two weighted actions.
        // Keep the weight sum explicit so a prior three-button measurement cannot leave a gap.
        primaryRow.weightSum = if (wrapReset) 2f else 3f
        if (wrapReset && resetButton.parent !== resetRow) {
            primaryRow.removeView(resetButton)
            resetRow.addView(
                resetButton,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    actionButtonHeight,
                ),
            )
            resetRow.visibility = View.VISIBLE
        } else if (!wrapReset && resetButton.parent !== primaryRow) {
            resetRow.removeView(resetButton)
            val params = LinearLayout.LayoutParams(0, actionButtonHeight, 1f)
            params.marginStart = spacing
            primaryRow.addView(resetButton, params)
            resetRow.visibility = View.GONE
        } else {
            resetRow.visibility = if (wrapReset) View.VISIBLE else View.GONE
        }
    }

    private fun applyAdvancedPrimaryButtonLayout(
        button: MaterialButton?,
        height: Int,
        marginStart: Int,
    ) {
        if (button == null) {
            return
        }
        val params = LinearLayout.LayoutParams(0, height, 1f)
        params.marginStart = marginStart
        button.layoutParams = params
    }

    private fun updateDockPresentation(
        activity: Activity,
        root: View,
        actionDock: View?,
        saveButton: MaterialButton,
    ) {
        val rootWidth = root.width
        if (rootWidth <= 0) {
            return
        }
        val resources = activity.resources
        val dockHorizontalMargins = resources.getDimensionPixelSize(
            R.dimen.land_app_detail_dock_margin_horizontal,
        ) * 2
        val dockPadding = resources.getDimensionPixelSize(
            R.dimen.land_app_detail_dock_padding,
        ) * 2
        val saveExpandedWidth = resources.getDimensionPixelSize(
            R.dimen.land_app_detail_dock_save_expanded_min_width,
        )
        val saveCompactWidth = resources.getDimensionPixelSize(
            R.dimen.land_app_detail_dock_button_height,
        )
        val spacing = resources.getDimensionPixelSize(
            R.dimen.land_app_detail_dock_button_spacing,
        )
        val dockMaxWidth = resources.getDimensionPixelSize(
            R.dimen.land_app_detail_dock_max_width,
        )
        val processGroupMinWidth = resources.getDimensionPixelSize(
            R.dimen.land_app_detail_dock_process_group_min_width,
        )
        val targetDockWidth = max(0, min(rootWidth - dockHorizontalMargins, dockMaxWidth))
        if (actionDock != null) {
            val dockParams = actionDock.layoutParams
            if (dockParams != null && dockParams.width != targetDockWidth) {
                dockParams.width = targetDockWidth
                actionDock.layoutParams = dockParams
            }
        }
        updateScrollContentClearance(activity, root, actionDock)
        val availableDockContentWidth = targetDockWidth - dockPadding
        val expandedRequiredWidth = saveExpandedWidth + spacing + processGroupMinWidth
        val compact = availableDockContentWidth < expandedRequiredWidth
        val targetSaveWidth = if (compact) saveCompactWidth else saveExpandedWidth
        val params = saveButton.layoutParams
        if (params != null && params.width != targetSaveWidth) {
            params.width = targetSaveWidth
            saveButton.layoutParams = params
        }
        saveButton.minWidth = targetSaveWidth
        saveButton.minimumWidth = targetSaveWidth
        saveButton.iconPadding = 0
        saveButton.contentDescription = activity.getString(R.string.status_save_button)
        if (compact) {
            saveButton.text = null
            saveButton.setIconResource(R.drawable.ic_save_24)
            saveButton.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        } else {
            saveButton.icon = null
            saveButton.setText(R.string.status_save_button)
        }
    }

    /** Adds scroll clearance without clipping content around the floating dock's rounded surface. */
    private fun updateScrollContentClearance(
        activity: Activity,
        root: View,
        actionDock: View?,
    ) {
        if (actionDock == null || actionDock.height <= 0) {
            return
        }
        val content = root.findViewById<View>(R.id.land_detail_scroll_content) ?: return
        val dockBottomMargin = activity.resources.getDimensionPixelSize(
            R.dimen.land_app_detail_dock_margin_bottom,
        )
        val contentGap = activity.resources.getDimensionPixelSize(
            R.dimen.land_app_detail_card_gap,
        )
        val requiredBottomPadding = actionDock.height + dockBottomMargin + contentGap
        if (content.paddingBottom != requiredBottomPadding) {
            content.setPaddingRelative(
                content.paddingStart,
                content.paddingTop,
                content.paddingEnd,
                requiredBottomPadding,
            )
        }
    }
}
