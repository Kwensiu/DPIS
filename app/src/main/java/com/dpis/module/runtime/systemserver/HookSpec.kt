package com.dpis.module.runtime.systemserver

import java.lang.reflect.Constructor
import java.lang.reflect.Method

class HookSpec private constructor(
    @JvmField val entryName: String,
    @JvmField val classNames: Array<String>,
    @JvmField val methodNames: Array<String>,
    @JvmField val hookIdPrefix: String,
) {
    fun describeClassNames(): String = classNames.joinToString("|")

    fun describeMethodNames(): String =
        if (methodNames.isEmpty()) "<constructor>" else methodNames.joinToString("|")

    fun hookIdFor(method: Method): String =
        hookIdPrefix + "_" + executableSuffix(method.name, method.parameterTypes)

    fun hookIdFor(constructor: Constructor<*>): String =
        hookIdPrefix + "_" + executableSuffix("ctor", constructor.parameterTypes)

    companion object {
        @JvmStatic
        fun method(
            entryName: String,
            hookIdPrefix: String,
            classNames: Array<String>,
            methodNames: Array<String>,
        ): HookSpec = HookSpec(entryName, classNames, methodNames, hookIdPrefix)

        @JvmStatic
        @JvmName("constructor")
        fun newConstructor(
            entryName: String,
            hookIdPrefix: String,
            classNames: Array<String>,
        ): HookSpec = HookSpec(entryName, classNames, emptyArray(), hookIdPrefix)

        private fun executableSuffix(name: String, parameterTypes: Array<Class<*>>?): String {
            val builder = StringBuilder(name)
            builder.append("__")
            if (parameterTypes == null || parameterTypes.isEmpty()) {
                builder.append("noargs")
                return builder.toString()
            }
            for (i in parameterTypes.indices) {
                if (i > 0) {
                    builder.append('_')
                }
                val parameterType = parameterTypes[i]
                val typeName = parameterType.simpleName
                for (c in typeName) {
                    builder.append(if (c.isLetterOrDigit()) c.lowercaseChar() else '_')
                }
            }
            return builder.toString()
        }
    }
}
