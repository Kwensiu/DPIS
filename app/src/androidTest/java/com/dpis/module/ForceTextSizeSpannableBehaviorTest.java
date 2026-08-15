package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dpis.module.runtime.font.ForceTextSizeHookInstaller;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

/** Exercises framework-backed span mutation that JVM Android stubs cannot execute. */
@RunWith(AndroidJUnit4.class)
public class ForceTextSizeSpannableBehaviorTest {
    @Test
    public void reusedSpannableScalesSpanAddedAfterFirstRewrite() throws Exception {
        SpannableStringBuilder text = new SpannableStringBuilder("qq");
        text.setSpan(new AbsoluteSizeSpan(20), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        CharSequence first = scaleSpans(text, 1.5f);
        assertTrue(first instanceof SpannableStringBuilder);
        SpannableStringBuilder reused = (SpannableStringBuilder) first;
        AbsoluteSizeSpan addedLater = new AbsoluteSizeSpan(20);
        reused.setSpan(addedLater, 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        reused = (SpannableStringBuilder) scaleSpans(reused, 1.5f);

        AbsoluteSizeSpan[] firstSpan = reused.getSpans(0, 1, AbsoluteSizeSpan.class);
        AbsoluteSizeSpan[] addedSpan = reused.getSpans(1, 2, AbsoluteSizeSpan.class);
        assertEquals(1, firstSpan.length);
        assertEquals(1, addedSpan.length);
        assertEquals(30, firstSpan[0].getSize());
        assertEquals(30, addedSpan[0].getSize());
    }

    @Test
    public void shiftedSpannableDoesNotRescaleExistingSpan() throws Exception {
        SpannableStringBuilder text = new SpannableStringBuilder("qq");
        text.setSpan(new AbsoluteSizeSpan(20), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableStringBuilder rewritten =
                (SpannableStringBuilder) scaleSpans(text, 1.5f);
        rewritten.insert(0, "x");
        rewritten = (SpannableStringBuilder) scaleSpans(rewritten, 1.5f);

        AbsoluteSizeSpan[] spans = rewritten.getSpans(1, 2, AbsoluteSizeSpan.class);
        assertEquals(1, spans.length);
        assertEquals(30, spans[0].getSize());
    }

    @Test
    public void changedFactorRebasesExistingAbsoluteSpan() throws Exception {
        SpannableStringBuilder text = new SpannableStringBuilder("qq");
        text.setSpan(new AbsoluteSizeSpan(20), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableStringBuilder rewritten =
                (SpannableStringBuilder) scaleSpans(text, 1.5f);
        rewritten = (SpannableStringBuilder) scaleSpans(rewritten, 1.3f);

        AbsoluteSizeSpan[] spans = rewritten.getSpans(0, 1, AbsoluteSizeSpan.class);
        assertEquals(1, spans.length);
        assertEquals(26, spans[0].getSize());
    }

    @Test
    public void relativeSpansScaleOnceAndNewRelativeSpanIsProcessed() throws Exception {
        SpannableStringBuilder text = new SpannableStringBuilder("qq");
        text.setSpan(new RelativeSizeSpan(0.8f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableStringBuilder rewritten =
                (SpannableStringBuilder) scaleSpans(text, 1.5f);
        RelativeSizeSpan addedLater = new RelativeSizeSpan(0.8f);
        rewritten.setSpan(addedLater, 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        rewritten = (SpannableStringBuilder) scaleSpans(rewritten, 1.5f);

        RelativeSizeSpan[] firstSpan = rewritten.getSpans(0, 1, RelativeSizeSpan.class);
        RelativeSizeSpan[] addedSpan = rewritten.getSpans(1, 2, RelativeSizeSpan.class);
        assertEquals(1, firstSpan.length);
        assertEquals(1, addedSpan.length);
        assertEquals(1.2f, firstSpan[0].getSizeChange(), 0.0001f);
        assertEquals(1.2f, addedSpan[0].getSizeChange(), 0.0001f);
    }

    @Test
    public void copiedSpannableDoesNotRescaleExistingAbsoluteSpan() throws Exception {
        SpannableStringBuilder text = new SpannableStringBuilder("qq");
        text.setSpan(new AbsoluteSizeSpan(20), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableStringBuilder rewritten =
                (SpannableStringBuilder) scaleSpans(text, 1.5f);
        SpannableStringBuilder copied = new SpannableStringBuilder(rewritten);
        copied = (SpannableStringBuilder) scaleSpans(copied, 1.5f);

        AbsoluteSizeSpan[] spans = copied.getSpans(0, 1, AbsoluteSizeSpan.class);
        assertEquals(1, spans.length);
        assertEquals(30, spans[0].getSize());
    }

    private static CharSequence scaleSpans(Spanned source, float factor) throws Exception {
        Method method = ForceTextSizeHookInstaller.class.getDeclaredMethod(
                "scaleSpans", Spanned.class, float.class);
        method.setAccessible(true);
        return (CharSequence) method.invoke(null, source, factor);
    }
}
