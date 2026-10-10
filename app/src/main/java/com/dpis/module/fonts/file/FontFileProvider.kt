package com.dpis.module.fonts

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.ParcelFileDescriptor
import com.dpis.module.BuildConfig
import com.dpis.module.DpisApplication
import com.dpis.module.runtime.ConfigStoreFactory
import java.io.FileNotFoundException

/** Exposes only the configured face for the calling target app. */
class FontFileProvider : ContentProvider() {
    companion object {
        private const val PATH_FACE = "face"

        @JvmStatic
        fun buildFaceUri(typefaceId: String): Uri = Uri.Builder()
            .scheme("content").authority("${BuildConfig.APPLICATION_ID}.fonts")
            .appendPath(PATH_FACE).appendPath(typefaceId).build()
    }

    override fun onCreate(): Boolean = true
    override fun getType(uri: Uri): String = "font/*"

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Font files are read-only")
        val typefaceId = resolveTypefaceId(uri)
        val currentContext = context
        if (currentContext == null || typefaceId == null || !isCallerAuthorized(
                currentContext,
                typefaceId
            )
        ) {
            throw FileNotFoundException("Font face is unavailable for caller")
        }
        val file = ConfigStoreFactory.createLocalUiFontLibraryStore(currentContext, null)
            .resolveFontFile(typefaceId)
        if (file == null || !file.isFile) throw FileNotFoundException("Font file is missing")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun resolveTypefaceId(uri: Uri?): String? {
        val segments = uri?.pathSegments ?: return null
        if (segments.size != 2 || segments[0] != PATH_FACE) return null
        return segments[1].takeIf { it.isNotBlank() }
    }

    private fun isCallerAuthorized(context: Context, typefaceId: String): Boolean {
        val callingUid = Binder.getCallingUid()
        if (callingUid == android.os.Process.myUid()) return true
        val packages = context.packageManager.getPackagesForUid(callingUid).orEmpty()
        val configStore = DpisApplication.getActiveHookConfigStore(context) ?: return false
        return packages.any { typefaceId == configStore.getTargetTypefaceId(it) }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri =
        throw UnsupportedOperationException("Font files are read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Font files are read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException("Font files are read-only")
}
