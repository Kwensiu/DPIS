package com.dpis.displaytool.scene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SceneRegistry {
    private final List<DisplayScene> coreScenes;
    private final List<DisplayScene> composeScenes;
    private final List<DisplayScene> composeColdStartScenes;

    private SceneRegistry(
            List<DisplayScene> coreScenes,
            List<DisplayScene> composeScenes,
            List<DisplayScene> composeColdStartScenes
    ) {
        this.coreScenes = Collections.unmodifiableList(new ArrayList<>(coreScenes));
        this.composeScenes = Collections.unmodifiableList(new ArrayList<>(composeScenes));
        this.composeColdStartScenes = Collections.unmodifiableList(new ArrayList<>(composeColdStartScenes));
    }

    public static SceneRegistry create() {
        List<DisplayScene> coreScenes = createPhase1Scenes();
        List<DisplayScene> composeScenes = createComposeScenes();
        List<DisplayScene> composeColdStartScenes = Arrays.asList(
                composeScenes.get(0),
                composeScenes.get(2)
        );
        return new SceneRegistry(coreScenes, composeScenes, composeColdStartScenes);
    }

    public static SceneRegistry createPhase1() {
        return new SceneRegistry(
                createPhase1Scenes(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private static List<DisplayScene> createPhase1Scenes() {
        List<DisplayScene> scenes = new ArrayList<>();
        scenes.add(new BaselineTextSpScene());
        scenes.add(new NestedScrollTextScene());
        scenes.add(new RecyclerTextBindScene());
        scenes.add(new DialogTextSpScene());
        scenes.add(new StyledTextAppearanceScene());
        scenes.add(new ProgrammaticTextPxScene());
        return scenes;
    }

    private static List<DisplayScene> createComposeScenes() {
        List<DisplayScene> scenes = new ArrayList<>();
        scenes.add(new ComposeBaselineTextScene());
        scenes.add(new ComposeNestedScrollTextScene());
        scenes.add(new ComposeLazyListTextScene());
        scenes.add(new ComposeStyledTextScene());
        return scenes;
    }

    public List<DisplayScene> coreScenes() {
        return coreScenes;
    }

    public List<DisplayScene> composeScenes() {
        return composeScenes;
    }

    public List<DisplayScene> composeColdStartScenes() {
        return composeColdStartScenes;
    }

    public DisplayScene findById(String sceneId) {
        if (sceneId == null) {
            return null;
        }
        for (DisplayScene scene : coreScenes) {
            if (scene.id().equals(sceneId)) {
                return scene;
            }
        }
        for (DisplayScene scene : composeScenes) {
            if (scene.id().equals(sceneId)) {
                return scene;
            }
        }
        return null;
    }
}
