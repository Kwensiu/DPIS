package com.dpis.module.templates;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.dpis.module.config.PackageConfigValue;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import org.junit.Test;

public class TemplateConfigValueAdaptersTest {
    @Test
    public void relativeViewportPreservesItsRuntimeScaleAndIgnoresAbsoluteDraft() {
        TemplateConfigValue value = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(125000),
                ViewportTargetType.ABSOLUTE_DP,
                180000,
                411,
                "system",
                130,
                "field_rewrite",
                "font_a",
                "resources_font");

        assertEquals(ViewportTargetType.RELATIVE_SCALE, value.viewportTargetType);
        assertEquals(Integer.valueOf(125000), value.viewportScaleMilliPercent);
        assertNull(value.viewportWidthDp);
        assertEquals(ViewportTargetSpec.relativeScale(125000),
                TemplateConfigValueAdapters.toViewportTargetSpec(value));
    }

    @Test
    public void absoluteViewportPreservesItsRuntimeWidthAndIgnoresScaleDraft() {
        TemplateConfigValue value = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.absoluteDp(411),
                ViewportTargetType.RELATIVE_SCALE,
                180000,
                411,
                "compat",
                null,
                null,
                null,
                null);

        assertEquals(ViewportTargetType.ABSOLUTE_DP, value.viewportTargetType);
        assertNull(value.viewportScaleMilliPercent);
        assertEquals(Integer.valueOf(411), value.viewportWidthDp);
        PackageConfigValue packageValue = TemplateConfigValueAdapters.toPackageConfigValue(value);
        assertEquals(ViewportTargetSpec.absoluteDp(411), packageValue.viewportTargetSpec);
    }

    @Test
    public void disabledViewportUsesNormalizedIntentButDoesNotCreateRuntimeOverride() {
        TemplateConfigValue value = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                "unknown",
                125000,
                411,
                "auto",
                null,
                null,
                null,
                null);

        assertEquals(ViewportTargetType.OFF, value.viewportTargetType);
        assertEquals(ViewportTargetSpec.off(), TemplateConfigValueAdapters.toViewportTargetSpec(value));
    }
}
