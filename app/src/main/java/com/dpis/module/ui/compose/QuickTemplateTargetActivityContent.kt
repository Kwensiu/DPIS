package com.dpis.module.ui.compose

import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpis.module.templates.QuickTemplateTargetsPresentationController

/** Compose host for the manifest-owned portrait target-selection Activity contract. */
object QuickTemplateTargetActivityContent {
    @JvmStatic
    fun install(
        activity: ComponentActivity,
        controller: QuickTemplateTargetsPresentationController,
        onBack: Runnable,
        onSaved: Runnable,
        onMissingTemplate: Runnable
    ) {
        var state by mutableStateOf<QuickTemplateTargetsPresentationController.State?>(null)
        controller.addListener { next ->
            state = next
            if (next.missingTemplate) {
                Toast.makeText(
                    activity,
                    com.dpis.module.R.string.quick_template_target_missing,
                    Toast.LENGTH_SHORT
                ).show()
                onMissingTemplate.run()
            }
        }
        activity.setContent {
            DpisTheme(darkTheme = dpisDarkTheme()) {
                QuickTemplateTargetsContent(
                    state = state,
                    onBack = onBack::run,
                    onQueryChanged = controller::setQuery,
                    onFiltersChanged = controller::setFilters,
                    onSelectionChanged = controller::toggleSelection,
                    onSaveAndExit = {
                        val result = controller.save()
                        Toast.makeText(activity, result.messageResId, Toast.LENGTH_SHORT).show()
                        if (result.success) onSaved.run()
                        result.success
                    }
                )
            }
        }
    }
}
