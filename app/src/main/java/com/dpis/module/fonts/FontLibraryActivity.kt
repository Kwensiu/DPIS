package com.dpis.module.fonts

import android.content.Intent
import android.os.Bundle
import com.dpis.module.fonts.presentation.FontLibrarySession
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.ui.compose.installFontLibrary

class FontLibraryActivity : LocalizedActivity() {
    private lateinit var session: FontLibrarySession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = FontLibrarySession(this, ::openFontDetails)
        session.start()
        installFontLibrary(
            session.presentation,
            session::openFontImportPicker,
            session::openFontLibraryExportPicker,
            session::openFontLibraryImportPicker,
            session::openFontDetails,
            session::onNameSubmit,
            session::onLargeConfirm,
            session::onRepairConfirm,
        )
    }

    override fun onResume() {
        super.onResume()
        session.onResume()
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        session.onActivityResult(requestCode, resultCode, data)
    }

    private fun openFontDetails(fontId: String) {
        startActivity(
            Intent(this, FontDetailActivity::class.java)
                .putExtra(FontDetailActivity.EXTRA_FONT_ID, fontId),
        )
    }
}
