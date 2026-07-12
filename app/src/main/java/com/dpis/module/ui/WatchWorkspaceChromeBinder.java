package com.dpis.module.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.IdRes;

import com.dpis.module.R;

/**
 * Applies the small amount of shared workspace chrome that is unique to compact watches.
 *
 * <p>Page content continues to own its own layout. This binder only centers the two
 * standalone workspace headings, which otherwise look visually offset inside a round window.</p>
 */
public final class WatchWorkspaceChromeBinder {
    private WatchWorkspaceChromeBinder() {
    }

    public static void applyIfSupported(Context context, View homeWorkspace, View settingsWorkspace) {
        if (!WatchUiMode.shouldUseCompactUi(context)) {
            return;
        }
        centerTitle(homeWorkspace, R.id.home_workspace_title);
        centerText(homeWorkspace, R.id.home_workspace_subtitle);
        centerTitle(settingsWorkspace, R.id.settings_workspace_title);
    }

    /** Keeps the shared app/template search toolbar inside a compact round display's safe area. */
    public static void applyTopContainerInsets(View topContainer) {
        boolean compactWatch = topContainer != null
                && WatchUiMode.shouldUseCompactUi(topContainer.getContext());
        WindowInsetsBinder.applySafeDrawingPadding(
                topContainer,
                compactWatch,
                true,
                compactWatch,
                false
        );
    }

    private static void centerTitle(View workspace, @IdRes int titleId) {
        if (workspace == null) {
            return;
        }
        TextView title = workspace.findViewById(titleId);
        if (title == null) {
            return;
        }
        title.setGravity(Gravity.CENTER);
        ViewGroup.LayoutParams params = title.getLayoutParams();
        if (params != null && params.width != ViewGroup.LayoutParams.MATCH_PARENT) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            title.setLayoutParams(params);
        }
    }

    private static void centerText(View workspace, @IdRes int textId) {
        if (workspace == null) {
            return;
        }
        TextView text = workspace.findViewById(textId);
        if (text != null) {
            text.setGravity(Gravity.CENTER);
        }
    }
}
