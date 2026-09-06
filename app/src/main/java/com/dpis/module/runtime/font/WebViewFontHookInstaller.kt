package com.dpis.module.runtime.font

import android.util.Log
import android.webkit.WebSettings
import com.dpis.module.BuildConfig
import com.dpis.module.DpisConfigStore
import com.dpis.module.DpisLog
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.runtime.ProcessScopedInstallGate
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.concurrent.Volatile
import kotlin.math.max
import kotlin.math.min

object WebViewFontHookInstaller {
    private const val BRIDGE_LOG_PREFIX = "DPIS "
    private const val FONT_LOG_KEY_PREFIX = "font"
    private const val HOOK_ID_WEBVIEW_GET_SETTINGS = "webview_get_settings"
    private const val HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM = "websettings_set_text_zoom"
    private const val HOOK_ID_X5_WEBVIEW_GET_SETTINGS = "x5_webview_get_settings"
    private const val HOOK_ID_X5_WEBSETTINGS_SET_TEXT_ZOOM = "x5_websettings_set_text_zoom"

    @Volatile
    private var installedPid = -1
    private val LAST_MESSAGES: MutableMap<String?, String?> = ConcurrentHashMap<String?, String?>()
    private val INTERNAL_UPDATE: ThreadLocal<Boolean?> =
        ThreadLocal.withInitial<Boolean?>(Supplier { false })

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
    }

    @Throws(ReflectiveOperationException::class)
    @JvmStatic
    fun install(
        xposed: XposedInterface,
        packageName: String?,
        store: DpisConfigStore?,
        apiCapabilities: ModernApiCapabilities
    ) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return
        }
        synchronized(WebViewFontHookInstaller::class.java) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return
            }
            val fontScale = FontScaleOverride.resolve(store, packageName ?: return, 1.0f)
            val targetPercent = fontScale.targetPercent
            if (!isTargetPercentActive(targetPercent)) {
                return
            }
            val targetZoom = clampTextZoom(targetPercent ?: return)

            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val webViewClass = Class.forName("android.webkit.WebView", false, bootClassLoader)
            val getSettingsMethod = webViewClass.getDeclaredMethod("getSettings")
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(getSettingsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_WEBVIEW_GET_SETTINGS
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (result !is WebSettings) {
                        return@Hooker result
                    }
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker result
                    }
                    val detail = ("textZoom=" + targetZoom
                            + ", settings=" + result.javaClass.name)
                    RuntimeHotPathEvents.begin(
                        packageName,
                        "webview_text_zoom",
                        detail
                    )
                    INTERNAL_UPDATE.set(true)
                    try {
                        result.textZoom = targetZoom
                        RuntimeHotPathEvents.applied(
                            packageName,
                            "webview_text_zoom",
                            detail
                        )
                    } finally {
                        INTERNAL_UPDATE.remove()
                        RuntimeHotPathEvents.end(
                            packageName,
                            "webview_text_zoom",
                            detail
                        )
                    }
                    logIfChanged(
                        buildFontLogKey(packageName, "webview-getsettings"),
                        "DPIS_FONT WebView getSettings override: textZoom=" + targetZoom
                    )
                    bridgeLog(
                        xposed, ("DPIS_FONT WebView getSettings override applied: package="
                                + packageName + ", hookId=" + HOOK_ID_WEBVIEW_GET_SETTINGS
                                + ", textZoom=" + targetZoom)
                    )
                    result
                })

            installAndroidWebSettingsHook(
                xposed, packageName, targetZoom, bootClassLoader, apiCapabilities
            )

            installX5Hooks(xposed, packageName, targetZoom, apiCapabilities)
            installedPid = ProcessScopedInstallGate.currentPid()
            DpisLog.i(
                ("WebView font hook ready: hookIds="
                        + HOOK_ID_WEBVIEW_GET_SETTINGS + "," + HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM)
            )
            bridgeLog(
                xposed, ("DPIS_FONT WebView font hook ready: package=" + packageName
                        + ", hookIds=" + HOOK_ID_WEBVIEW_GET_SETTINGS + ","
                        + HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM)
            )
            RuntimeEvents.recordHotReload(
                packageName,
                "font",
                "installed",
                "webview font hook ready: textZoom=" + targetZoom
            )
        }
    }

    private fun installAndroidWebSettingsHook(
        xposed: XposedInterface,
        packageName: String?,
        targetZoom: Int,
        bootClassLoader: ClassLoader?,
        apiCapabilities: ModernApiCapabilities
    ) {
        try {
            val webSettingsClass =
                Class.forName("android.webkit.WebSettings", false, bootClassLoader)
            val setTextZoomMethod =
                webSettingsClass.getDeclaredMethod("setTextZoom", Int::class.javaPrimitiveType)
            if (Modifier.isAbstract(setTextZoomMethod.modifiers)) {
                logIfChanged(
                    buildFontLogKey(packageName, "websettings-abstract"),
                    "DPIS_FONT skip abstract WebSettings#setTextZoom hook"
                )
                return
            }
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(setTextZoomMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker result
                    }
                    val thisObject = chain.thisObject
                    if (thisObject !is WebSettings) {
                        return@Hooker result
                    }
                    val incomingZoom = chain.getArg(0) as Int
                    val detail = ("in=" + incomingZoom
                            + ", out=" + targetZoom
                            + ", settings=" + thisObject.javaClass.name)
                    RuntimeHotPathEvents.begin(
                        packageName,
                        "webview_text_zoom",
                        detail
                    )
                    INTERNAL_UPDATE.set(true)
                    try {
                        thisObject.textZoom = targetZoom
                        RuntimeHotPathEvents.applied(
                            packageName,
                            "webview_text_zoom",
                            detail
                        )
                    } finally {
                        INTERNAL_UPDATE.remove()
                        RuntimeHotPathEvents.end(
                            packageName,
                            "webview_text_zoom",
                            detail
                        )
                    }
                    if (incomingZoom != targetZoom) {
                        logIfChanged(
                            buildFontLogKey(packageName, "websettings-settextzoom"),
                            ("DPIS_FONT WebSettings setTextZoom override: in="
                                    + incomingZoom + ", out=" + targetZoom)
                        )
                        bridgeLog(
                            xposed,
                            ("DPIS_FONT WebSettings setTextZoom override applied: package="
                                    + packageName + ", hookId="
                                    + HOOK_ID_WEBSETTINGS_SET_TEXT_ZOOM
                                    + ", in=" + incomingZoom + ", out=" + targetZoom)
                        )
                    }
                    result
                })
        } catch (t: Throwable) {
            logIfChanged(
                buildFontLogKey(packageName, "websettings-hook-failed"),
                "DPIS_FONT WebSettings#setTextZoom hook skipped: "
                        + t.javaClass.simpleName
            )
        }
    }

    private fun installX5Hooks(
        xposed: XposedInterface,
        packageName: String?,
        targetZoom: Int,
        apiCapabilities: ModernApiCapabilities
    ) {
        val appClassLoader = Thread.currentThread().contextClassLoader
        val x5WebViewClass = findClassOptional("com.tencent.smtt.sdk.WebView", appClassLoader)
        val x5WebSettingsClass =
            findClassOptional("com.tencent.smtt.sdk.WebSettings", appClassLoader)
        if (x5WebViewClass == null || x5WebSettingsClass == null) {
            return
        }
        try {
            val getSettingsMethod = x5WebViewClass.getDeclaredMethod("getSettings")
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(getSettingsMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_X5_WEBVIEW_GET_SETTINGS
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker result
                    }
                    if (!x5WebSettingsClass.isInstance(result)) {
                        return@Hooker result
                    }
                    val setTextZoom =
                        x5WebSettingsClass.getMethod("setTextZoom", Int::class.javaPrimitiveType)
                    val detail = ("textZoom=" + targetZoom
                            + ", settings=" + result!!.javaClass.name)
                    RuntimeHotPathEvents.begin(
                        packageName,
                        "x5_webview_text_zoom",
                        detail
                    )
                    INTERNAL_UPDATE.set(true)
                    try {
                        setTextZoom.invoke(result, targetZoom)
                        RuntimeHotPathEvents.applied(
                            packageName,
                            "x5_webview_text_zoom",
                            detail
                        )
                    } finally {
                        INTERNAL_UPDATE.remove()
                        RuntimeHotPathEvents.end(
                            packageName,
                            "x5_webview_text_zoom",
                            detail
                        )
                    }
                    logIfChanged(
                        buildFontLogKey(packageName, "x5-webview-getsettings"),
                        "DPIS_FONT X5 WebView getSettings override: textZoom=" + targetZoom
                    )
                    bridgeLog(
                        xposed, ("DPIS_FONT X5 WebView getSettings override applied: package="
                                + packageName + ", hookId=" + HOOK_ID_X5_WEBVIEW_GET_SETTINGS
                                + ", textZoom=" + targetZoom)
                    )
                    result
                })
        } catch (ignored: Throwable) {
            return
        }
        try {
            val setTextZoomMethod =
                x5WebSettingsClass.getDeclaredMethod("setTextZoom", Int::class.javaPrimitiveType)
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(setTextZoomMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_X5_WEBSETTINGS_SET_TEXT_ZOOM
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker result
                    }
                    val thisObject = chain.thisObject
                    if (!x5WebSettingsClass.isInstance(thisObject)) {
                        return@Hooker result
                    }
                    val setTextZoom =
                        x5WebSettingsClass.getMethod("setTextZoom", Int::class.javaPrimitiveType)
                    val incomingZoom = chain.getArg(0) as Int
                    val detail = ("in=" + incomingZoom
                            + ", out=" + targetZoom
                            + ", settings=" + thisObject!!.javaClass.name)
                    RuntimeHotPathEvents.begin(
                        packageName,
                        "x5_webview_text_zoom",
                        detail
                    )
                    INTERNAL_UPDATE.set(true)
                    try {
                        setTextZoom.invoke(thisObject, targetZoom)
                        RuntimeHotPathEvents.applied(
                            packageName,
                            "x5_webview_text_zoom",
                            detail
                        )
                    } finally {
                        INTERNAL_UPDATE.remove()
                        RuntimeHotPathEvents.end(
                            packageName,
                            "x5_webview_text_zoom",
                            detail
                        )
                    }
                    if (incomingZoom != targetZoom) {
                        logIfChanged(
                            buildFontLogKey(packageName, "x5-websettings-settextzoom"),
                            ("DPIS_FONT X5 WebSettings setTextZoom override: in="
                                    + incomingZoom + ", out=" + targetZoom)
                        )
                        bridgeLog(
                            xposed,
                            ("DPIS_FONT X5 WebSettings setTextZoom override applied: package="
                                    + packageName + ", hookId="
                                    + HOOK_ID_X5_WEBSETTINGS_SET_TEXT_ZOOM
                                    + ", in=" + incomingZoom + ", out=" + targetZoom)
                        )
                    }
                    result
                })
        } catch (ignored: Throwable) {
            return
        }
        logIfChanged(buildFontLogKey(packageName, "x5-ready"), "DPIS_FONT X5 font hook ready")
    }

    private fun isTargetPercentActive(targetPercent: Int?): Boolean {
        return targetPercent != null && targetPercent > 0 && targetPercent != 100
    }

    private fun clampTextZoom(targetPercent: Int): Int {
        return max(50, min(500, targetPercent))
    }

    private fun findClassOptional(className: String, classLoader: ClassLoader?): Class<*>? {
        if (classLoader != null) {
            try {
                return Class.forName(className, false, classLoader)
            } catch (ignored: Throwable) {
            }
        }
        try {
            return Class.forName(className)
        } catch (ignored: Throwable) {
            return null
        }
    }

    private fun logIfChanged(key: String?, message: String) {
        val previous = LAST_MESSAGES.put(key, message)
        if (message != previous) {
            DpisLog.i(message)
        }
    }

    private fun bridgeLog(xposed: XposedInterface?, message: String?) {
        if (xposed == null || (!BuildConfig.DEBUG && !DpisLog.isLoggingEnabled())) {
            return
        }
        try {
            xposed.log(Log.INFO, DpisLog.TAG, BRIDGE_LOG_PREFIX + message)
        } catch (ignored: Throwable) {
            // Bridge evidence must not affect target app behavior.
        }
    }

    private fun buildFontLogKey(packageName: String?, suffix: String?): String {
        val pkg = if (packageName == null) "unknown" else packageName
        return pkg + ":" + FONT_LOG_KEY_PREFIX + ":" + suffix
    }
}
