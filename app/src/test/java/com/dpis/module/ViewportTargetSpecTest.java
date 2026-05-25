package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ViewportTargetSpecTest {
    @Test
    public void relativeScaleUsesPermilleFingerprint() {
        ViewportTargetSpec spec = ViewportTargetSpec.relativeScale(1060);

        assertTrue(spec.isRelativeScale());
        assertEquals(1060, spec.scalePermille());
        assertEquals("rtg", spec.fingerprint());
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
        assertFalse(ViewportTargetSpec.relativeScale(299).isEnabled());
        assertFalse(ViewportTargetSpec.relativeScale(3001).isEnabled());
    }

    @Test
    public void acceptsUpdatedRelativeScaleRange() {
        assertTrue(ViewportTargetSpec.relativeScale(300).isEnabled());
        assertTrue(ViewportTargetSpec.relativeScale(3000).isEnabled());
    }
}
