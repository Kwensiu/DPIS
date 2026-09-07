package com.dpis.module.viewport

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Rect
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.min

object ViewportConfigurationScope {
    private const val WINDOW_AREA_RATIO_THRESHOLD = 0.85f
    private const val WINDOW_SHORT_SIDE_RATIO_THRESHOLD = 0.90f

    // --- reflection metadata cache (stage 1 perf optimization, issue #54 item 6) ---
    // The windowConfiguration field and its accessor methods are stable for the
    // lifetime of a process (always android.content.res.WindowConfiguration on
    // API 24+). Resolving them via getDeclaredMethod + setAccessible on every
    // Resources.getConfiguration() call was ~7% of process CPU on measured
    // scroll workloads. Cache the reflection metadata once; the Rect/int values
    // themselves are still read fresh on each call because Configuration mutates.
    @Volatile
    private var cachedWindowConfigurationField: Field? = null

    @Volatile
    private var cachedWindowConfigurationClass: Class<*>? = null

    @Volatile
    private var cachedGetWindowingModeMethod: Method? = null

    @Volatile
    private var cachedGetBoundsMethod: Method? = null

    @Volatile
    private var cachedGetAppBoundsMethod: Method? = null

    @Volatile
    private var cachedGetMaxBoundsMethod: Method? = null

    @JvmStatic
    fun isValidDisplayConfiguration(config: Configuration?): Boolean {
        return config != null && config.screenWidthDp > 0 && config.screenHeightDp > 0 && config.smallestScreenWidthDp > 0 && config.densityDpi > 0 && config.fontScale > 0f
    }

    @JvmStatic
    fun isWindowScoped(config: Configuration?): Boolean {
        val windowConfiguration = readWindowConfiguration(config)
        if (windowConfiguration == null) {
            return false
        }
        val windowingMode = readIntMethod(windowConfiguration, "getWindowingMode", -1)
        if (windowingMode > 1) {
            return true
        }
        val bounds = readRectMethod(windowConfiguration, "getBounds")
        val appBounds = readRectMethod(windowConfiguration, "getAppBounds")
        val maxBounds = readRectMethod(windowConfiguration, "getMaxBounds")
        return isWindowScopedBounds(bounds, maxBounds)
                || isWindowScopedBounds(appBounds, maxBounds)
    }

    @JvmStatic
    fun isWindowScopedBounds(bounds: Rect?, maxBounds: Rect?): Boolean {
        if (bounds == null || maxBounds == null || bounds.isEmpty || maxBounds.isEmpty) {
            return false
        }
        return isWindowScopedBounds(
            abs(bounds.right - bounds.left),
            abs(bounds.bottom - bounds.top),
            abs(maxBounds.right - maxBounds.left),
            abs(maxBounds.bottom - maxBounds.top)
        )
    }

    @JvmStatic
    fun isWindowScopedBounds(
        boundsWidth: Int, boundsHeight: Int,
        maxWidth: Int, maxHeight: Int
    ): Boolean {
        if (boundsWidth <= 0 || boundsHeight <= 0 || maxWidth <= 0 || maxHeight <= 0) {
            return false
        }
        if (boundsWidth > maxWidth || boundsHeight > maxHeight) {
            return false
        }
        val boundsShort = min(boundsWidth, boundsHeight)
        val maxShort = min(maxWidth, maxHeight)
        val boundsArea = boundsWidth.toLong() * boundsHeight.toLong()
        val maxArea = maxWidth.toLong() * maxHeight.toLong()
        return boundsShort < Math.round(maxShort * WINDOW_SHORT_SIDE_RATIO_THRESHOLD)
                || boundsArea < Math.round(maxArea * WINDOW_AREA_RATIO_THRESHOLD)
    }

    @SuppressLint("BlockedPrivateApi")
    private fun readWindowConfiguration(config: Configuration?): Any? {
        if (config == null) {
            return null
        }
        var field = cachedWindowConfigurationField
        if (field == null) {
            try {
                field = Configuration::class.java.getDeclaredField("windowConfiguration")
                field.isAccessible = true
                cachedWindowConfigurationField = field
            } catch (ignored: ReflectiveOperationException) {
                return null
            } catch (ignored: RuntimeException) {
                return null
            }
        }
        try {
            return field.get(config)
        } catch (ignored: ReflectiveOperationException) {
            return null
        } catch (ignored: RuntimeException) {
            return null
        }
    }

    private fun readRectMethod(target: Any, methodName: String): Rect? {
        val method = resolveMethod(target, methodName)
        if (method == null) {
            return null
        }
        try {
            val value = method.invoke(target)
            return if (value is Rect) value else null
        } catch (ignored: ReflectiveOperationException) {
            return null
        } catch (ignored: RuntimeException) {
            return null
        }
    }

    private fun readIntMethod(target: Any, methodName: String, fallback: Int): Int {
        val method = resolveMethod(target, methodName)
        if (method == null) {
            return fallback
        }
        try {
            val value = method.invoke(target)
            return if (value is Int) value else fallback
        } catch (ignored: ReflectiveOperationException) {
            return fallback
        } catch (ignored: RuntimeException) {
            return fallback
        }
    }

    // Resolve a declared method on target's class, caching the Method object per
    // windowConfiguration class. The class is framework-stable so this resolves
    // once and reuses; a different class (theoretical) triggers a one-off re-resolve.
    private fun resolveMethod(target: Any, methodName: String): Method? {
        val clazz: Class<*> = target.javaClass
        val cached = cachedMethodFor(methodName)
        if (cached != null && cachedWindowConfigurationClass == clazz) {
            return cached
        }
        try {
            val method = clazz.getDeclaredMethod(methodName)
            method.isAccessible = true
            cachedWindowConfigurationClass = clazz
            cacheMethodFor(methodName, method)
            return method
        } catch (ignored: ReflectiveOperationException) {
            return null
        } catch (ignored: RuntimeException) {
            return null
        }
    }

    private fun cachedMethodFor(methodName: String): Method? {
        when (methodName) {
            "getWindowingMode" -> return cachedGetWindowingModeMethod
            "getBounds" -> return cachedGetBoundsMethod
            "getAppBounds" -> return cachedGetAppBoundsMethod
            "getMaxBounds" -> return cachedGetMaxBoundsMethod
            else -> return null
        }
    }

    private fun cacheMethodFor(methodName: String, method: Method?) {
        when (methodName) {
            "getWindowingMode" -> cachedGetWindowingModeMethod = method
            "getBounds" -> cachedGetBoundsMethod = method
            "getAppBounds" -> cachedGetAppBoundsMethod = method
            "getMaxBounds" -> cachedGetMaxBoundsMethod = method
            else -> {}
        }
    }

    // --- test seam ---
    @JvmStatic
    fun resetReflectionCacheForTest() {
        cachedWindowConfigurationField = null
        cachedWindowConfigurationClass = null
        cachedGetWindowingModeMethod = null
        cachedGetBoundsMethod = null
        cachedGetAppBoundsMethod = null
        cachedGetMaxBoundsMethod = null
    }
}
