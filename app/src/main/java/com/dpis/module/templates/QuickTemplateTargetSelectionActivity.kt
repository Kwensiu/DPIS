package com.dpis.module.templates

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.dpis.module.LocalizedActivity
import com.dpis.module.templates.presentation.QuickTemplateTargetActivityContent

class QuickTemplateTargetSelectionActivity : LocalizedActivity() {
    private var targetsController: QuickTemplateTargetsPresentationController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (shouldClosePortraitPageInLandscape()) {
            finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_ORIENTATION_MIGRATION)
            return
        }
        val templateId = intent?.getStringExtra(QuickTemplateTargetSelectionContract.EXTRA_TEMPLATE_ID)
        val controller = QuickTemplateTargetsPresentationController(this)
        targetsController = controller
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_USER_BACK)
            }
        })
        QuickTemplateTargetActivityContent.install(
            this,
            controller,
            { finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_USER_BACK) },
            { finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_SAVED) },
            { finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_MISSING_TEMPLATE) }
        )
        controller.load(templateId)
    }

    override fun onResume() {
        super.onResume()
        if (shouldClosePortraitPageInLandscape()) {
            finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_ORIENTATION_MIGRATION)
        }
    }

    override fun onDestroy() {
        targetsController?.dispose()
        targetsController = null
        super.onDestroy()
    }

    private fun shouldClosePortraitPageInLandscape(): Boolean =
        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun finishWithReason(closeReason: String) {
        val result = Intent().apply {
            putExtra(QuickTemplateTargetSelectionContract.EXTRA_CLOSE_REASON, closeReason)
        }
        setResult(RESULT_OK, result)
        finish()
    }
}
