package com.dpis.displaytool.scene;

import android.app.Activity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

public final class SceneRuntime {
    private final Activity activity;
    private final FrameLayout detailHost;
    private final String sceneId;
    private final String variant;

    public SceneRuntime(Activity activity, FrameLayout detailHost, String sceneId, String variant) {
        this.activity = activity;
        this.detailHost = detailHost;
        this.sceneId = sceneId;
        this.variant = variant;
    }

    public Activity activity() {
        return activity;
    }

    public LayoutInflater inflater() {
        return LayoutInflater.from(activity);
    }

    public FrameLayout detailHost() {
        return detailHost;
    }

    public String sceneId() {
        return sceneId;
    }

    public String variant() {
        return variant;
    }
}
