package com.dpis.displaytool.scene;

import android.app.Dialog;
import android.view.View;
import android.widget.TextView;

import com.dpis.displaytool.R;

final class DialogTextSpScene implements DisplayScene {
    @Override
    public String id() {
        return "dialog_text_sp";
    }

    @Override
    public boolean supportsVariant(String variant) {
        return TextSceneSupport.supportsNormal(variant);
    }

    @Override
    public ScenePresentation create(SceneRuntime runtime, String variant) {
        View root = runtime.inflater().inflate(R.layout.scene_dialog_text_sp, runtime.detailHost(), false);
        TextView textView = root.findViewById(R.id.text_primary);
        TextSceneSupport.applySp(textView, TextSceneSupport.BASE_SP);
        Dialog dialog = new Dialog(runtime.activity());
        dialog.setContentView(root);
        dialog.setCanceledOnTouchOutside(false);
        return ScenePresentation.dialog(
                dialog,
                root,
                textView,
                TextSceneSupport.BASE_SP,
                TextSceneSupport.VIEW_PRIMARY,
                "dialog_shown"
        );
    }
}
