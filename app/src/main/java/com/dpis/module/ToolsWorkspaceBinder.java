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
        View toolsToolbar = workspaceView.findViewById(R.id.tools_toolbar);
        WindowInsetsBinder.applySafeDrawingPadding(toolsToolbar, false, true, false, false);
        bindLogEntry(workspaceView);
        fontScaleToolBinder = new SystemFontScaleToolBinder(activity, workspaceView);
        fontScaleToolBinder.bind();
    }

    private void bindLogEntry(View workspaceView) {
        View logCard = workspaceView.findViewById(R.id.tools_log_card);
        if (logCard != null) {
            logCard.setOnClickListener(view ->
                    activity.startActivity(new Intent(activity, LogActivity.class))
            );
        }
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

    void onShown() {
        if (fontScaleToolBinder != null) {
            fontScaleToolBinder.collapseAndRefreshFromSystem();
        }
    }
}
