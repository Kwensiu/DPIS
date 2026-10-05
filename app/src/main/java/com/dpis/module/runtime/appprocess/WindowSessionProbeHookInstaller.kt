package com.dpis.module.runtime.appprocess

import android.graphics.Rect
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.runtime.ProcessScopedInstallGate
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.Volatile

object WindowSessionProbeHookInstaller {
    private const val MAX_LOGS = 12
    private val LOG_COUNT = AtomicInteger()

    @Volatile
    private var installedPid = -1

    @Volatile
    private var lastLog: String? = null

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
        LOG_COUNT.set(0)
        lastLog = null
    }

    @JvmStatic
    @Throws(ReflectiveOperationException::class)
    fun install(xposed: XposedInterface) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return
        }
        synchronized(WindowSessionProbeHookInstaller::class.java) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return
            }
            val bootClassLoader = ClassLoader.getSystemClassLoader()
            for (clazz in resolveSessionClasses(bootClassLoader)) {
                hookNamedMethods(xposed, clazz, "relayout")
                hookNamedMethods(xposed, clazz, "relayoutAsync")
            }
            installedPid = ProcessScopedInstallGate.currentPid()
            DpisLog.i("WindowSession probe hook ready")
        }
    }

    @JvmStatic
    fun buildLog(
        methodName: String?,
        stage: String?,
        result: Int,
        frameWidth: Int,
        frameHeight: Int,
    ): String {
        if (stage == "before") {
            return "WindowSession probe($methodName:$stage): frame=${frameWidth}x$frameHeight"
        }
        return "WindowSession probe($methodName:$stage): result=$result, frame=${frameWidth}x$frameHeight"
    }

    @JvmStatic
    fun findFrameForTest(args: List<*>): Rect? {
        return findFrame(args)
    }

    @Throws(ReflectiveOperationException::class)
    private fun resolveSessionClasses(classLoader: ClassLoader): Set<Class<*>> {
        val classes = LinkedHashSet<Class<*>>()
        classes.add(Class.forName("android.view.IWindowSession\$Stub\$Proxy", false, classLoader))
        return classes
    }

    @Throws(ReflectiveOperationException::class)
    private fun hookNamedMethods(
        xposed: XposedInterface,
        targetClass: Class<*>,
        methodName: String,
    ) {
        for (method: Method in targetClass.declaredMethods) {
            if (methodName != method.name) {
                continue
            }
            if (Modifier.isAbstract(method.modifiers)) {
                continue
            }
            xposed.hook(method)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val active = chain!!
                    log(methodName, "before", -1, active.args)
                    val result = active.proceed()
                    log(
                        methodName,
                        "after",
                        if (result is Int) result else -1,
                        active.args,
                    )
                    result
                })
        }
    }

    private fun log(methodName: String, stage: String, result: Int, args: List<*>) {
        if (LOG_COUNT.incrementAndGet() > MAX_LOGS) {
            return
        }
        val frame = findFrame(args)
        val width = if (frame != null) frame.width() else -1
        val height = if (frame != null) frame.height() else -1
        val message = buildLog(methodName, stage, result, width, height)
        if (message != lastLog) {
            lastLog = message
            DpisLog.i(message)
        }
    }

    private fun findFrame(args: List<*>): Rect? {
        for (arg in args) {
            val frame = findRectRecursive(arg, 0)
            if (frame != null) {
                return frame
            }
        }
        return null
    }

    private fun findRectRecursive(target: Any?, depth: Int): Rect? {
        if (target == null || depth > 4) {
            return null
        }
        if (target is Rect) {
            return target
        }
        val directFrame = readRectField(target, "frame")
        if (directFrame != null) {
            return directFrame
        }
        for (fieldName in arrayOf("frames", "windowFrames", "outFrames", "result")) {
            val nested = readField(target, fieldName)
            val nestedRect = findRectRecursive(nested, depth + 1)
            if (nestedRect != null) {
                return nestedRect
            }
        }
        for (field in target.javaClass.declaredFields) {
            if (field.type.isPrimitive || field.type.isEnum) {
                continue
            }
            try {
                field.isAccessible = true
                val nested = field.get(target)
                if (nested == null || nested === target) {
                    continue
                }
                if (field.name == "this$0") {
                    continue
                }
                val nestedRect = findRectRecursive(nested, depth + 1)
                if (nestedRect != null) {
                    return nestedRect
                }
            } catch (_: ReflectiveOperationException) {
                // Keep probing other fields.
            }
        }
        return null
    }

    private fun readRectField(target: Any?, fieldName: String): Rect? {
        val value = readField(target, fieldName)
        return if (value is Rect) value else null
    }

    private fun readField(target: Any?, fieldName: String): Any? {
        if (target == null) {
            return null
        }
        return try {
            val field: Field = target.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.get(target)
        } catch (_: ReflectiveOperationException) {
            null
        }
    }
}
