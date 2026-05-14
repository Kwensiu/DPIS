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

    @Test
    public void createIncludesPhase1CoreScenesInExistingOrder() {
        SceneRegistry registry = SceneRegistry.create();

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
    public void composeScenesStayInPhase2Order() {
        SceneRegistry registry = SceneRegistry.create();

        List<String> coldStartIds = Arrays.asList(
                registry.composeColdStartScenes().get(0).id(),
                registry.composeColdStartScenes().get(1).id()
        );

        assertEquals(Arrays.asList(
                "compose_baseline_text",
                "compose_lazy_list_text"
        ), coldStartIds);

        List<String> allComposeIds = Arrays.asList(
                registry.composeScenes().get(0).id(),
                registry.composeScenes().get(1).id(),
                registry.composeScenes().get(2).id(),
                registry.composeScenes().get(3).id()
        );

        assertEquals(Arrays.asList(
                "compose_baseline_text",
                "compose_nested_scroll_text",
                "compose_lazy_list_text",
                "compose_styled_text"
        ), allComposeIds);
    }

    @Test
    public void composeScenesSupportOnlyNormalVariant() {
        SceneRegistry registry = SceneRegistry.create();

        assertSupportsOnlyNormal(registry, "compose_baseline_text");
        assertSupportsOnlyNormal(registry, "compose_nested_scroll_text");
        assertSupportsOnlyNormal(registry, "compose_lazy_list_text");
        assertSupportsOnlyNormal(registry, "compose_styled_text");
    }

    @Test
    public void phase1FactoryDoesNotIncludeComposeScenes() {
        SceneRegistry registry = SceneRegistry.createPhase1();

        assertTrue(registry.composeScenes().isEmpty());
        assertTrue(registry.composeColdStartScenes().isEmpty());
        assertEquals(null, registry.findById("compose_baseline_text"));
    }

    @Test
    public void createFindsNativeAndComposeScenes() {
        SceneRegistry registry = SceneRegistry.create();

        assertNotNull(registry.findById("baseline_text_sp"));
        assertNotNull(registry.findById("compose_styled_text"));
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
