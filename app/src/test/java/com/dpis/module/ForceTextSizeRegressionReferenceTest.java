package com.dpis.module;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForceTextSizeRegressionReferenceTest {
    @Test
    public void markdownSpanReference_scalesAbsoluteAndRelative() {
        assertEquals(40, FontFieldRewriteMath.scaleAbsoluteSize(20, 2.0f));
        assertEquals(2.4f, FontFieldRewriteMath.scaleRelativeSize(1.2f, 2.0f), 0.0001f);
    }

    @Test
    public void textSizeScalingReference_doesNotDoubleScaleSameViewState() {
        Map<Object, Float> base = new HashMap<>();
        Object key = new Object();

        float first = FontFieldRewriteMath.resolveScaledTextSize(18f, 2.0f, base, key);
        float second = FontFieldRewriteMath.resolveScaledTextSize(36f, 2.0f, base, key);

        assertEquals(36f, first, 0.0001f);
        assertEquals(36f, second, 0.0001f);
    }

    @Test
    public void textSizeScalingReference_rebasesWhenCurrentClearlyChanges() {
        Map<Object, Float> base = new HashMap<>();
        Object key = new Object();

        FontFieldRewriteMath.resolveScaledTextSize(18f, 2.0f, base, key);
        float rebased = FontFieldRewriteMath.resolveScaledTextSize(22f, 2.0f, base, key);

        assertEquals(44f, rebased, 0.0001f);
    }

    @Test
    public void commentHintReference_identifiesCommentLikeAndNonCommentLike() {
        assertTrue(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.comment.CommentTextView"));
        assertTrue(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.reply.ReplyItem"));
        assertTrue(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.bbs.HbLineHeightView"));
        assertFalse(FontFieldRewriteMath.containsCommentHint("com.max.xiaoheihe.feed.NormalTitleView"));
    }
}
