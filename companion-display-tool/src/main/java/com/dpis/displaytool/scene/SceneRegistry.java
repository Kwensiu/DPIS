package com.dpis.displaytool.scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SceneRegistry {
    private final List<DisplayScene> coreScenes;

    private SceneRegistry(List<DisplayScene> coreScenes) {
        this.coreScenes = Collections.unmodifiableList(new ArrayList<>(coreScenes));
    }

    public static SceneRegistry createPhase1() {
        List<DisplayScene> scenes = new ArrayList<>();
        scenes.add(new BaselineTextSpScene());
        scenes.add(new NestedScrollTextScene());
        scenes.add(new RecyclerTextBindScene());
        scenes.add(new DialogTextSpScene());
        scenes.add(new StyledTextAppearanceScene());
        scenes.add(new ProgrammaticTextPxScene());
        return new SceneRegistry(scenes);
    }

    public List<DisplayScene> coreScenes() {
        return coreScenes;
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
        return null;
    }
}
