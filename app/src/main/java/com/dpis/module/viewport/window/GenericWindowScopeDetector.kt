package com.dpis.module.viewport.window

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Rect
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.concurrent.Volatile
import kotlin.math.abs
import kotlin.math.min

/** Detects window scope using platform Configuration data available to app processes. */
object GenericWindowScopeDetector : WindowScopeDetector {
    private const val WINDOW_AREA_RATIO_THRESHOLD = 0.85f
    private const val WINDOW_SHORT_SIDE_RATIO_THRESHOLD = 0.90f

    @Volatile
    private var windowConfigurationField: Field? = null

    @Volatile
    private var windowConfigurationClass: Class<*>? = null

    @Volatile
    private var methods: Map<String, Method> = emptyMap()

    override fun detect(input: WindowScopeInput): WindowScopeDecision? {
        val windowConfiguration = readWindowConfiguration(input.configuration) ?: return null
        val windowingMode = readInt(windowConfiguration, "getWindowingMode")
        if (windowingMode != null && windowingMode > 1) {
            return WindowScopeDecision(
                kind = WindowScopeKind.WINDOW,
                evidence = WindowScopeEvidence.PLATFORM_WINDOWING_MODE,
                mode = if (windowingMode == 5) WindowMode.FREEFORM else WindowMode.SPLIT_SCREEN,
            )
        }
        val bounds = readRect(windowConfiguration, "getBounds")
        val appBounds = readRect(windowConfiguration, "getAppBounds")
        val maxBounds = readRect(windowConfiguration, "getMaxBounds")
        if (isWindowBounds(bounds, maxBounds) || isWindowBounds(appBounds, maxBounds)) {
            return WindowScopeDecision(
                kind = WindowScopeKind.WINDOW,
                evidence = WindowScopeEvidence.PLATFORM_BOUNDS,
            )
        }
        return WindowScopeDecision(
            kind = WindowScopeKind.DISPLAY,
            mode = WindowMode.FULLSCREEN,
        )
    }

    @JvmStatic
    fun isWindowBounds(
        boundsWidth: Int,
        boundsHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Boolean {
        if (boundsWidth <= 0 || boundsHeight <= 0 || maxWidth <= 0 || maxHeight <= 0
            || boundsWidth > maxWidth || boundsHeight > maxHeight
        ) {
            return false
        }
        val boundsShort = min(boundsWidth, boundsHeight)
        val maxShort = min(maxWidth, maxHeight)
        val boundsArea = boundsWidth.toLong() * boundsHeight.toLong()
        val maxArea = maxWidth.toLong() * maxHeight.toLong()
        return boundsShort < (maxShort * WINDOW_SHORT_SIDE_RATIO_THRESHOLD).toInt()
                || boundsArea < (maxArea * WINDOW_AREA_RATIO_THRESHOLD).toLong()
    }

    @JvmStatic
    fun resetReflectionCacheForTest() {
        windowConfigurationField = null
        windowConfigurationClass = null
        methods = emptyMap()
    }

    // The app process has no public windowing-mode field, so the module reads
    // the hidden Configuration.windowConfiguration.
    @SuppressLint("BlockedPrivateApi")
    private fun readWindowConfiguration(config: Configuration?): Any? {
        if (config == null) return null
        var field = windowConfigurationField
        if (field == null) {
            field = try {
                Configuration::class.java.getDeclaredField("windowConfiguration").also {
                    it.isAccessible = true
                }
            } catch (_: ReflectiveOperationException) {
                return null
            } catch (_: RuntimeException) {
                return null
            }
            windowConfigurationField = field
        }
        return try {
            field.get(config)
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun method(target: Any, name: String): Method? {
        val clazz = target.javaClass
        methods[name]?.takeIf { windowConfigurationClass == clazz }?.let { return it }
        return try {
            clazz.getDeclaredMethod(name).also {
                it.isAccessible = true
                windowConfigurationClass = clazz
                methods = methods + (name to it)
            }
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun readInt(target: Any, name: String): Int? = invoke(target, name) as? Int

    private fun readRect(target: Any, name: String): Rect? = invoke(target, name) as? Rect

    private fun invoke(target: Any, name: String): Any? {
        return try {
            method(target, name)?.invoke(target)
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun isWindowBounds(bounds: Rect?, maxBounds: Rect?): Boolean {
        if (bounds == null || maxBounds == null || bounds.isEmpty || maxBounds.isEmpty) return false
        return isWindowBounds(
            abs(bounds.width()),
            abs(bounds.height()),
            abs(maxBounds.width()),
            abs(maxBounds.height())
        )
    }
}
