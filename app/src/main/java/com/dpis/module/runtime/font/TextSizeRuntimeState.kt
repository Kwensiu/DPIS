package com.dpis.module.runtime.font

import android.widget.TextView
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/** Process-local mutable state shared by the font hook families. */
internal object TextSizeRuntimeState {
    @Volatile
    var installedPid: Int = -1
    val lastMessages: MutableMap<String?, String?> = ConcurrentHashMap()
    val hotLogCounts: MutableMap<String?, Int?> = ConcurrentHashMap()
    val callerSampleCounts: MutableMap<String?, Int?> = ConcurrentHashMap()
    val callerSourceCounts: MutableMap<String?, Int?> = ConcurrentHashMap()
    val internalUpdate: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    val internalTextUpdate: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    val textViewSetTextSizeDepth: ThreadLocal<Int> = ThreadLocal.withInitial { 0 }
    val expressionBaseTextSizes: MutableMap<TextView?, Float?> =
        Collections.synchronizedMap(WeakHashMap())
    val textViewBaseTextSizes: MutableMap<TextView?, Float?> =
        Collections.synchronizedMap(WeakHashMap())
    val commentTextBaseTextSizes: MutableMap<TextView?, Float?> =
        Collections.synchronizedMap(WeakHashMap())
    val lastTargetTextSizes: MutableMap<TextView?, Any?> =
        Collections.synchronizedMap(WeakHashMap())
}
