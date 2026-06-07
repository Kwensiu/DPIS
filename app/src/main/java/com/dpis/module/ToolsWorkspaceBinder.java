package com.dpis.module;

import android.content.Intent;
import android.view.View;

final class ToolsWorkspaceBinder {
    private final LocalizedActivity activity;
    private SystemFontScaleToolBinder fontScaleToolBinder;

    ToolsWorkspaceBinder(LocalizedActivity activity) {
        this.activity = activity;
    }

    void bind(View workspaceView) {
        if (workspaceView == null || fontScaleToolBinder != null) {
            return;
        }
        fontScaleToolBinder = new SystemFontScaleToolBinder(activity, workspaceView);
        fontScaleToolBinder.bind();
    }

    void onStart() {
        if (fontScaleToolBinder != null) {
            fontScaleToolBinder.refreshFromSystem();
        }
    }

    void onResume() {
        if (fontScaleToolBinder != null) {
            fontScaleToolBinder.refreshFromSystem();
        }
    }

    void onStop() {
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (fontScaleToolBinder != null) {
            fontScaleToolBinder.refreshFromSystem();
        }
    }
}
