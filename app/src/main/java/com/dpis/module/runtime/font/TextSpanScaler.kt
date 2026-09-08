package com.dpis.module.runtime.font

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.RelativeSizeSpan
import com.dpis.module.fonts.FontFieldRewriteMath
import kotlin.math.abs

/** Rewrites explicit text-size spans while preserving an idempotence marker. */
internal object TextSpanScaler {
    fun scale(source: Spanned, factor: Float): CharSequence {
        if (!TextSizePolicy.isScaleFactorActive(factor)) return source
        val markers = source.getSpans(0, source.length, FontScaledMarker::class.java)
        var builder: SpannableStringBuilder? = null
        var changed = false

        for (span in source.getSpans(0, source.length, AbsoluteSizeSpan::class.java)) {
            val start = source.getSpanStart(span)
            val end = source.getSpanEnd(span)
            if (start < 0 || end <= start) continue
            val flags = source.getSpanFlags(span)
            val marker = findMarker(source, markers, start, end, FontScaledMarker.KIND_ABSOLUTE)
            var original = span.size
            if (marker?.matchesFactor(factor) == true && marker.appliedAbsoluteSize == original) continue
            if (marker != null && !marker.matchesFactor(factor)) original = marker.originalAbsoluteSize
            val scaled = FontFieldRewriteMath.scaleAbsoluteSize(original, factor)
            if (scaled == original) continue
            if (builder == null) builder = SpannableStringBuilder(source)
            builder.removeSpan(span)
            if (marker != null) builder.removeSpan(marker)
            builder.setSpan(AbsoluteSizeSpan(scaled, span.dip), start, end, flags)
            builder.setSpan(FontScaledMarker(FontScaledMarker.KIND_ABSOLUTE, factor, original, scaled, 0f, 0f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            changed = true
        }

        for (span in source.getSpans(0, source.length, RelativeSizeSpan::class.java)) {
            val start = source.getSpanStart(span)
            val end = source.getSpanEnd(span)
            if (start < 0 || end <= start) continue
            val flags = source.getSpanFlags(span)
            val marker = findMarker(source, markers, start, end, FontScaledMarker.KIND_RELATIVE)
            var original = span.sizeChange
            if (marker?.matchesFactor(factor) == true && abs(marker.appliedRelativeSize - original) < 0.0001f) continue
            if (marker != null && !marker.matchesFactor(factor)) original = marker.originalRelativeSize
            val scaled = FontFieldRewriteMath.scaleRelativeSize(original, factor)
            if (abs(scaled - original) < 0.0001f) continue
            if (builder == null) builder = SpannableStringBuilder(source)
            builder.removeSpan(span)
            if (marker != null) builder.removeSpan(marker)
            builder.setSpan(RelativeSizeSpan(scaled), start, end, flags)
            builder.setSpan(FontScaledMarker(FontScaledMarker.KIND_RELATIVE, factor, 0, 0, original, scaled), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            changed = true
        }
        return if (changed) builder!! else source
    }

    private fun findMarker(source: Spanned, markers: Array<FontScaledMarker>, start: Int, end: Int, kind: Int): FontScaledMarker? =
        markers.firstOrNull { source.getSpanStart(it) == start && source.getSpanEnd(it) == end && it.kind == kind }
}

internal class FontScaledMarker(
    val kind: Int,
    val factor: Float,
    val originalAbsoluteSize: Int,
    val appliedAbsoluteSize: Int,
    val originalRelativeSize: Float,
    val appliedRelativeSize: Float
) {
    fun matchesFactor(candidate: Float): Boolean = abs(factor - candidate) <= 0.001f

    companion object {
        const val KIND_ABSOLUTE = 1
        const val KIND_RELATIVE = 2
    }
}
