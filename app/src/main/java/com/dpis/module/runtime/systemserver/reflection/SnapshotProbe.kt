package com.dpis.module.runtime.systemserver.reflection

import android.content.res.Configuration
import android.graphics.Rect
import com.dpis.module.runtime.systemserver.HookInstaller

/** Configuration, window frame, and DisplayInfo carried on a system_server hook argument. */
object SnapshotProbe {
    private val DISPLAY_INFO_FIELD_NAMES = arrayOf(
        "displayInfo",
        "mDisplayInfo",
        "mTmpDisplayInfo",
        "mLastDisplayInfo",
    )
    private val FRAME_DIRECT_FIELD_NAMES = arrayOf(
        "frame", "mFrame", "displayFrame",
    )
    private val FRAME_NESTED_FIELD_NAMES = arrayOf(
        "frames", "windowFrames", "clientWindowFrames", "outFrames", "result",
    )

    @JvmStatic
    fun configuration(self: Any?, args: List<*>?): Configuration? {
        findConfiguration(self, 0)?.let { return it }
        if (args == null) {
            return null
        }
        for (arg in args) {
            findConfiguration(arg, 0)?.let { return it }
        }
        return null
    }

    @JvmStatic
    fun frame(self: Any?, args: List<*>?): Rect? {
        findFrame(self, 0)?.let { return it }
        if (args == null) {
            return null
        }
        for (arg in args) {
            findFrame(arg, 0)?.let { return it }
        }
        return null
    }

    @JvmStatic
    fun displayInfo(self: Any?, args: List<*>?): Any? {
        findDisplayInfo(self, 0)?.let { return it }
        if (args == null) {
            return null
        }
        for (arg in args) {
            findDisplayInfo(arg, 0)?.let { return it }
        }
        return null
    }

    private fun findConfiguration(target: Any?, depth: Int): Configuration? {
        if (target == null || depth > 3) {
            return null
        }
        if (target is Configuration) {
            return target
        }
        for (fieldName in HookInstaller.CONFIGURATION_FIELD_NAMES) {
            val value = Reflection.readField(target, fieldName)
            if (value is Configuration) {
                return value
            }
            findConfiguration(value, depth + 1)?.let { return it }
        }
        Reflection.invokeConfigurationMethod(
            target, "getMergedConfiguration",
        )?.let { return it }
        Reflection.invokeConfigurationMethod(
            target, "getConfiguration",
        )?.let { return it }
        return walkFields(target, depth, ::findConfiguration)
    }

    private fun findDisplayInfo(target: Any?, depth: Int): Any? {
        if (target == null || depth > 3) {
            return null
        }
        if (target.javaClass.name == "android.view.DisplayInfo") {
            return target
        }
        for (fieldName in DISPLAY_INFO_FIELD_NAMES) {
            val value = Reflection.readField(target, fieldName)
            if (value != null && value.javaClass.name == "android.view.DisplayInfo") {
                return value
            }
            findDisplayInfo(value, depth + 1)?.let { return it }
        }
        val fromMethod = Reflection.invokeObjectMethod(
            target, "getDisplayInfo",
        )
        if (fromMethod != null && fromMethod.javaClass.name == "android.view.DisplayInfo") {
            return fromMethod
        }
        return walkFields(target, depth, ::findDisplayInfo)
    }

    private fun findFrame(target: Any?, depth: Int): Rect? {
        if (target == null || depth > 4) {
            return null
        }
        if (target is Rect) {
            return target
        }
        for (fieldName in FRAME_DIRECT_FIELD_NAMES) {
            val value = Reflection.readField(target, fieldName)
            if (value is Rect) {
                return value
            }
        }
        for (fieldName in FRAME_NESTED_FIELD_NAMES) {
            val nested = Reflection.readField(target, fieldName)
            findFrame(nested, depth + 1)?.let { return it }
        }
        return walkFields(target, depth, ::findFrame)
    }

    private fun <T> walkFields(target: Any, depth: Int, visit: (Any?, Int) -> T?): T? {
        for (field in Reflection.getAllFields(target.javaClass)) {
            if (field.type.isPrimitive || field.type.isEnum) {
                continue
            }
            try {
                field.isAccessible = true
                val nested = field.get(target)
                if (nested == null || nested === target || field.name == "this\$0") {
                    continue
                }
                visit(nested, depth + 1)?.let { return it }
            } catch (_: ReflectiveOperationException) {
                // Continue probing.
            }
        }
        return null
    }
}
