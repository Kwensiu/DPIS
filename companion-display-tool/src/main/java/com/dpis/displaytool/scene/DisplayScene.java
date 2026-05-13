package com.dpis.displaytool.scene;

public interface DisplayScene {
    String id();

    boolean supportsVariant(String variant);

    ScenePresentation create(SceneRuntime runtime, String variant);
}
