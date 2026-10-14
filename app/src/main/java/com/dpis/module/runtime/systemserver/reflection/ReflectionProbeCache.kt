package com.dpis.module.runtime.systemserver.reflection

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Collections
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

class ReflectionProbeCache {
    private val allFieldsCache = ConcurrentHashMap<Class<*>, List<Field>>()
    private val fieldCache = ConcurrentHashMap<FieldKey, Optional<Field>>()
    private val noArgMethodCache = ConcurrentHashMap<MethodKey, Optional<Method>>()

    fun getAllFields(type: Class<*>?): List<Field> {
        if (type == null) {
            return Collections.emptyList()
        }
        return allFieldsCache.computeIfAbsent(type, ::collectAllFields)
    }

    fun findField(type: Class<*>?, fieldName: String?): Field? {
        if (type == null || fieldName.isNullOrEmpty()) {
            return null
        }
        return fieldCache.computeIfAbsent(FieldKey(type, fieldName), ::resolveField).orElse(null)
    }

    fun findNoArgMethod(type: Class<*>?, methodName: String?): Method? {
        if (type == null || methodName.isNullOrEmpty()) {
            return null
        }
        return noArgMethodCache.computeIfAbsent(MethodKey(type, methodName), ::resolveNoArgMethod)
            .orElse(null)
    }

    private fun collectAllFields(type: Class<*>): List<Field> {
        val fields = ArrayList<Field>()
        var current: Class<*>? = type
        while (current != null) {
            Collections.addAll(fields, *current.declaredFields)
            current = current.superclass
        }
        return Collections.unmodifiableList(fields)
    }

    private fun resolveField(key: FieldKey): Optional<Field> {
        var current: Class<*>? = key.type
        while (current != null) {
            try {
                return Optional.of(current.getDeclaredField(key.name))
            } catch (_: ReflectiveOperationException) {
                current = current.superclass
            }
        }
        return Optional.empty()
    }

    private fun resolveNoArgMethod(key: MethodKey): Optional<Method> =
        try {
            Optional.of(key.type.getMethod(key.name))
        } catch (_: ReflectiveOperationException) {
            Optional.empty()
        }

    private data class FieldKey(val type: Class<*>, val name: String)

    private data class MethodKey(val type: Class<*>, val name: String)
}
