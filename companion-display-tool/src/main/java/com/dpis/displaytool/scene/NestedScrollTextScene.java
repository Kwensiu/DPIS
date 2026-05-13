package com.dpis.displaytool.scene;

import android.view.View;
import android.widget.TextView;

import com.dpis.displaytool.R;

final class NestedScrollTextScene implements DisplayScene {
    @Override
    public String id() {
        return "nested_scroll_text";
    }

    @Override
    public boolean supportsVariant(String variant) {
        return TextSceneSupport.supportsNormalFragile(variant);
    }

    @Override
    public ScenePresentation create(SceneRuntime runtime, String variant) {
        View root = runtime.inflater().inflate(R.layout.scene_nested_scroll_text, runtime.detailHost(), false);
        TextView textView = root.findViewById(R.id.text_primary);
        if (TextSceneSupport.isFragile(variant)) {
            TextSceneSupport.applyDoubleScaledPx(textView, TextSceneSupport.BASE_SP);
        } else {
            TextSceneSupport.applySp(textView, TextSceneSupport.BASE_SP);
        }
        return ScenePresentation.view(
                root,
                textView,
                TextSceneSupport.BASE_SP,
                TextSceneSupport.VIEW_PRIMARY,
                TextSceneSupport.EVENT_FIRST_LAYOUT
        );
    }
}
