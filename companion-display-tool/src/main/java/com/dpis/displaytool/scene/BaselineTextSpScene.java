package com.dpis.displaytool.scene;

import android.view.View;
import android.widget.TextView;

import com.dpis.displaytool.R;

final class BaselineTextSpScene implements DisplayScene {
    @Override
    public String id() {
        return "baseline_text_sp";
    }

    @Override
    public boolean supportsVariant(String variant) {
        return TextSceneSupport.supportsNormal(variant);
    }

    @Override
    public ScenePresentation create(SceneRuntime runtime, String variant) {
        View root = runtime.inflater().inflate(R.layout.scene_baseline_text_sp, runtime.detailHost(), false);
        TextView textView = root.findViewById(R.id.text_primary);
        TextSceneSupport.applySp(textView, TextSceneSupport.BASE_SP);
        return ScenePresentation.view(
                root,
                textView,
                TextSceneSupport.BASE_SP,
                TextSceneSupport.VIEW_PRIMARY,
                TextSceneSupport.EVENT_FIRST_LAYOUT
        );
    }
}
