package com.dpis.module.runtime.font

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.TextView
import com.dpis.module.BuildConfig
import com.dpis.module.DpisConfigStore
import com.dpis.module.DpisLog
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.fonts.FontFace
import com.dpis.module.fonts.FontLibraryStore
import com.dpis.module.fonts.FontProviderTypefaceLoader
import com.dpis.module.fonts.FontTypefaceLoader
import com.dpis.module.fonts.PublishedFontFileResolver
import com.dpis.module.fonts.SystemFontRegistry
import com.dpis.module.runtime.ProcessScopedInstallGate
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import java.io.File
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.concurrent.Volatile

object TypefaceOverrideHookInstaller {
    private const val BRIDGE_LOG_PREFIX = "DPIS "
    private const val LOG_PREFIX = "DPIS_FONT_STYLE "
    private const val HOOK_ID_TEXTVIEW_SET_TYPEFACE = "typeface_textview_set_typeface"
    private const val HOOK_ID_TEXTVIEW_SET_TYPEFACE_WITH_STYLE =
        "typeface_textview_set_typeface_with_style"
    private const val HOOK_ID_PAINT_SET_TYPEFACE = "typeface_paint_set_typeface"
    private const val HOOK_ID_TEXTVIEW_ON_ATTACHED_TO_WINDOW =
        "typeface_textview_on_attached_to_window"
    private const val HOOK_ID_TEXTVIEW_ON_DRAW = "typeface_textview_on_draw"

    // Process-level hook matching existing app-process installers; ModulePackagePlan decides
    // whether it is loaded for the current package.
    @Volatile
    private var installedPid = -1
    private val LAST_MESSAGES: MutableMap<String?, String?> = ConcurrentHashMap<String?, String?>()
    private val LAST_LOAD_SOURCES: MutableMap<String?, String?> =
        ConcurrentHashMap<String?, String?>()
    private val LAST_LOAD_TTC_INDICES: MutableMap<String?, Int?> =
        ConcurrentHashMap<String?, Int?>()
    private val INTERNAL_UPDATE: ThreadLocal<Boolean?> =
        ThreadLocal.withInitial<Boolean?>(Supplier { false })

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
        LAST_MESSAGES.clear()
        LAST_LOAD_SOURCES.clear()
        LAST_LOAD_TTC_INDICES.clear()
    }

    @Throws(ReflectiveOperationException::class)
    @JvmStatic
    fun install(
        xposed: XposedInterface,
        packageName: String,
        targetTypefaceId: String?,
        store: DpisConfigStore?,
        fontLibraryStore: FontLibraryStore?,
        apiCapabilities: ModernApiCapabilities
    ) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return
        }
        synchronized(TypefaceOverrideHookInstaller::class.java) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return
            }
            val baseTypeface = loadTargetTypeface(
                packageName, targetTypefaceId, store, fontLibraryStore
            )
            if (baseTypeface == null) {
                return
            }
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val textViewClass = Class.forName("android.widget.TextView", false, bootClassLoader)
            val setTypeface = textViewClass.getDeclaredMethod("setTypeface", Typeface::class.java)
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(setTypeface)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_TEXTVIEW_SET_TYPEFACE
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker chain!!.proceed()
                    }
                    val original = chain!!.getArg(0) as Typeface?
                    val replacement = resolveReplacement(baseTypeface, original, null)
                    if (replacement == null) {
                        return@Hooker chain.proceed()
                    }
                    val result = chain.proceed()
                    val thisObject = chain.thisObject
                    if (thisObject is TextView) {
                        applyTextViewTypeface(thisObject, replacement, null)
                        logReplacementHit(packageName, "TextView.setTypeface(Typeface)")
                        bridgeOverrideAppliedIfChanged(
                            xposed, packageName, HOOK_ID_TEXTVIEW_SET_TYPEFACE
                        )
                    }
                    result
                })

            val setTypefaceWithStyle =
                textViewClass.getDeclaredMethod(
                    "setTypeface",
                    Typeface::class.java,
                    Int::class.javaPrimitiveType
                )
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(setTypefaceWithStyle)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_TEXTVIEW_SET_TYPEFACE_WITH_STYLE
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker chain!!.proceed()
                    }
                    val original = chain!!.getArg(0) as Typeface?
                    val style = chain.getArg(1) as Int?
                    val replacement = resolveReplacement(baseTypeface, original, style)
                    if (replacement == null) {
                        return@Hooker chain.proceed()
                    }
                    val result = chain.proceed()
                    val thisObject = chain.thisObject
                    if (thisObject is TextView) {
                        applyTextViewTypeface(thisObject, replacement, style)
                        logReplacementHit(packageName, "TextView.setTypeface(Typeface,int)")
                        bridgeOverrideAppliedIfChanged(
                            xposed, packageName, HOOK_ID_TEXTVIEW_SET_TYPEFACE_WITH_STYLE
                        )
                    }
                    result
                })

            val paintSetTypeface =
                Paint::class.java.getDeclaredMethod("setTypeface", Typeface::class.java)
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(paintSetTypeface)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_PAINT_SET_TYPEFACE
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker chain!!.proceed()
                    }
                    val original = chain!!.getArg(0) as Typeface?
                    val replacement = resolveReplacement(baseTypeface, original, null)
                    if (replacement == null) {
                        return@Hooker chain.proceed()
                    }
                    val result = chain.proceed()
                    val thisObject = chain.thisObject
                    if (thisObject is Paint) {
                        applyPaintTypeface(thisObject, replacement)
                        logReplacementHit(packageName, "Paint.setTypeface")
                        bridgeOverrideAppliedIfChanged(
                            xposed, packageName, HOOK_ID_PAINT_SET_TYPEFACE
                        )
                    }
                    result
                })
            installTextViewAttachHook(
                xposed, textViewClass, baseTypeface, packageName, apiCapabilities
            )
            installTextViewDrawHook(
                xposed, textViewClass, baseTypeface, packageName, apiCapabilities
            )
            installedPid = ProcessScopedInstallGate.currentPid()
            DpisLog.i(
                (LOG_PREFIX + "hook ready for " + packageName
                        + ", hookIds=" + HOOK_ID_TEXTVIEW_SET_TYPEFACE + ","
                        + HOOK_ID_TEXTVIEW_SET_TYPEFACE_WITH_STYLE + ","
                        + HOOK_ID_PAINT_SET_TYPEFACE + ","
                        + HOOK_ID_TEXTVIEW_ON_ATTACHED_TO_WINDOW + ","
                        + HOOK_ID_TEXTVIEW_ON_DRAW)
            )
            bridgeLog(
                xposed, (LOG_PREFIX + "hook ready: package=" + packageName
                        + ", hookIds=" + HOOK_ID_TEXTVIEW_SET_TYPEFACE + ","
                        + HOOK_ID_TEXTVIEW_SET_TYPEFACE_WITH_STYLE + ","
                        + HOOK_ID_PAINT_SET_TYPEFACE + ","
                        + HOOK_ID_TEXTVIEW_ON_ATTACHED_TO_WINDOW + ","
                        + HOOK_ID_TEXTVIEW_ON_DRAW)
            )
            RuntimeEvents.recordTypeface(
                packageName,
                "hook_installed",
                ("typefaceId=" + targetTypefaceId
                        + ", loadSource=" + loadSourceFor(packageName, targetTypefaceId)
                        + ", ttcIndex=" + ttcIndexFor(packageName, targetTypefaceId))
            )
        }
    }

    private fun loadTargetTypeface(
        packageName: String,
        targetTypefaceId: String?,
        store: DpisConfigStore?,
        fontLibraryStore: FontLibraryStore?
    ): Typeface? {
        var typefaceId = targetTypefaceId
        if ((typefaceId == null || typefaceId.isBlank()) && store != null) {
            typefaceId = store.getTargetTypefaceId(packageName)
        }
        if (typefaceId == null || typefaceId.isBlank()) {
            logIfChanged(
                packageName + ":missing-typeface-id",
                LOG_PREFIX + "target typeface missing: package=" + packageName
            )
            return null
        }
        val systemTypeface = SystemFontRegistry.loadTypeface(typefaceId)
        if (systemTypeface != null) {
            recordLoadSource(packageName, typefaceId, "system", 0)
            logIfChanged(
                packageName + ":loaded:" + typefaceId,
                (LOG_PREFIX + "target typeface loaded: package=" + packageName
                        + ", typefaceId=" + typefaceId)
            )
            return systemTypeface
        }
        if (SystemFontRegistry.isSystemFontId(typefaceId)) {
            logIfChanged(
                packageName + ":system-load-failed:" + typefaceId,
                (LOG_PREFIX + "system typeface unavailable: package=" + packageName
                        + ", typefaceId=" + typefaceId)
            )
            return null
        }
        val selectedFace = FontFace.fromLegacyId(typefaceId)
        var ttcIndex = if (selectedFace != null) selectedFace.ttcIndex else 0
        val providerTypeface = FontProviderTypefaceLoader.load(typefaceId, ttcIndex)
        if (providerTypeface != null) {
            recordLoadSource(packageName, typefaceId, "provider", ttcIndex)
            logTypefaceIfChanged(
                packageName, "source_provider_loaded", typefaceId,
                packageName + ":loaded-provider:" + typefaceId,
                (LOG_PREFIX + "target typeface loaded through provider: package=" + packageName
                        + ", typefaceId=" + typefaceId)
            )
            return providerTypeface
        }
        if (fontLibraryStore == null) {
            recordLoadSource(packageName, typefaceId, "provider_failed", ttcIndex)
            logIfChanged(
                packageName + ":provider-load-failed:" + typefaceId,
                (LOG_PREFIX + "font provider unavailable and no fallback catalog: package="
                        + packageName + ", typefaceId=" + typefaceId)
            )
            return null
        }
        val entry = fontLibraryStore.findById(typefaceId)
        var file: File? = null
        if (entry != null) {
            file = fontLibraryStore.resolveFontFile(typefaceId)
            ttcIndex = entry.ttcIndex
        }
        if (file == null) {
            file = PublishedFontFileResolver.resolve(typefaceId)
            if (entry == null) {
                ttcIndex = parseTtcIndexFromId(typefaceId)
            }
        }
        if (file == null || !file.canRead()) {
            recordLoadSource(packageName, typefaceId, "unreadable", ttcIndex)
            logIfChanged(
                packageName + ":unreadable:" + typefaceId,
                (LOG_PREFIX + "font file unreadable: package=" + packageName
                        + ", typefaceId=" + typefaceId)
            )
            return null
        }
        val loaded = FontTypefaceLoader.load(file, ttcIndex)
        if (loaded == null) {
            recordLoadSource(packageName, typefaceId, "load_failed", ttcIndex)
            logTypefaceIfChanged(
                packageName, "load_failed", typefaceId,
                packageName + ":load-failed:" + typefaceId,
                (LOG_PREFIX + "font load failed: package=" + packageName
                        + ", typefaceId=" + typefaceId)
            )
        } else {
            recordLoadSource(packageName, typefaceId, "fallback", ttcIndex)
            logTypefaceIfChanged(
                packageName, "source_fallback_loaded", typefaceId,
                packageName + ":loaded-fallback:" + typefaceId,
                (LOG_PREFIX + "target typeface loaded through fallback: package=" + packageName
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

    @JvmStatic
    fun resolveStyleForTest(originalStyle: Int?, explicitStyle: Int?): Int {
        return resolveStyle(originalStyle, explicitStyle)
    }

    @JvmStatic
    fun resolveReplacementStyleForTest(originalStyle: Int?, explicitStyle: Int?): Int {
        return resolveStyle(originalStyle, explicitStyle)
    }

    @JvmStatic
    fun parseTtcIndexFromIdForTest(typefaceId: String): Int {
        return parseTtcIndexFromId(typefaceId)
    }

    @JvmStatic
    fun resolveReplacementForTest(baseTypeface: Typeface?, original: Typeface?): Typeface? {
        return resolveReplacement(baseTypeface, original, null)
    }

    private fun resolveReplacement(
        baseTypeface: Typeface?,
        original: Typeface?,
        explicitStyle: Int?
    ): Typeface? {
        if (baseTypeface == null) {
            return original
        }
        val originalStyle = if (original != null) original.style else null
        val style = resolveStyle(originalStyle, explicitStyle)
        try {
            val styled = Typeface.create(baseTypeface, style)
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

    private fun parseTtcIndexFromId(typefaceId: String): Int {
        val marker = typefaceId.lastIndexOf("_ttc_")
        if (marker <= 0 || marker + 5 >= typefaceId.length) {
            return 0
        }
        val suffix = typefaceId.substring(marker + 5)
        try {
            val index = suffix.toInt()
            return if (index >= 0) index else 0
        } catch (ignored: NumberFormatException) {
            return 0
        }
    }

    private fun recordLoadSource(
        packageName: String?,
        typefaceId: String?,
        source: String?,
        ttcIndex: Int
    ) {
        val key = packageName + ":" + typefaceId
        LAST_LOAD_SOURCES.put(key, source)
        LAST_LOAD_TTC_INDICES.put(key, ttcIndex)
        RuntimeEvents.recordTypeface(
            packageName,
            "load_source",
            "typefaceId=" + typefaceId + ", source=" + source + ", ttcIndex=" + ttcIndex
        )
    }

    private fun loadSourceFor(packageName: String?, typefaceId: String?): String {
        val value = LAST_LOAD_SOURCES.get(packageName + ":" + typefaceId)
        return if (value != null) value else "unknown"
    }

    private fun ttcIndexFor(packageName: String?, typefaceId: String?): Int {
        val value = LAST_LOAD_TTC_INDICES.get(packageName + ":" + typefaceId)
        return if (value != null) value else 0
    }

    private fun logIfChanged(key: String?, message: String): Boolean {
        val previous = LAST_MESSAGES.put(key, message)
        if (message != previous) {
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
        xposed: XposedInterface,
        textViewClass: Class<*>,
        baseTypeface: Typeface?,
        packageName: String?,
        apiCapabilities: ModernApiCapabilities
    ) {
        try {
            val onAttachedToWindow = findOnAttachedToWindowMethod(textViewClass)
            // Stable id lets 102 replace the reinforcement hook without keeping
            // a stale attach-time typeface route after a module hot reload.
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(onAttachedToWindow)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_TEXTVIEW_ON_ATTACHED_TO_WINDOW
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker result
                    }
                    val thisObject = chain.thisObject
                    if (thisObject !is TextView) {
                        return@Hooker result
                    }
                    val replacement =
                        resolveReplacement(baseTypeface, thisObject.typeface, null)
                    if (replacement == null) {
                        return@Hooker result
                    }
                    applyTextViewTypeface(thisObject, replacement, null)
                    logReplacementHit(packageName, "TextView.onAttachedToWindow")
                    bridgeOverrideAppliedIfChanged(
                        xposed, packageName, HOOK_ID_TEXTVIEW_ON_ATTACHED_TO_WINDOW
                    )
                    result
                })
            logIfChanged(
                packageName + ":attach-hook",
                LOG_PREFIX + "TextView attach hook ready for " + packageName
            )
        } catch (throwable: Throwable) {
            logIfChanged(
                packageName + ":attach-hook-skipped",
                (LOG_PREFIX + "TextView attach hook skipped: package=" + packageName
                        + ", error=" + throwable.javaClass.simpleName)
            )
        }
    }

    private fun installTextViewDrawHook(
        xposed: XposedInterface,
        textViewClass: Class<*>,
        baseTypeface: Typeface?,
        packageName: String?,
        apiCapabilities: ModernApiCapabilities
    ) {
        try {
            val onDraw = textViewClass.getDeclaredMethod("onDraw", Canvas::class.java)
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(onDraw)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_TEXTVIEW_ON_DRAW
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    if (true != INTERNAL_UPDATE.get()) {
                        val thisObject = chain!!.thisObject
                        if (thisObject is TextView) {
                            val replacement = resolveReplacement(
                                baseTypeface, thisObject.typeface, null
                            )
                            if (replacement != null) {
                                applyTextViewTypeface(thisObject, replacement, null)
                                logReplacementHit(packageName, "TextView.onDraw")
                                bridgeOverrideAppliedIfChanged(
                                    xposed, packageName, HOOK_ID_TEXTVIEW_ON_DRAW
                                )
                            }
                        }
                    }
                    chain!!.proceed()
                })
            logIfChanged(
                packageName + ":draw-hook",
                LOG_PREFIX + "TextView draw hook ready for " + packageName
            )
        } catch (throwable: Throwable) {
            logIfChanged(
                packageName + ":draw-hook-skipped",
                (LOG_PREFIX + "TextView draw hook skipped: package=" + packageName
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
                packageName + ":replacement-hit:" + source,
                (LOG_PREFIX + "replacement hit: package=" + packageName
                        + ", source=" + source)
            )
        ) {
            RuntimeEvents.recordTypeface(
                packageName, "replacement_hit", "source=" + source
            )
        }
    }

    private fun bridgeLog(xposed: XposedInterface?, message: String?) {
        if (xposed == null || (!BuildConfig.DEBUG && !DpisLog.isLoggingEnabled())) {
            return
        }
        try {
            xposed.log(Log.INFO, DpisLog.TAG, BRIDGE_LOG_PREFIX + message)
        } catch (ignored: Throwable) {
            // Bridge evidence is diagnostic-only; target app behavior wins.
        }
    }

    private fun bridgeOverrideAppliedIfChanged(
        xposed: XposedInterface?, packageName: String?, hookId: String?
    ) {
        val key = packageName + ":bridge-override:" + hookId
        val message = (LOG_PREFIX + "override applied: package="
                + packageName + ", hookId=" + hookId)
        val previous = LAST_MESSAGES.put(key, message)
        if (message != previous) {
            bridgeLog(xposed, message)
        }
    }
}
