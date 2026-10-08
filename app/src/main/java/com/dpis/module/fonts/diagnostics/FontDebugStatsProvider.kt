package com.dpis.module.fonts

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import com.dpis.module.DpisApplication

class FontDebugStatsProvider : ContentProvider() {
    companion object {
        const val METHOD_APPLY_UPDATE = "apply_update"
        const val AUTHORITY_SUFFIX = ".fontdebugstats"
        const val EXTRA_SOURCE_PACKAGE = "font_debug_source_package"
        const val RESULT_ACCEPTED = "accepted"
    }

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != METHOD_APPLY_UPDATE) return super.call(method, arg, extras)
        val appContext = context?.applicationContext ?: context ?: return result(false)
        if (!isAuthorizedCaller(appContext, extras)) return result(false)
        FontDebugStatsUpdateWriter.applyExtras(appContext, extras)
        return result(true)
    }

    private fun isAuthorizedCaller(context: Context, extras: Bundle?): Boolean {
        val sourcePackage = extras?.getString(EXTRA_SOURCE_PACKAGE)
        val callerPackages = mutableSetOf<String>()
        context.packageManager.getPackagesForUid(Binder.getCallingUid())?.forEach { packageName ->
            if (packageName != null) callerPackages += packageName
        }
        val configuredPackages = mutableSetOf<String>()
        DpisApplication.getActiveHookConfigStore(context)
            ?.getConfiguredPackages()
            ?.forEach { packageName ->
                if (packageName != null) configuredPackages += packageName
            }
        return FontDebugStatsCallerPolicy.isAuthorized(
            sourcePackage,
            callerPackages,
            configuredPackages,
        )
    }

    private fun result(accepted: Boolean): Bundle = Bundle().apply {
        putBoolean(RESULT_ACCEPTED, accepted)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
