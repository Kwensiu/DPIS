package com.dpis.module.fonts.presentation

import android.content.Context
import android.net.Uri
import com.dpis.module.fonts.FontLibraryArchiveCodec
import com.dpis.module.fonts.FontLibraryStore
import java.io.IOException

/** Owns archive I/O; the session only translates results into presentation feedback. */
class FontLibraryArchiveWorkflow(
    private val context: Context,
    private val store: FontLibraryStore,
    private val onExported: (FontLibraryArchiveCodec.ExportResult?) -> Unit,
    private val onImported: (FontLibraryArchiveCodec.RestoreResult?) -> Unit,
) {
    fun export(uri: Uri) {
        Thread({
            var result: FontLibraryArchiveCodec.ExportResult? = null
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    result = FontLibraryArchiveCodec.writeArchive(output, store)
                }
            } catch (_: IOException) {
            } catch (_: RuntimeException) {
            }
            val finalResult = result
            context.runOnUiThreadCompat { onExported(finalResult) }
        }, "dpis-font-library-export").start()
    }

    fun import(uri: Uri) {
        Thread({
            var result: FontLibraryArchiveCodec.RestoreResult? = null
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    result = FontLibraryArchiveCodec.restoreArchive(input, store, context.cacheDir)
                }
            } catch (_: IOException) {
            } catch (_: RuntimeException) {
            }
            val finalResult = result
            context.runOnUiThreadCompat { onImported(finalResult) }
        }, "dpis-font-library-import").start()
    }
}

private fun Context.runOnUiThreadCompat(action: () -> Unit) {
    (this as? android.app.Activity)?.runOnUiThread(action) ?: action()
}
