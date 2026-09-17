package com.dpis.module.fonts

import android.os.Bundle
import com.dpis.module.fonts.presentation.FontDetailSession
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.fonts.presentation.installFontDetail

class FontDetailActivity : LocalizedActivity() {
    private lateinit var session: FontDetailSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extraId = intent.getStringExtra(EXTRA_FONT_ID)
        if (extraId.isNullOrBlank()) {
            finish()
            return
        }
        session = FontDetailSession(this, extraId, ::finish)
        if (!session.start()) {
            return
        }
        installFontDetail(
            session.presentation,
            session::showFallbackExplanationDialog,
            session::promptRename,
            session::confirmDeleteForCurrentEntry,
            session::confirmClearAppTypefaceByPackage,
            session::onRenameSubmit,
            session::onFallbackRetry,
            session::onDeleteConfirm,
            session::onRestoreConfirm,
        )
    }

    override fun onResume() {
        super.onResume()
        if (::session.isInitialized) {
            session.onResume()
        }
    }

    companion object {
        const val EXTRA_FONT_ID = "com.dpis.module.fonts.extra.FONT_ID"
    }
}
