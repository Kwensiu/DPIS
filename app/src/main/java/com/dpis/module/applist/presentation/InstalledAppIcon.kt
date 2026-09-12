package com.dpis.module.applist.presentation

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.flow.collect
import com.dpis.module.applist.AppListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Visible app rows load independently. Icons are rasterized once at the row size and drawn with
 * Compose [Image], matching InstallerX's scope list (no per-row [android.view.View]).
 */
internal object InstalledAppIconCache {
    private val bitmaps = LruCache<String, Bitmap>(160)

    fun peek(packageName: String, sizePx: Int): Bitmap? = bitmaps.get(cacheKey(packageName, sizePx))

    fun loadBitmap(
        context: Context,
        packageName: String,
        initialIcon: Drawable?,
        sizePx: Int,
    ): Bitmap? {
        if (sizePx <= 0) return null
        val key = cacheKey(packageName, sizePx)
        peek(packageName, sizePx)?.let { return it }
        val drawable = initialIcon ?: loadDrawable(context, packageName) ?: return null
        val bitmap = rasterize(drawable, sizePx) ?: return null
        bitmaps.put(key, bitmap)
        return bitmap
    }

    private fun loadDrawable(context: Context, packageName: String): Drawable? = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: RuntimeException) {
        null
    }

    private fun rasterize(drawable: Drawable, sizePx: Int): Bitmap? {
        if (drawable is BitmapDrawable) {
            val source = drawable.bitmap ?: return null
            if (!source.isRecycled && source.width == sizePx && source.height == sizePx) {
                return source
            }
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }

    private fun cacheKey(packageName: String, sizePx: Int): String = "$packageName@$sizePx"
}

@Composable
internal fun rememberInstalledAppIcon(packageName: String, initialIcon: Drawable?): Drawable? {
    val context = LocalContext.current
    val icon by produceState<Drawable?>(initialValue = initialIcon, key1 = packageName) {
        value = initialIcon ?: withContext(Dispatchers.IO) {
            try {
                context.applicationContext.packageManager.getApplicationIcon(packageName)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            } catch (_: RuntimeException) {
                null
            }
        }
    }
    return icon
}

@Composable
internal fun rememberInstalledAppIconBitmap(
    packageName: String,
    initialIcon: Drawable?,
    sizePx: Int,
): ImageBitmap? {
    val context = LocalContext.current
    val peeked = InstalledAppIconCache.peek(packageName, sizePx)
    if (peeked != null && !peeked.isRecycled) {
        return remember(peeked) { peeked.asImageBitmap() }
    }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = packageName, key2 = sizePx) {
        value = withContext(Dispatchers.IO) {
            InstalledAppIconCache.loadBitmap(
                context.applicationContext,
                packageName,
                initialIcon,
                sizePx,
            )?.asImageBitmap()
        }
    }
    return bitmap
}

@Composable
internal fun InstalledAppIconImage(
    packageName: String,
    initialIcon: Drawable?,
    sizePx: Int,
    modifier: Modifier = Modifier,
) {
    val icon = rememberInstalledAppIconBitmap(packageName, initialIcon, sizePx)
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(modifier)
    }
}

@Composable
internal fun PrefetchVisibleAppIcons(
    listState: LazyListState,
    items: List<AppListItem>,
    sizePx: Int,
) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(listState, items, sizePx) {
        if (items.isEmpty() || sizePx <= 0) return@LaunchedEffect
        snapshotFlow {
            val visible = listState.layoutInfo.visibleItemsInfo
            val first = visible.firstOrNull()?.index ?: 0
            val last = visible.lastOrNull()?.index ?: 0
            first to last
        }.collect { (first, last) ->
            val from = (first - 8).coerceAtLeast(0)
            val to = (last + 16).coerceAtMost(items.lastIndex)
            withContext(Dispatchers.IO) {
                for (index in from..to) {
                    val item = items[index]
                    InstalledAppIconCache.loadBitmap(
                        context,
                        item.packageName,
                        item.icon,
                        sizePx,
                    )
                }
            }
        }
    }
}
