package com.dpis.module

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Process
import android.view.View
import android.widget.TextView
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.fonts.FontFace
import com.dpis.module.fonts.FontLibraryEntry
import com.dpis.module.fonts.FontLibraryStore
import com.dpis.module.fonts.FontProviderTypefaceLoader
import com.dpis.module.fonts.FontTypefaceLoader
import com.dpis.module.fonts.PublishedFontFileResolver
import com.dpis.module.fonts.SystemFontRegistry
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import java.io.File
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

internal object LegacyTypefaceOverrideHookInstaller {
    private val LOG_PREFIX = "DPIS_FONT_STYLE "
    private val LAST_MESSAGES: MutableMap<String?, String?> = ConcurrentHashMap()
    private val INTERNAL_UPDATE: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    @kotlin.concurrent.Volatile
    private var hookInstalled = false

    @kotlin.concurrent.Volatile
    private var hookInstalledPid = -1

    @Throws(ReflectiveOperationException::class)
    fun install(
        packageName: String?,
        targetTypefaceId: String?,
        store: DpisConfigStore?,
        fontLibraryStore: FontLibraryStore?
    ) {
        if (isHookInstalledForCurrentProcess) {
            return
        }
        synchronized(LegacyTypefaceOverrideHookInstaller::class.java) {
            if (isHookInstalledForCurrentProcess) {
                return
            }
            val baseTypeface: Typeface? =
                loadTargetTypeface(
                    packageName, targetTypefaceId, store, fontLibraryStore
                )
            if (baseTypeface == null) {
                return
            }
            val bootClassLoader: ClassLoader? = ClassLoader.getSystemClassLoader()
            val textViewClass: Class<*> =
                Class.forName("android.widget.TextView", false, bootClassLoader)
            val setTypeface: Method? =
                textViewClass.getDeclaredMethod("setTypeface", Typeface::class.java)
            XposedBridge.hookMethod(setTypeface, object : XC_MethodHook() {
                protected override fun afterHookedMethod(param: MethodHookParam) {
                    if (INTERNAL_UPDATE.get() == true) {
                        return
                    }
                    val thisObject: Any? = param.thisObject
                    if (thisObject !is TextView) {
                        return
                    }
                    val original: Typeface? = param.args[0] as Typeface?
                    val replacement: Typeface? =
                        resolveReplacement(
                            baseTypeface,
                            original,
                            null
                        )
                    if (replacement != null) {
                        applyTextViewTypeface(
                            thisObject,
                            replacement,
                            null
                        )
                        logReplacementHit(
                            packageName,
                            "TextView.setTypeface(Typeface)"
                        )
                    }
                }
            })

            val setTypefaceWithStyle: Method? =
                textViewClass.getDeclaredMethod(
                    "setTypeface",
                    Typeface::class.java,
                    Int::class.javaPrimitiveType
                )
            XposedBridge.hookMethod(setTypefaceWithStyle, object : XC_MethodHook() {
                protected override fun afterHookedMethod(param: MethodHookParam) {
                    if (INTERNAL_UPDATE.get() == true) {
                        return
                    }
                    val thisObject: Any? = param.thisObject
                    if (thisObject !is TextView) {
                        return
                    }
                    val original: Typeface? = param.args[0] as Typeface?
                    val style: Int? = param.args[1] as Int?
                    val replacement: Typeface? =
                        resolveReplacement(
                            baseTypeface,
                            original,
                            style
                        )
                    if (replacement != null) {
                        applyTextViewTypeface(
                            thisObject,
                            replacement,
                            style
                        )
                        logReplacementHit(
                            packageName,
                            "TextView.setTypeface(Typeface,int)"
                        )
                    }
                }
            })

            val paintSetTypeface: Method? =
                Paint::class.java.getDeclaredMethod("setTypeface", Typeface::class.java)
            XposedBridge.hookMethod(paintSetTypeface, object : XC_MethodHook() {
                protected override fun afterHookedMethod(param: MethodHookParam) {
                    if (INTERNAL_UPDATE.get() == true) {
                        return
                    }
                    val thisObject: Any? = param.thisObject
                    if (thisObject !is Paint) {
                        return
                    }
                    val original: Typeface? = param.args[0] as Typeface?
                    val replacement: Typeface? =
                        resolveReplacement(
                            baseTypeface,
                            original,
                            null
                        )
                    if (replacement != null) {
                        applyPaintTypeface(
                            thisObject,
                            replacement
                        )
                        logReplacementHit(
                            packageName,
                            "Paint.setTypeface"
                        )
                    }
                }
            })
            installTextViewAttachHook(
                textViewClass,
                baseTypeface,
                packageName
            )
            installTextViewDrawHook(
                textViewClass,
                baseTypeface,
                packageName
            )
            hookInstalled = true
            hookInstalledPid = Process.myPid()
            DpisLog.i(LOG_PREFIX.toString() + "hook ready for " + packageName)
            XposedBridge.log("DPIS " + LOG_PREFIX + "hook ready for " + packageName)
            RuntimeEvents.recordTypeface(
                packageName, "hook_installed", "typeface hook ready: id=" + targetTypefaceId
            )
        }
    }

    private val isHookInstalledForCurrentProcess: Boolean
        get() = hookInstalled && hookInstalledPid == Process.myPid()

    private fun loadTargetTypeface(
        packageName: String?,
        targetTypefaceId: String?,
        store: DpisConfigStore?,
        fontLibraryStore: FontLibraryStore?
    ): Typeface? {
        var typefaceId = targetTypefaceId
        if ((typefaceId == null || typefaceId.isBlank()) && store != null) {
            typefaceId = store.getTargetTypefaceId(packageName ?: return null)
        }
        if (typefaceId == null || typefaceId.isBlank()) {
            logIfChanged(
                packageName.toString() + ":missing-typeface-id",
                LOG_PREFIX.toString() + "target typeface missing: package=" + packageName
            )
            return null
        }
        val systemTypeface: Typeface? = SystemFontRegistry.loadTypeface(typefaceId)
        if (systemTypeface != null) {
            logIfChanged(
                packageName.toString() + ":loaded:" + typefaceId,
                (LOG_PREFIX.toString() + "target typeface loaded: package=" + packageName
                        + ", typefaceId=" + typefaceId)
            )
            return systemTypeface
        }
        if (SystemFontRegistry.isSystemFontId(typefaceId)) {
            logIfChanged(
                packageName.toString() + ":system-load-failed:" + typefaceId,
                (LOG_PREFIX.toString() + "system typeface unavailable: package=" + packageName
                        + ", typefaceId=" + typefaceId)
            )
            return null
        }
        val selectedFace: FontFace? = FontFace.fromLegacyId(typefaceId)
        var ttcIndex = if (selectedFace != null) selectedFace.ttcIndex else 0
        val providerTypeface: Typeface? = FontProviderTypefaceLoader.load(typefaceId, ttcIndex)
        if (providerTypeface != null) {
            logTypefaceIfChanged(
                packageName, "source_provider_loaded", typefaceId,
                packageName.toString() + ":loaded-provider:" + typefaceId,
                (LOG_PREFIX.toString() + "target typeface loaded through provider: package=" + packageName
                        + ", typefaceId=" + typefaceId)
            )
            return providerTypeface
        }
        if (fontLibraryStore == null) {
            logIfChanged(
                packageName.toString() + ":provider-load-failed:" + typefaceId,
                (LOG_PREFIX.toString() + "font provider unavailable and no fallback catalog: package="
                        + packageName + ", typefaceId=" + typefaceId)
            )
            return null
        }
        val entry: FontLibraryEntry? = fontLibraryStore.findById(typefaceId)
        var file: File? = fontLibraryStore.resolveFontFile(typefaceId)
        if (file == null) {
            file = PublishedFontFileResolver.resolve(typefaceId)
        }
        if (file == null || !file.canRead()) {
            logIfChanged(
                packageName.toString() + ":unreadable:" + typefaceId,
                (LOG_PREFIX.toString() + "font file unreadable: package=" + packageName
                        + ", typefaceId=" + typefaceId)
            )
            return null
        }
        if (entry != null) {
            ttcIndex = entry.ttcIndex
        }
        val loaded: Typeface? = FontTypefaceLoader.load(file, ttcIndex)
        if (loaded == null) {
            logTypefaceIfChanged(
                packageName, "load_failed", typefaceId,
                packageName.toString() + ":load-failed:" + typefaceId,
                (LOG_PREFIX.toString() + "font load failed: package=" + packageName
                        + ", typefaceId=" + typefaceId)
            )
        } else {
            logTypefaceIfChanged(
                packageName, "source_fallback_loaded", typefaceId,
                packageName.toString() + ":loaded-fallback:" + typefaceId,
                (LOG_PREFIX.toString() + "target typeface loaded through fallback: package=" + packageName
                        + ", typefaceId=" + typefaceId)
            )
        }
        return loaded
    }

    private fun applyTextViewTypeface(
        textView: TextView,
        replacement: Typeface?,
        explicitStyle: Int?
    ) {
        INTERNAL_UPDATE.set(true)
        try {
            if (explicitStyle != null) {
                textView.setTypeface(replacement, explicitStyle)
                return
            }
            textView.setTypeface(replacement)
        } finally {
            INTERNAL_UPDATE.remove()
        }
    }

    private fun applyPaintTypeface(paint: Paint, replacement: Typeface?) {
        INTERNAL_UPDATE.set(true)
        try {
            paint.typeface = replacement
        } finally {
            INTERNAL_UPDATE.remove()
        }
    }

    private fun resolveReplacement(
        baseTypeface: Typeface?,
        original: Typeface?,
        explicitStyle: Int?
    ): Typeface? {
        if (baseTypeface == null) {
            return original
        }
        val style: Int = resolveStyle(
            if (original != null) original.style else null,
            explicitStyle
        )
        try {
            val styled: Typeface? = Typeface.create(baseTypeface, style)
            return if (styled != null) styled else baseTypeface
        } catch (ignored: Throwable) {
            return baseTypeface
        }
    }

    private fun resolveStyle(originalStyle: Int?, explicitStyle: Int?): Int {
        if (explicitStyle != null) {
            return explicitStyle
        }
        return if (originalStyle != null) originalStyle else Typeface.NORMAL
    }

    private fun logIfChanged(key: String?, message: String): Boolean {
        val previous: String? =
            LAST_MESSAGES.put(key, message)
        if (!message.equals(previous)) {
            DpisLog.i(message)
            return true
        }
        return false
    }

    private fun logTypefaceIfChanged(
        packageName: String?,
        stage: String?,
        typefaceId: String?,
        key: String?,
        message: String
    ) {
        if (logIfChanged(key, message)) {
            RuntimeEvents.recordTypeface(
                packageName, stage, "typefaceId=" + typefaceId
            )
        }
    }

    private fun installTextViewAttachHook(
        textViewClass: Class<*>,
        baseTypeface: Typeface?,
        packageName: String?
    ) {
        try {
            val onAttachedToWindow: Method =
                findOnAttachedToWindowMethod(
                    textViewClass
                )
            XposedBridge.hookMethod(onAttachedToWindow, object : XC_MethodHook() {
                protected override fun afterHookedMethod(param: MethodHookParam) {
                    if (INTERNAL_UPDATE.get() == true) {
                        return
                    }
                    val thisObject: Any? = param.thisObject
                    if (thisObject !is TextView) {
                        return
                    }
                    val replacement: Typeface? =
                        resolveReplacement(
                            baseTypeface,
                            thisObject.typeface,
                            null
                        )
                    if (replacement != null) {
                        applyTextViewTypeface(
                            thisObject,
                            replacement,
                            null
                        )
                        logReplacementHit(
                            packageName,
                            "TextView.onAttachedToWindow"
                        )
                    }
                }
            })
            logIfChanged(
                packageName.toString() + ":attach-hook",
                LOG_PREFIX.toString() + "TextView attach hook ready for " + packageName
            )
        } catch (throwable: Throwable) {
            logIfChanged(
                packageName.toString() + ":attach-hook-skipped",
                (LOG_PREFIX.toString() + "TextView attach hook skipped: package=" + packageName
                        + ", error=" + throwable.javaClass.simpleName)
            )
        }
    }

    private fun installTextViewDrawHook(
        textViewClass: Class<*>,
        baseTypeface: Typeface?,
        packageName: String?
    ) {
        try {
            val onDraw: Method? = textViewClass.getDeclaredMethod("onDraw", Canvas::class.java)
            XposedBridge.hookMethod(onDraw, object : XC_MethodHook() {
                protected override fun beforeHookedMethod(param: MethodHookParam) {
                    if (INTERNAL_UPDATE.get() == true) {
                        return
                    }
                    val thisObject: Any? = param.thisObject
                    if (thisObject !is TextView) {
                        return
                    }
                    val replacement: Typeface? =
                        resolveReplacement(
                            baseTypeface,
                            thisObject.typeface,
                            null
                        )
                    if (replacement != null) {
                        applyTextViewTypeface(
                            thisObject,
                            replacement,
                            null
                        )
                        logReplacementHit(
                            packageName,
                            "TextView.onDraw"
                        )
                    }
                }
            })
            logIfChanged(
                packageName.toString() + ":draw-hook",
                LOG_PREFIX.toString() + "TextView draw hook ready for " + packageName
            )
        } catch (throwable: Throwable) {
            logIfChanged(
                packageName.toString() + ":draw-hook-skipped",
                (LOG_PREFIX.toString() + "TextView draw hook skipped: package=" + packageName
                        + ", error=" + throwable.javaClass.simpleName)
            )
        }
    }

    @Throws(NoSuchMethodException::class)
    private fun findOnAttachedToWindowMethod(textViewClass: Class<*>): Method {
        try {
            return textViewClass.getDeclaredMethod("onAttachedToWindow")
        } catch (ignored: NoSuchMethodException) {
            return View::class.java.getDeclaredMethod("onAttachedToWindow")
        }
    }

    private fun logReplacementHit(packageName: String?, source: String?) {
        if (logIfChanged(
                packageName.toString() + ":replacement-hit:" + source,
                (LOG_PREFIX.toString() + "replacement hit: package=" + packageName
                        + ", source=" + source)
            )
        ) {
            RuntimeEvents.recordTypeface(
                packageName, "replacement_hit", "source=" + source
            )
        }
    }
}
