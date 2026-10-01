package com.dpis.module.wechat

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.quirks.WechatDpiPropertyBridge
import com.dpis.module.quirks.WechatDpiRuntime
import com.dpis.module.runtime.hookapi.ModernApiCapabilitiesResolver
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** Owns the WeChat-specific tab icon reflection route, separate from DPI getter routing. */
object WechatDpiModernBottomTabHookInstaller {
    private const val VIEW_CLASS = "com.tencent.mm.ui.TabIconView"
    private const val HOOK_ID = "wechat_dpi_bottom_tab_init"
    private const val MAX_NORMALIZE_ATTEMPTS = 8
    private val hookLock = Any()
    private val hookedClasses = Collections.newSetFromMap(IdentityHashMap<Class<*>, Boolean>())
    private val callbackLogged = AtomicBoolean(false)
    private val mutationLogged = AtomicBoolean(false)

    @JvmStatic
    fun install(
        xposed: XposedInterface?,
        classLoader: ClassLoader?,
        phase: WechatDpiInstallPhase,
        scaleCompensationEnabled: Boolean
    ) {
        if (xposed == null || classLoader == null) return
        val viewClass = try {
            Class.forName(VIEW_CLASS, false, classLoader)
        } catch (error: ClassNotFoundException) {
            skipped(phase, "class_not_found")
            return
        } catch (error: Throwable) {
            DpisLog.e(
                "modern WeChat DPI bottom tab icon hook failed while resolving class: ${error.javaClass.name}: ${error.message}",
                error
            )
            skipped(phase, "class_resolution_failed, error=${error.javaClass.simpleName}")
            return
        }
        val initMethod = viewClass.declaredMethods.firstOrNull(::isInitMethod)
        val scaleField = viewClass.declaredFields.firstOrNull(::isScaleField)
        if (initMethod == null) {
            skipped(
                phase,
                "init_method_not_found, declaredMethodCount=${viewClass.declaredMethods.size}"
            )
            return
        }
        synchronized(hookLock) { if (!hookedClasses.add(viewClass)) return }
        try {
            initMethod.isAccessible = true
            scaleField?.isAccessible = true
            ModernApiCapabilitiesResolver.fromXposed(xposed)
                .applyStableHookId(
                    xposed.hook(initMethod)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                    HOOK_ID,
                )
                .intercept { chain ->
                    val view = chain.thisObject
                    if (callbackLogged.compareAndSet(false, true)) {
                        val detail = "method=${methodName(initMethod)}, configuredDpi=${
                            WechatDpiPropertyBridge.readDpi(WechatDpiConfig.PACKAGE_NAME)
                        }"
                        DpisLog.i("modern WeChat DPI bottom tab icon callback hit: $detail")
                        RuntimeHotPathEvents.event(
                            WechatDpiConfig.PACKAGE_NAME,
                            "wechat_dpi",
                            "bottom_tab_icon",
                            "route_callback_entered",
                            detail
                        )
                    }
                    val configuredDpi = configuredDpiOrNull()
                    if (view != null && scaleField != null && configuredDpi != null && scaleCompensationEnabled) {
                        val oldScale = scaleField.getFloat(view)
                        val targetScale = WechatDpiRuntime.bottomTabIconScale(configuredDpi)
                        scaleField.setFloat(view, targetScale)
                        if (mutationLogged.compareAndSet(false, true)) {
                            val detail =
                                "field=${scaleField.name}, targetDpi=$configuredDpi, scale=$oldScale->$targetScale"
                            DpisLog.i("modern WeChat DPI bottom tab icon scale prepared: $detail")
                            RuntimeHotPathEvents.event(
                                WechatDpiConfig.PACKAGE_NAME,
                                "wechat_dpi",
                                "bottom_tab_icon",
                                "mutation_applied",
                                detail
                            )
                        }
                    }
                    val result = chain.proceed()
                    (view as? View)?.let { postNormalize(it, 0) }
                    result
                }
            DpisLog.i("modern WeChat DPI bottom tab icon hook ready: method=${methodName(initMethod)}, field=${scaleField?.name ?: "none"}, classLoader=$classLoader")
            RuntimeHotPathEvents.event(
                WechatDpiConfig.PACKAGE_NAME,
                "wechat_dpi",
                "bottom_tab_icon",
                "hook_ready",
                "attempt=${phase.routeName}, method=${methodName(initMethod)}, field=${scaleField?.name ?: "none"}"
            )
        } catch (error: Throwable) {
            synchronized(hookLock) { hookedClasses.remove(viewClass) }
            DpisLog.e(
                "modern WeChat DPI bottom tab icon hook failed: ${error.javaClass.name}: ${error.message}",
                error
            )
            skipped(phase, "hookFailed=true, error=${error.javaClass.simpleName}")
        }
    }

    private fun skipped(phase: WechatDpiInstallPhase, reason: String) {
        RuntimeHotPathEvents.event(
            WechatDpiConfig.PACKAGE_NAME,
            "wechat_dpi",
            "bottom_tab_icon",
            "skipped",
            "attempt=${phase.routeName}, reason=$reason"
        )
    }

    private fun postNormalize(view: View, attempt: Int) {
        if (attempt >= MAX_NORMALIZE_ATTEMPTS) return
        view.postOnAnimation {
            if (normalize(view)) return@postOnAnimation
            postNormalize(view, attempt + 1)
        }
    }

    private fun normalize(view: View): Boolean {
        val edge = minOf(view.width, view.height)
        if (edge <= 0 || !hasOversizedBitmap(view, edge)) return edge > 0
        val normalized = replaceOversizedBitmaps(view, edge)
        if (normalized) view.invalidate()
        if (normalized && mutationLogged.compareAndSet(false, true)) {
            DpisLog.i("modern WeChat DPI bottom tab icon bitmaps normalized: edge=$edge")
            RuntimeHotPathEvents.event(
                WechatDpiConfig.PACKAGE_NAME,
                "wechat_dpi",
                "bottom_tab_icon",
                "mutation_applied",
                "bitmapEdge=$edge"
            )
        }
        return normalized
    }

    private fun hasOversizedBitmap(view: Any, edge: Int): Boolean =
        view.javaClass.declaredFields.any { field ->
            if (field.type != Bitmap::class.java) return@any false
            try {
                field.isAccessible = true
                val bitmap = field.get(view) as? Bitmap
                bitmap != null && (bitmap.width > edge || bitmap.height > edge)
            } catch (_: Throwable) {
                false
            }
        }

    private fun replaceOversizedBitmaps(view: Any, edge: Int): Boolean {
        val bitmapFields = view.javaClass.declaredFields.filter { it.type == Bitmap::class.java }
        val rectFields = view.javaClass.declaredFields.filter { it.type == Rect::class.java }
        if (bitmapFields.size != 3 || rectFields.size != 3) return false
        val originalBitmaps = mutableListOf<Bitmap?>()
        val originalRects = mutableListOf<Rect?>()
        return try {
            var replaced = false
            bitmapFields.forEach { field ->
                field.isAccessible = true
                val bitmap = field.get(view) as Bitmap?
                originalBitmaps += bitmap
                if (bitmap != null && (bitmap.width > edge || bitmap.height > edge)) {
                    field.set(view, Bitmap.createScaledBitmap(bitmap, edge, edge, true))
                    replaced = true
                }
            }
            if (replaced) rectFields.forEach { field ->
                field.isAccessible = true
                val rect = field.get(view) as Rect?
                originalRects += rect
                if (rect != null && (rect.width() > edge || rect.height() > edge)) field.set(
                    view,
                    Rect(0, 0, edge, edge)
                )
            }
            replaced
        } catch (error: Throwable) {
            bitmapFields.forEachIndexed { index, field ->
                runCatching {
                    field.isAccessible = true; field.set(view, originalBitmaps.getOrNull(index))
                }
            }
            rectFields.forEachIndexed { index, field ->
                runCatching {
                    field.isAccessible = true; field.set(view, originalRects.getOrNull(index))
                }
            }
            DpisLog.e(
                "modern WeChat DPI bottom tab bitmap normalization failed: ${error.javaClass.name}: ${error.message}",
                error
            )
            false
        }
    }

    private fun isInitMethod(method: Method): Boolean = method.parameterTypes.contentEquals(
        arrayOf(
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType
        )
    )

    private fun isScaleField(field: Field): Boolean =
        field.type == Float::class.javaPrimitiveType && !Modifier.isStatic(field.modifiers)

    private fun configuredDpiOrNull(): Int? =
        WechatDpiPropertyBridge.readDpi(WechatDpiConfig.PACKAGE_NAME).takeIf { it > 0 }

    private fun methodName(method: Method): String = "${method.declaringClass.name}#${method.name}"
}
