package com.dpis.module.runtime.systemserver

import android.content.res.Configuration
import java.lang.reflect.Field
import java.lang.reflect.Method

/** Cached reflective reads used by system_server hooks. */
object Reflection {
    private val cache = ReflectionProbeCache()

    @JvmStatic
    fun findNoArgMethod(type: Class<*>?, methodName: String?): Method? =
        cache.findNoArgMethod(type, methodName)

    @JvmStatic
    fun invokeStringMethod(target: Any?, methodName: String?): String? {
        if (target == null) {
            return null
        }
        return try {
            val method = cache.findNoArgMethod(target.javaClass, methodName) ?: return null
            val value = method.invoke(target)
            value as? String
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    @JvmStatic
    fun invokeConfigurationMethod(target: Any?, methodName: String?): Configuration? {
        if (target == null) {
            return null
        }
        return try {
            val method = cache.findNoArgMethod(target.javaClass, methodName) ?: return null
            val value = method.invoke(target)
            value as? Configuration
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    @JvmStatic
    fun invokeObjectMethod(target: Any?, methodName: String?): Any? {
        if (target == null) {
            return null
        }
        return try {
            val method = cache.findNoArgMethod(target.javaClass, methodName) ?: return null
            method.invoke(target)
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    @JvmStatic
    fun readField(target: Any?, fieldName: String?): Any? {
        if (target == null) {
            return null
        }
        val field = cache.findField(target.javaClass, fieldName) ?: return null
        return try {
            field.isAccessible = true
            field.get(target)
        } catch (_: ReflectiveOperationException) {
            null
        }
    }

    @JvmStatic
    fun getAllFields(clazz: Class<*>?): List<Field> = cache.getAllFields(clazz)

    @JvmStatic
    fun readIntField(target: Any?, fieldName: String?): Int? {
        val value = readField(target, fieldName)
        return value as? Int
    }

    @JvmStatic
    fun writeIntField(target: Any?, fieldName: String?, value: Int): Boolean {
        if (target == null) {
            return false
        }
        val field = cache.findField(target.javaClass, fieldName) ?: return false
        return try {
            field.isAccessible = true
            val previous = field.getInt(target)
            if (previous == value) {
                return false
            }
            field.setInt(target, value)
            true
        } catch (_: ReflectiveOperationException) {
            false
        }
    }
}
