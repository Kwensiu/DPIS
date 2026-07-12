package com.dpis.module.applist;

import android.content.Context;
import android.view.View;

import com.dpis.module.ui.WatchUiMode;
import com.google.android.material.tabs.TabLayout;

/**
 * Owns app-list tab positioning without coupling workspace navigation to list layout details.
 *
 * <p>The portrait pager places tabs over the list, while the landscape list keeps them as a
 * normal sibling. This controller keeps that difference explicit so a portrait-only inset cannot
 * accidentally create a second blank row in landscape.</p>
 */
public final class AppListTabsChromeController {
    private final Context context;
    private final TabLayout tabs;
    private final AppListPagerAdapter pagerAdapter;
    private final AppListPagerAdapter.AppListPageController landscapeListController;
    private int scrollOffset;

    public AppListTabsChromeController(Context context,
            TabLayout tabs,
            AppListPagerAdapter pagerAdapter,
            AppListPagerAdapter.AppListPageController landscapeListController) {
        this.context = context;
        this.tabs = tabs;
        this.pagerAdapter = pagerAdapter;
        this.landscapeListController = landscapeListController;
    }

    /** Starts synchronizing list padding after the tab strip has its measured height. */
    public void bind() {
        if (tabs == null) {
            return;
        }
        tabs.addOnLayoutChangeListener((view, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> syncListInsets());
    }

    /**
     * Updates the portrait tab overlay. Returns true only when it consumed the app-list scroll.
     */
    public boolean onPageListScrolled(int dy) {
        if (!usesPortraitOverlay() || tabs == null || tabs.getVisibility() != View.VISIBLE) {
            return false;
        }
        int height = tabs.getHeight();
        if (height <= 0) {
            return false;
        }
        scrollOffset = Math.max(0, Math.min(height, scrollOffset + dy));
        tabs.setTranslationY(-scrollOffset);
        return true;
    }

    /** Resets transient scroll state when leaving the app workspace and refreshes list padding. */
    public void onWorkspaceChanged(boolean appWorkspace) {
        if (!appWorkspace) {
            scrollOffset = 0;
        }
        syncListInsets();
    }

    /** Re-applies the current policy after visibility or orientation changes. */
    public void syncListInsets() {
        if (tabs == null) {
            return;
        }
        int tabHeight = tabs.getHeight();
        if (pagerAdapter != null) {
            pagerAdapter.setTopContentInset(tabHeight);
        }
        if (landscapeListController != null) {
            landscapeListController.setTopContentInset(0);
        }
        if (!usesPortraitOverlay()) {
            scrollOffset = 0;
        }
        scrollOffset = Math.min(scrollOffset, tabHeight);
        tabs.setTranslationY(-scrollOffset);
    }

    private boolean usesPortraitOverlay() {
        return WatchUiMode.shouldUseCompactUi(context) && pagerAdapter != null;
    }
}
