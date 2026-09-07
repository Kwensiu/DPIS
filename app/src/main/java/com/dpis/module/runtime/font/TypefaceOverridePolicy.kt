package com.dpis.module.runtime.font

import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import java.lang.reflect.Method

/** Shared, API-neutral decisions used by both Xposed hook implementations. */
object TypefaceOverridePolicy {
    fun resolveReplacement(
        baseTypeface: Typeface?,
        original: Typeface?,
        explicitStyle: Int?
    ): Typeface? {
        if (baseTypeface == null) return original
        return try {
            Typeface.create(baseTypeface, resolveStyle(original?.style, explicitStyle))
                ?: baseTypeface
        } catch (_: Throwable) {
            baseTypeface
        }
    }

    fun resolveStyle(originalStyle: Int?, explicitStyle: Int?): Int =
        explicitStyle ?: originalStyle ?: Typeface.NORMAL

    @Throws(NoSuchMethodException::class)
    fun findOnAttachedToWindowMethod(textViewClass: Class<*>): Method = try {
        textViewClass.getDeclaredMethod("onAttachedToWindow")
    } catch (_: NoSuchMethodException) {
        View::class.java.getDeclaredMethod("onAttachedToWindow")
    }

    fun applyTextViewTypeface(
        textView: TextView,
        replacement: Typeface?,
        explicitStyle: Int?,
        internalUpdate: ThreadLocal<Boolean?>
    ) {
        internalUpdate.set(true)
        try {
            if (explicitStyle != null) textView.setTypeface(replacement, explicitStyle)
            else textView.setTypeface(replacement)
        } finally {
            internalUpdate.remove()
        }
    }
}
