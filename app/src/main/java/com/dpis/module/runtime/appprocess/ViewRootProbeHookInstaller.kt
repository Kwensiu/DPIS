package com.dpis.module.runtime.appprocess

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.View
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.runtime.ProcessScopedInstallGate
import com.dpis.module.viewport.VirtualDisplayState
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile

object ViewRootProbeHookInstaller {
    private const val MAX_LOGS = 8
    private const val MAX_LAYOUT_EVIDENCE = 32
    private const val MEASURE_SPEC_MODE_MASK = 0x3 shl 30
    private const val MEASURE_SPEC_EXACTLY = 0x1 shl 30
    private const val MEASURE_SPEC_AT_MOST = 0x2 shl 30
    private const val LAYOUT_DIAGNOSTIC_ROUTE = "window_layout_diagnostics"

    private val LOG_COUNT = AtomicInteger()
    private val MEASURE_LOG_COUNT = AtomicInteger()
    private val FRAME_LOG_COUNT = AtomicInteger()
    private val RELAYOUT_LOG_COUNT = AtomicInteger()
    private val RESIZED_LOG_COUNT = AtomicInteger()
    private val LAYOUT_EVIDENCE_COUNT = AtomicInteger()

    @Volatile
    private var installedPid = -1

    @Volatile
    private var lastMeasureLog: String? = null

    @Volatile
    private var lastFrameLog: String? = null

    @Volatile
    private var lastRelayoutLog: String? = null

    @Volatile
    private var lastResizedLog: String? = null

    @Volatile
    private var installedPackageName: String? = null

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
        LOG_COUNT.set(0)
        MEASURE_LOG_COUNT.set(0)
        FRAME_LOG_COUNT.set(0)
        RELAYOUT_LOG_COUNT.set(0)
        RESIZED_LOG_COUNT.set(0)
        LAYOUT_EVIDENCE_COUNT.set(0)
        lastMeasureLog = null
        lastFrameLog = null
        lastRelayoutLog = null
        lastResizedLog = null
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(xposed: XposedInterface, packageName: String?) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return
        }
        synchronized(ViewRootProbeHookInstaller::class.java) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return
            }
            installedPackageName = packageName
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            val viewRootImplClass = Class.forName(
                "android.view.ViewRootImpl",
                false,
                bootClassLoader,
            )
            val performTraversalsMethod = resolvePerformTraversalsMethod(viewRootImplClass)
            val performMeasureMethod = resolvePerformMeasureMethod(viewRootImplClass)
            val setFrameMethod = viewRootImplClass.getDeclaredMethod(
                "setFrame",
                Rect::class.java,
                java.lang.Boolean.TYPE,
            )
            hookNamedMethods(xposed, viewRootImplClass, "relayoutWindow", true)
            hookNamedMethods(xposed, viewRootImplClass, "handleResized", false)
            xposed.hook(performTraversalsMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val active = chain!!
                    val thisObject = active.thisObject
                    if (LOG_COUNT.incrementAndGet() <= MAX_LOGS) {
                        DpisLog.i(buildPerformTraversalsLog(installedPackageName, thisObject))
                        val rootViewLog = buildRootViewLog(thisObject)
                        if (rootViewLog != null) {
                            DpisLog.i(rootViewLog)
                        }
                    }
                    val result = active.proceed()
                    recordTraversalEvidence(thisObject)
                    result
                })
            xposed.hook(performMeasureMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val active = chain!!
                    if (MEASURE_LOG_COUNT.incrementAndGet() <= MAX_LOGS) {
                        val args = active.args
                        if (args.size >= 2) {
                            val widthMeasureSpec = args[0]
                            val heightMeasureSpec = args[1]
                            if (widthMeasureSpec is Int && heightMeasureSpec is Int) {
                                val message = buildMeasureLog(
                                    installedPackageName,
                                    widthMeasureSpec,
                                    heightMeasureSpec,
                                )
                                if (message != lastMeasureLog) {
                                    lastMeasureLog = message
                                    DpisLog.i(message)
                                    RuntimeHotPathEvents.probe(
                                        installedPackageName,
                                        "viewport",
                                        LAYOUT_DIAGNOSTIC_ROUTE,
                                        message,
                                    )
                                }
                            }
                        }
                    }
                    active.proceed()
                })
            xposed.hook(setFrameMethod)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val active = chain!!
                    if (FRAME_LOG_COUNT.incrementAndGet() <= MAX_LOGS) {
                        val args = active.args
                        if (args.size >= 2) {
                            val frame = args[0]
                            val withinRelayout = args[1]
                            if (frame is Rect && withinRelayout is Boolean) {
                                maybeOverrideSetFrame(active.thisObject, frame)
                                val message =
                                    buildSetFrameLog(active.thisObject, frame, withinRelayout)
                                if (message != lastFrameLog) {
                                    lastFrameLog = message
                                    DpisLog.i(message)
                                }
                            }
                        }
                    }
                    active.proceed()
                })
            installedPid = ProcessScopedInstallGate.currentPid()
            DpisLog.i("ViewRoot probe hook ready")
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    @Throws(ReflectiveOperationException::class)
    private fun resolvePerformTraversalsMethod(viewRootImplClass: Class<*>): Method {
        // Xposed runtime uses this hidden method to observe traversal timing and
        // apply frame diagnostics consistently in the target process.
        return viewRootImplClass.getDeclaredMethod("performTraversals")
    }

    @SuppressLint("SoonBlockedPrivateApi")
    @Throws(ReflectiveOperationException::class)
    private fun resolvePerformMeasureMethod(viewRootImplClass: Class<*>): Method {
        // Xposed runtime uses this hidden method to observe measure specs for
        // viewport/frame correlation in real device diagnostics.
        return viewRootImplClass.getDeclaredMethod(
            "performMeasure",
            Integer.TYPE,
            Integer.TYPE,
        )
    }

    @JvmStatic
    fun buildPerformTraversalsLog(viewRootImpl: Any?): String {
        return "ViewRoot probe(performTraversals): width=" +
                "${readIntField(viewRootImpl, "mWidth")}, height=${
                    readIntField(
                        viewRootImpl,
                        "mHeight"
                    )
                }"
    }

    @JvmStatic
    fun buildPerformTraversalsLog(packageName: String?, viewRootImpl: Any?): String {
        return "ViewRoot probe(performTraversals): package=${safePackage(packageName)}, width=" +
                "${readIntField(viewRootImpl, "mWidth")}, height=${
                    readIntField(
                        viewRootImpl,
                        "mHeight"
                    )
                }"
    }

    @JvmStatic
    fun buildViewLog(width: Int, height: Int): String {
        return "ViewRoot probe(rootView): width=$width, height=$height"
    }

    @JvmStatic
    fun buildViewLog(packageName: String?, width: Int, height: Int): String {
        return "ViewRoot probe(rootView): package=${safePackage(packageName)}, width=$width, height=$height"
    }

    @JvmStatic
    fun buildMeasureLog(widthMeasureSpec: Int, heightMeasureSpec: Int): String {
        return "ViewRoot probe(performMeasure): widthSpec=${describeMeasureSpec(widthMeasureSpec)}, " +
                "heightSpec=${describeMeasureSpec(heightMeasureSpec)}"
    }

    @JvmStatic
    fun buildMeasureLog(
        packageName: String?,
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ): String {
        return "ViewRoot probe(performMeasure): package=${safePackage(packageName)}, widthSpec=" +
                "${describeMeasureSpec(widthMeasureSpec)}, heightSpec=${
                    describeMeasureSpec(
                        heightMeasureSpec
                    )
                }"
    }

    @JvmStatic
    fun buildSetFrameLog(
        oldWidth: Int,
        oldHeight: Int,
        frame: Rect?,
        withinRelayout: Boolean
    ): String {
        val newWidth = if (frame != null) frame.width() else -1
        val newHeight = if (frame != null) frame.height() else -1
        return buildSetFrameLog(oldWidth, oldHeight, newWidth, newHeight, withinRelayout)
    }

    @JvmStatic
    fun buildSetFrameLog(
        oldWidth: Int,
        oldHeight: Int,
        newWidth: Int,
        newHeight: Int,
        withinRelayout: Boolean,
    ): String {
        return "ViewRoot probe(setFrame): withinRelayout=$withinRelayout, " +
                "frame=${newWidth}x$newHeight, oldWinFrame=${oldWidth}x$oldHeight"
    }

    @JvmStatic
    fun buildSetFrameLog(
        packageName: String?,
        oldWidth: Int,
        oldHeight: Int,
        newWidth: Int,
        newHeight: Int,
        withinRelayout: Boolean,
    ): String {
        return "ViewRoot probe(setFrame): package=${safePackage(packageName)}, " +
                "withinRelayout=$withinRelayout, frame=${newWidth}x$newHeight, " +
                "oldWinFrame=${oldWidth}x$oldHeight"
    }

    @JvmStatic
    fun buildRelayoutWindowLog(
        stage: String?,
        result: Int,
        relayoutFrameWidth: Int,
        relayoutFrameHeight: Int,
        tmpFrameWidth: Int,
        tmpFrameHeight: Int,
        winFrameWidth: Int,
        winFrameHeight: Int,
    ): String {
        return "ViewRoot probe(relayoutWindow:$stage): result=$result, " +
                "relayoutFrame=${relayoutFrameWidth}x$relayoutFrameHeight, " +
                "tmpFrame=${tmpFrameWidth}x$tmpFrameHeight, winFrame=${winFrameWidth}x$winFrameHeight"
    }

    @JvmStatic
    fun buildRelayoutWindowLog(
        packageName: String?,
        stage: String?,
        result: Int,
        relayoutFrameWidth: Int,
        relayoutFrameHeight: Int,
        tmpFrameWidth: Int,
        tmpFrameHeight: Int,
        winFrameWidth: Int,
        winFrameHeight: Int,
    ): String {
        return "ViewRoot probe(relayoutWindow:$stage): package=${safePackage(packageName)}, " +
                "result=$result, relayoutFrame=${relayoutFrameWidth}x$relayoutFrameHeight, " +
                "tmpFrame=${tmpFrameWidth}x$tmpFrameHeight, winFrame=${winFrameWidth}x$winFrameHeight"
    }

    @JvmStatic
    fun buildHandleResizedLog(frameWidth: Int, frameHeight: Int): String {
        return "ViewRoot probe(handleResized): frame=${frameWidth}x$frameHeight"
    }

    @JvmStatic
    fun buildHandleResizedLog(packageName: String?, frameWidth: Int, frameHeight: Int): String {
        return "ViewRoot probe(handleResized): package=${safePackage(packageName)}, " +
                "frame=${frameWidth}x$frameHeight"
    }

    @JvmStatic
    fun shouldOverrideSetFrame(
        relayoutFrameWidth: Int,
        relayoutFrameHeight: Int,
        frameWidth: Int,
        frameHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Boolean {
        return WindowFrameOverride.isEnabled() &&
                WindowFrameOverride.shouldApply(
                    relayoutFrameWidth,
                    relayoutFrameHeight,
                    frameWidth,
                    frameHeight,
                    targetWidth,
                    targetHeight,
                )
    }

    private fun buildRootViewLog(viewRootImpl: Any?): String? {
        val rootView = readField(viewRootImpl, "mView")
        if (rootView !is View) {
            return null
        }
        val width = rootView.width
        val height = rootView.height
        if (width <= 0 || height <= 0) {
            return null
        }
        return buildViewLog(installedPackageName, width, height)
    }

    private fun buildSetFrameLog(
        viewRootImpl: Any?,
        frame: Rect?,
        withinRelayout: Boolean
    ): String {
        return buildSetFrameLog(
            installedPackageName,
            readRectWidth(viewRootImpl, "mWinFrame"),
            readRectHeight(viewRootImpl, "mWinFrame"),
            if (frame != null) frame.width() else -1,
            if (frame != null) frame.height() else -1,
            withinRelayout,
        )
    }

    private fun maybeOverrideSetFrame(viewRootImpl: Any?, frame: Rect?) {
        if (viewRootImpl == null || frame == null) {
            return
        }
        val override = VirtualDisplayState.get() ?: return
        val relayoutFrameWidth = readFirstRectWidth(readField(viewRootImpl, "mRelayoutResult"))
        val relayoutFrameHeight = readFirstRectHeight(readField(viewRootImpl, "mRelayoutResult"))
        if (!shouldOverrideSetFrame(
                relayoutFrameWidth,
                relayoutFrameHeight,
                frame.width(),
                frame.height(),
                override.widthPx,
                override.heightPx,
            )
        ) {
            return
        }
        WindowFrameOverride.apply(frame, override.widthPx, override.heightPx)
    }

    @Throws(ReflectiveOperationException::class)
    private fun hookNamedMethods(
        xposed: XposedInterface,
        targetClass: Class<*>,
        methodName: String,
        logAfter: Boolean,
    ) {
        for (method in targetClass.declaredMethods) {
            if (methodName != method.name) {
                continue
            }
            xposed.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val active = chain!!
                    if (logAfter) {
                        val result = active.proceed()
                        logRelayoutWindow(active.thisObject, result)
                        return@Hooker result
                    }
                    logHandleResized(active.args)
                    active.proceed()
                })
        }
    }

    private fun logRelayoutWindow(viewRootImpl: Any?, result: Any?) {
        if (RELAYOUT_LOG_COUNT.incrementAndGet() > MAX_LOGS) {
            return
        }
        val message = buildRelayoutWindowLog(
            installedPackageName,
            "after",
            if (result is Int) result else -1,
            readFirstRectWidth(readField(viewRootImpl, "mRelayoutResult")),
            readFirstRectHeight(readField(viewRootImpl, "mRelayoutResult")),
            readNestedRectWidth(viewRootImpl, "mTmpFrames", "frame"),
            readNestedRectHeight(viewRootImpl, "mTmpFrames", "frame"),
            readRectWidth(viewRootImpl, "mWinFrame"),
            readRectHeight(viewRootImpl, "mWinFrame"),
        )
        if (message != lastRelayoutLog) {
            lastRelayoutLog = message
            DpisLog.i(message)
            RuntimeHotPathEvents.probe(
                installedPackageName,
                "viewport",
                LAYOUT_DIAGNOSTIC_ROUTE,
                message,
            )
        }
    }

    private fun logHandleResized(args: List<*>) {
        if (RESIZED_LOG_COUNT.incrementAndGet() > MAX_LOGS) {
            return
        }
        var frameWidth = -1
        var frameHeight = -1
        for (arg in args) {
            if (arg is Rect) {
                frameWidth = arg.width()
                frameHeight = arg.height()
                break
            }
            val nestedWidth = readNestedRectWidth(arg, "frame")
            val nestedHeight = readNestedRectHeight(arg, "frame")
            if (nestedWidth >= 0 || nestedHeight >= 0) {
                frameWidth = nestedWidth
                frameHeight = nestedHeight
                break
            }
        }
        val message = buildHandleResizedLog(installedPackageName, frameWidth, frameHeight)
        if (message != lastResizedLog) {
            lastResizedLog = message
            DpisLog.i(message)
            RuntimeHotPathEvents.probe(
                installedPackageName,
                "viewport",
                LAYOUT_DIAGNOSTIC_ROUTE,
                message,
            )
        }
    }

    private fun recordTraversalEvidence(viewRootImpl: Any?) {
        if (LAYOUT_EVIDENCE_COUNT.incrementAndGet() > MAX_LAYOUT_EVIDENCE) {
            return
        }
        RuntimeHotPathEvents.probe(
            installedPackageName,
            "viewport",
            LAYOUT_DIAGNOSTIC_ROUTE,
            buildPerformTraversalsLog(installedPackageName, viewRootImpl),
        )
        val rootView = readField(viewRootImpl, "mView")
        if (rootView is View && rootView.width > 0 && rootView.height > 0) {
            RuntimeHotPathEvents.probe(
                installedPackageName,
                "viewport",
                LAYOUT_DIAGNOSTIC_ROUTE,
                buildViewLog(installedPackageName, rootView.width, rootView.height),
            )
        }
    }

    private fun describeMeasureSpec(measureSpec: Int): String {
        val mode = measureSpec and MEASURE_SPEC_MODE_MASK
        val size = measureSpec and MEASURE_SPEC_MODE_MASK.inv()
        val modeLabel = when (mode) {
            MEASURE_SPEC_EXACTLY -> "EXACTLY"
            MEASURE_SPEC_AT_MOST -> "AT_MOST"
            else -> "UNSPECIFIED"
        }
        return "$modeLabel($size)"
    }

    private fun readIntField(target: Any?, fieldName: String): Int {
        if (target == null) {
            return -1
        }
        return try {
            val field = target.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.getInt(target)
        } catch (_: ReflectiveOperationException) {
            -1
        }
    }

    private fun safePackage(packageName: String?): String {
        return if (packageName.isNullOrBlank()) "unknown" else packageName
    }

    private fun readField(target: Any?, fieldName: String): Any? {
        if (target == null) {
            return null
        }
        return try {
            val field = target.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(target)
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    private fun readRectWidth(target: Any?, fieldName: String): Int {
        val value = readField(target, fieldName)
        return if (value is Rect) value.width() else -1
    }

    private fun readRectHeight(target: Any?, fieldName: String): Int {
        val value = readField(target, fieldName)
        return if (value is Rect) value.height() else -1
    }

    private fun readNestedRectWidth(target: Any?, vararg fieldNames: String): Int {
        val value = readNestedField(target, *fieldNames)
        return if (value is Rect) value.width() else -1
    }

    private fun readNestedRectHeight(target: Any?, vararg fieldNames: String): Int {
        val value = readNestedField(target, *fieldNames)
        return if (value is Rect) value.height() else -1
    }

    private fun readFirstRectWidth(target: Any?): Int {
        val rect = findFirstRect(target, 0)
        return if (rect != null) rect.width() else -1
    }

    private fun readFirstRectHeight(target: Any?): Int {
        val rect = findFirstRect(target, 0)
        return if (rect != null) rect.height() else -1
    }

    private fun readNestedField(target: Any?, vararg fieldNames: String): Any? {
        var current = target
        for (fieldName in fieldNames) {
            current = readField(current, fieldName)
            if (current == null) {
                return null
            }
        }
        return current
    }

    private fun findFirstRect(target: Any?, depth: Int): Rect? {
        if (target == null || depth > 4) {
            return null
        }
        if (target is Rect) {
            return target
        }
        for (field in target.javaClass.declaredFields) {
            if (field.type.isPrimitive || field.type.isEnum) {
                continue
            }
            if (field.name == "this$0") {
                continue
            }
            try {
                field.isAccessible = true
                val value = field.get(target)
                if (value == null || value === target) {
                    continue
                }
                val nestedRect = findFirstRect(value, depth + 1)
                if (nestedRect != null) {
                    return nestedRect
                }
            } catch (_: ReflectiveOperationException) {
                // Probe only.
            }
        }
        return null
    }
}
