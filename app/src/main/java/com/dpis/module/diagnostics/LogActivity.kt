package com.dpis.module.diagnostics

import android.content.Intent
import android.os.Bundle
import com.dpis.module.diagnostics.presentation.LogActivitySession
import com.dpis.module.settings.LocalizedActivity

class LogActivity : LocalizedActivity() {
    private lateinit var session: LogActivitySession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = LogActivitySession(this)
        session.start()
    }

    override fun onResume() {
        super.onResume()
        session.onResume()
    }

    override fun onPause() {
        session.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        session.onDestroy()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        session.onActivityResult(requestCode, resultCode, data)
    }
}
