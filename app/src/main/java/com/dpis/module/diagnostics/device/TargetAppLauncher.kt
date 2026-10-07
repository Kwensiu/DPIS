package com.dpis.module.diagnostics.device

import android.content.Context
import com.dpis.module.root.RootAppProcessLauncher

class TargetAppLauncher(context: Context) {
    private val rootLauncher = RootAppProcessLauncher(context)

    fun restartForDiagnostic(packageName: String?): Boolean =
        packageName != null && rootLauncher.restart(packageName).code() == 0
}
