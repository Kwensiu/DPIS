package com.dpis.module.runtime.probe

object CallerTrace {
    @JvmStatic
    fun findRelevantCaller(
        frames: List<StackTraceElement>?,
        targetPackage: String?,
    ): String? {
        if (frames.isNullOrEmpty()) {
            return null
        }
        frames.firstOrNull { frame ->
            val className = frame.className
            !isIgnored(className) &&
                    !targetPackage.isNullOrEmpty() &&
                    className.startsWith(targetPackage)
        }?.let(::format)?.also { return it }

        return frames.firstOrNull { !isIgnored(it.className) }?.let(::format)
    }

    @JvmStatic
    fun capture(targetPackage: String?): String? =
        findRelevantCaller(Throwable().stackTrace.toList(), targetPackage)

    private fun isIgnored(className: String): Boolean =
        className.startsWith("com.dpis.module.") ||
                className.startsWith("android.") ||
                className.startsWith("com.android.") ||
                className.startsWith("java.") ||
                className.startsWith("javax.") ||
                className.startsWith("kotlin.") ||
                className.startsWith("dalvik.") ||
                className.startsWith("sun.") ||
                className.startsWith("libcore.") ||
                className.startsWith("io.github.libxposed.")

    private fun format(frame: StackTraceElement): String =
        "${frame.className}#${frame.methodName}:${frame.lineNumber}"
}
