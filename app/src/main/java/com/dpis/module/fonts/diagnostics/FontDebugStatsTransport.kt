package com.dpis.module.fonts

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.dpis.module.BuildConfig
import com.dpis.module.diagnostics.DpisLog

object FontDebugStatsTransport {
    @JvmStatic
    fun sendUpdate(context: Context?, extras: Bundle?) {
        if (context == null || extras == null || extras.isEmpty) return
        if (isModuleContext(context)) {
            FontDebugStatsUpdateWriter.applyExtras(context, extras)
            return
        }

        try {
            val request = Bundle(extras).apply {
                putString(FontDebugStatsProvider.EXTRA_SOURCE_PACKAGE, context.packageName)
            }
            val result = context.contentResolver.call(
                buildUri(),
                FontDebugStatsProvider.METHOD_APPLY_UPDATE,
                null,
                request,
            )
            if (result?.getBoolean(FontDebugStatsProvider.RESULT_ACCEPTED, false) != true) {
                DpisLog.w("font debug stats provider rejected update")
            }
        } catch (error: Throwable) {
            DpisLog.e("font debug stats provider update failed", error)
        }
    }

    private fun isModuleContext(context: Context): Boolean =
        context.packageName == BuildConfig.APPLICATION_ID

    private fun buildUri(): Uri = Uri.Builder()
        .scheme("content")
        .authority(BuildConfig.APPLICATION_ID + FontDebugStatsProvider.AUTHORITY_SUFFIX)
        .build()
}
