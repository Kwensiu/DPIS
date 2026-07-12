package com.dpis.module.settings;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import com.dpis.module.R;
import com.dpis.module.ui.WatchUiMode;

public final class ToolsWorkspaceBinder {
    public interface Host {
        android.app.Activity activity();

        void applyToolsToolbarInsets(View toolbar);

        void bindPressHaptic(View view);

        void openLogsWhenDiagnosticLogsEnabled();
    }

    private final Host host;
    private SystemFontScaleToolBinder fontScaleToolBinder;

    public ToolsWorkspaceBinder(Host host) {
        this.host = host;
    }

    public void bind(View workspaceView) {
        if (workspaceView == null || fontScaleToolBinder != null) {
            return;
        }
        View toolsToolbar = workspaceView.findViewById(R.id.tools_toolbar);
        host.applyToolsToolbarInsets(toolsToolbar);
        if (WatchUiMode.shouldUseCompactUi(host.activity())
                && toolsToolbar instanceof LinearLayout) {
            ((LinearLayout) toolsToolbar).setGravity(Gravity.CENTER);
        }
        bindLogEntry(workspaceView);
        fontScaleToolBinder = new SystemFontScaleToolBinder(host.activity(), workspaceView, host);
        fontScaleToolBinder.bind();
    }

    private void bindLogEntry(View workspaceView) {
        View logCard = workspaceView.findViewById(R.id.tools_log_card);
        if (logCard != null) {
            logCard.setOnClickListener(view -> host.openLogsWhenDiagnosticLogsEnabled());
        }
    }

    public void onStart() {
        if (fontScaleToolBinder != null) {
            fontScaleToolBinder.refreshFromSystem();
        }
    }

    public void onResume() {
        if (fontScaleToolBinder != null) {
            fontScaleToolBinder.refreshFromSystem();
        }
    }

    public void onStop() {
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (fontScaleToolBinder != null) {
            fontScaleToolBinder.refreshFromSystem();
        }
    }

    public void onShown() {
        if (fontScaleToolBinder != null) {
            fontScaleToolBinder.collapseAndRefreshFromSystem();
        }
    }
}
