package com.dpis.module;

import android.content.Intent;
import android.view.View;

final class SettingsWorkspaceBinder {
    private final LocalizedActivity activity;
    private SystemServerSettingsPageController controller;

    SettingsWorkspaceBinder(LocalizedActivity activity) {
        this.activity = activity;
    }

    void bind(View workspaceView) {
        if (workspaceView == null || controller != null) {
            return;
        }
        controller = new SystemServerSettingsPageController(
                activity,
                workspaceView);
        controller.bind();
    }

    void onStart() {
        if (controller != null) {
            controller.onStart();
        }
    }

    void onResume() {
        if (controller != null) {
            controller.onResume();
        }
    }

    void onStop() {
        if (controller != null) {
            controller.onStop();
        }
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (controller != null) {
            controller.onActivityResult(requestCode, resultCode, data);
        }
    }

    void onServiceStateChanged() {
        if (controller != null) {
            controller.onServiceStateChanged();
        }
    }
}
