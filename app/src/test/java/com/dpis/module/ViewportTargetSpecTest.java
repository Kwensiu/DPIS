package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ViewportTargetSpecTest {
    @Test
    public void relativeScaleUsesMilliPercentFingerprint() {
        ViewportTargetSpec spec = ViewportTargetSpec.relativeScale(106000);

        assertTrue(spec.isRelativeScale());
        assertEquals(106000, spec.scaleMilliPercent());
        assertEquals("r29sg", spec.fingerprint());
    }

    @Test
    public void absoluteDpUsesAbsoluteFingerprint() {
        ViewportTargetSpec spec = ViewportTargetSpec.absoluteDp(900);

        assertTrue(spec.isAbsoluteDp());
        assertEquals(900, spec.absoluteWidthDp());
        assertEquals("ap0", spec.fingerprint());
    }

    @Test
    public void rejectsOutOfRangeRelativeScale() {
        assertFalse(ViewportTargetSpec.relativeScale(29900).isEnabled());
        assertFalse(ViewportTargetSpec.relativeScale(300001).isEnabled());
    }

    @Test
    public void acceptsUpdatedRelativeScaleRange() {
        assertTrue(ViewportTargetSpec.relativeScale(30000).isEnabled());
        assertTrue(ViewportTargetSpec.relativeScale(300000).isEnabled());
    }
}
