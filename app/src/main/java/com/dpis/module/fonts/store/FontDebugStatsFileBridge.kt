package com.dpis.module.fonts

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

object FontDebugStatsFileBridge {
    private const val DIR_NAME = "font_debug_stats"
    private const val FILE_NAME = "font_debug_stats.properties"

    @JvmStatic
    internal fun importIfNewer(context: Context?) {
        if (context == null) return
        val preferences = FontDebugStatsStore.getPreferences(context)
        importIfNewer(preferences, resolveFile(context))
        importIfNewer(preferences, resolveLegacyPublicFile())
    }

    @JvmStatic
    fun importIfNewer(preferences: SharedPreferences?, properties: Properties?) {
        if (preferences == null || properties == null || properties.isEmpty) return
        val incoming = FontDebugStatsSchema.propertyUpdatedAt(properties)
        val current = preferences.getLong(FontDebugStatsStore.KEY_UPDATED_AT, 0L)
        if (incoming <= 0L || incoming <= current) return
        val editor = preferences.edit()
        FontDebugStatsSchema.copyPropertiesToPreferences(properties, editor)
        editor.apply()
    }

    @JvmStatic
    fun importIfNewer(preferences: SharedPreferences?, file: File?) {
        loadProperties(file)?.let { importIfNewer(preferences, it) }
    }

    @JvmStatic
    fun resolveAppSpecificStatsFile(context: Context?): File? = resolveFile(context)

    @JvmStatic
    fun resolveAppSpecificStatsFile(baseDir: File?): File? = resolveFile(baseDir)

    @JvmStatic
    fun resolveLegacyPublicStatsFile(downloads: File?): File? = resolveLegacyPublicFile(downloads)

    private fun resolveFile(context: Context?): File? = resolveFile(context?.getExternalFilesDir(null))

    private fun resolveFile(baseDir: File?): File? =
        baseDir?.let { File(File(it, DIR_NAME), FILE_NAME) }

    private fun resolveLegacyPublicFile(): File? = resolveLegacyPublicFile(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
    )

    private fun resolveLegacyPublicFile(downloads: File?): File? =
        downloads?.let { File(File(it, "DPIS"), FILE_NAME) }

    private fun loadProperties(file: File?): Properties? {
        if (file == null || !file.isFile) return null
        return try {
            Properties().also { properties ->
                FileInputStream(file).use(properties::load)
            }
        } catch (_: IOException) {
            null
        }
    }
}
