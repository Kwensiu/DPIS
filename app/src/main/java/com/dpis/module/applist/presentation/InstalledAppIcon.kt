package com.dpis.module.ui.compose

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Visible app rows load independently; icon decoding must never delay their list or sheet. */
private object InstalledAppIconCache {
    private val icons = object : LruCache<String, Drawable>(160) {}

    fun load(context: Context, packageName: String): Drawable? {
        icons.get(packageName)?.let { return it }
        return try {
            context.packageManager.getApplicationIcon(packageName).also { icons.put(packageName, it) }
        } catch (ignored: PackageManager.NameNotFoundException) {
            null
        } catch (ignored: RuntimeException) {
            null
        }
    }
}

@Composable
internal fun rememberInstalledAppIcon(packageName: String, initialIcon: Drawable?): Drawable? {
    val context = LocalContext.current
    val icon by produceState<Drawable?>(initialValue = initialIcon, key1 = packageName) {
        value = initialIcon ?: withContext(Dispatchers.IO) {
            InstalledAppIconCache.load(context.applicationContext, packageName)
        }
    }
    return icon
}
