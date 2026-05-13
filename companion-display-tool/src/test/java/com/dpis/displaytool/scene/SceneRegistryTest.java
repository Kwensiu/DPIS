package com.dpis.displaytool.scene;

import com.dpis.displaytool.CompanionContract;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class SceneRegistryTest {
    @Test
    public void coreScenesStayInPhase1ColdStartOrder() {
        SceneRegistry registry = SceneRegistry.createPhase1();

        List<String> ids = Arrays.asList(
                registry.coreScenes().get(0).id(),
                registry.coreScenes().get(1).id(),
                registry.coreScenes().get(2).id(),
                registry.coreScenes().get(3).id(),
                registry.coreScenes().get(4).id(),
                registry.coreScenes().get(5).id()
        );

        assertEquals(Arrays.asList(
                "baseline_text_sp",
                "nested_scroll_text",
                "recycler_text_bind",
                "dialog_text_sp",
                "styled_text_appearance",
                "programmatic_text_px"
        ), ids);
    }

    @Test
    public void phase1FragileMatrixMatchesDesign() {
        SceneRegistry registry = SceneRegistry.createPhase1();

        assertSupportsOnlyNormal(registry, "baseline_text_sp");
        assertSupportsNormalAndFragile(registry, "nested_scroll_text");
        assertSupportsNormalAndFragile(registry, "recycler_text_bind");
        assertSupportsOnlyNormal(registry, "dialog_text_sp");
        assertSupportsNormalAndFragile(registry, "styled_text_appearance");
        assertSupportsNormalAndFragile(registry, "programmatic_text_px");
    }

    private static void assertSupportsOnlyNormal(SceneRegistry registry, String sceneId) {
        DisplayScene scene = registry.findById(sceneId);
        assertNotNull(scene);
        assertTrue(scene.supportsVariant(CompanionContract.VARIANT_NORMAL));
        assertFalse(scene.supportsVariant(CompanionContract.VARIANT_FRAGILE));
    }

    private static void assertSupportsNormalAndFragile(SceneRegistry registry, String sceneId) {
        DisplayScene scene = registry.findById(sceneId);
        assertNotNull(scene);
        assertTrue(scene.supportsVariant(CompanionContract.VARIANT_NORMAL));
        assertTrue(scene.supportsVariant(CompanionContract.VARIANT_FRAGILE));
    }
}
