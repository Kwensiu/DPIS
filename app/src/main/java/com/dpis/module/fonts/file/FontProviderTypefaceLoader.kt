package com.dpis.module.fonts

import android.content.Context
import android.graphics.Typeface
import java.lang.reflect.Method

/** Loads an imported face through the guarded provider without file-path access. */
object FontProviderTypefaceLoader {
    @JvmStatic
    fun load(typefaceId: String?, ttcIndex: Int): Typeface? {
        val context = resolveCurrentApplication()
        if (context == null || typefaceId.isNullOrBlank()) return null
        return try {
            context.contentResolver.openFileDescriptor(
                FontFileProvider.buildFaceUri(typefaceId),
                "r"
            )
                ?.use { descriptor -> FontTypefaceLoader.load(descriptor.fileDescriptor, ttcIndex) }
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolveCurrentApplication(): Context? {
        return try {
            val activityThread = Class.forName(
                "android.app.ActivityThread",
                false,
                ClassLoader.getSystemClassLoader()
            )
            val currentApplication: Method = activityThread.getDeclaredMethod("currentApplication")
            currentApplication.invoke(null) as? Context
        } catch (_: Throwable) {
            null
        }
    }
}
