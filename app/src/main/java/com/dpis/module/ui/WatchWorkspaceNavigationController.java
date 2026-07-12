package com.dpis.module.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.shape.AbsoluteCornerSize;
import com.google.android.material.shape.ShapeAppearanceModel;

/**
 * Replaces persistent navigation chrome with a compact workspace selector on small watches.
 *
 * <p>The controller intentionally treats the existing navigation menu as the source of truth. It
 * owns only the transient floating UI and delegates all state changes back to its host.</p>
 */
public final class WatchWorkspaceNavigationController {
    private static final int WORKSPACE_COUNT = 5;
    private static final int MAIN_BUTTON_SIZE_DP = 56;
    private static final int MENU_BUTTON_SIZE_DP = 48;
    private static final int MENU_ARC_RADIUS_DP = 104;
    private static final int MENU_ARC_START_ANGLE_DEGREES = 210;
    private static final int MENU_ARC_ANGLE_STEP_DEGREES = 30;
    private static final int MENU_CORNER_RADIUS_DP = MENU_BUTTON_SIZE_DP / 2;
    private static final int MAIN_BUTTON_MARGIN_DP = 12;
    private static final long MENU_ANIMATION_DURATION_MS = 180L;

    public interface Host {
        void onWorkspaceSelected(int itemId);
    }

    private final Context context;
    private final FrameLayout root;
    private final NavigationBarView navigationView;
    private final View navigationContainer;
    private final Host host;
    private final SparseArray<MaterialCardView> menuButtons = new SparseArray<>();
    private final int surfaceColor;
    private final int selectedSurfaceColor;
    private final int iconColor;
    private final int selectedIconColor;
    private final int mainButtonColor;
    private final int mainButtonIconColor;

    private FloatingActionButton mainButton;
    private View scrim;
    private FrameLayout menuPanel;
    private boolean expanded;
    private int selectedItemId;

    private WatchWorkspaceNavigationController(Context context,
            FrameLayout root,
            NavigationBarView navigationView,
            View navigationContainer,
            Host host) {
        this.context = context;
        this.root = root;
        this.navigationView = navigationView;
        this.navigationContainer = navigationContainer;
        this.host = host;
        surfaceColor = MaterialColors.getColor(
                root, com.google.android.material.R.attr.colorSurfaceContainerHigh);
        selectedSurfaceColor = MaterialColors.getColor(
                root, com.google.android.material.R.attr.colorSecondaryContainer);
        iconColor = MaterialColors.getColor(
                root, com.google.android.material.R.attr.colorOnSurface);
        selectedIconColor = MaterialColors.getColor(
                root, com.google.android.material.R.attr.colorOnSecondaryContainer);
        mainButtonColor = MaterialColors.getColor(
                root, com.google.android.material.R.attr.colorPrimaryContainer);
        mainButtonIconColor = MaterialColors.getColor(
                root, com.google.android.material.R.attr.colorOnPrimaryContainer);
    }

    /** Returns a controller only when the window needs watch-specific navigation chrome. */
    public static WatchWorkspaceNavigationController attachIfSupported(Context context,
            FrameLayout root,
            NavigationBarView navigationView,
            View navigationContainer,
            Host host) {
        if (context == null || root == null || navigationView == null || host == null
                || !shouldUseWatchNavigation(context)
                || navigationView.getMenu().size() != WORKSPACE_COUNT) {
            return null;
        }
        WatchWorkspaceNavigationController controller = new WatchWorkspaceNavigationController(
                context, root, navigationView,
                navigationContainer != null ? navigationContainer : navigationView, host);
        controller.attach();
        return controller;
    }

    public static boolean shouldUseWatchNavigation(Context context) {
        return WatchUiMode.shouldUseCompactUi(context);
    }

    public void setSelectedItem(int itemId) {
        selectedItemId = itemId;
        updateMainButton();
        for (int index = 0; index < menuButtons.size(); index++) {
            int menuItemId = menuButtons.keyAt(index);
            applyMenuButtonState(menuButtons.valueAt(index), menuItemId == itemId);
        }
    }

    public boolean closeMenuIfExpanded() {
        if (!expanded) {
            return false;
        }
        hideMenu(true);
        return true;
    }

    private void attach() {
        navigationContainer.setVisibility(View.GONE);
        createScrim();
        createMenuPanel();
        createMainButton();
        setSelectedItem(navigationView.getSelectedItemId());
    }

    private void createScrim() {
        scrim = new View(context);
        scrim.setBackgroundColor(Color.argb(76, 0, 0, 0));
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(view -> hideMenu(true));
        root.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    private void createMenuPanel() {
        menuPanel = new FrameLayout(context);
        menuPanel.setVisibility(View.GONE);
        menuPanel.setOnClickListener(view -> hideMenu(true));
        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        root.addView(menuPanel, panelParams);

        for (int index = 0; index < navigationView.getMenu().size(); index++) {
            MenuItem item = navigationView.getMenu().getItem(index);
            MaterialCardView card = createMenuButton(item);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    dp(MENU_BUTTON_SIZE_DP),
                    dp(MENU_BUTTON_SIZE_DP),
                    Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM
            );
            int angleDegrees = MENU_ARC_START_ANGLE_DEGREES
                    + index * MENU_ARC_ANGLE_STEP_DEGREES;
            double angleRadians = Math.toRadians(angleDegrees);
            int horizontalOffset = Math.round(
                    (float) Math.cos(angleRadians) * MENU_ARC_RADIUS_DP);
            int verticalOffset = Math.round(
                    (float) Math.sin(angleRadians) * MENU_ARC_RADIUS_DP);
            int mainCenterBottom = MAIN_BUTTON_MARGIN_DP + MAIN_BUTTON_SIZE_DP / 2;
            params.bottomMargin = dp(
                    mainCenterBottom - verticalOffset - MENU_BUTTON_SIZE_DP / 2);
            card.setTranslationX(dp(horizontalOffset));
            menuPanel.addView(card, params);
            applyNavigationBarInset(card, params.bottomMargin);
            menuButtons.put(item.getItemId(), card);
        }
    }

    private MaterialCardView createMenuButton(MenuItem item) {
        MaterialCardView card = new MaterialCardView(context);
        card.setRadius(dp(MENU_CORNER_RADIUS_DP));
        card.setCardElevation(dp(3));
        card.setStrokeWidth(0);
        ImageButton button = new ImageButton(context);
        button.setBackground(null);
        button.setScaleType(ImageButton.ScaleType.CENTER);
        button.setImageDrawable(item.getIcon());
        button.setContentDescription(item.getTitle());
        button.setOnClickListener(view -> {
            hideMenu(true);
            host.onWorkspaceSelected(item.getItemId());
        });
        TouchFeedbackBinder.bindPressHaptic(button);
        card.addView(button, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return card;
    }

    private void createMainButton() {
        mainButton = new FloatingActionButton(context);
        mainButton.setContentDescription("");
        mainButton.setShapeAppearanceModel(ShapeAppearanceModel.builder()
                .setAllCornerSizes(new AbsoluteCornerSize(dp(MAIN_BUTTON_SIZE_DP / 2)))
                .build());
        mainButton.setBackgroundTintList(ColorStateList.valueOf(mainButtonColor));
        mainButton.setImageTintList(ColorStateList.valueOf(mainButtonIconColor));
        mainButton.setOnClickListener(view -> {
            if (expanded) {
                hideMenu(true);
            } else {
                showMenu();
            }
        });
        TouchFeedbackBinder.bindPressHaptic(mainButton);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dp(MAIN_BUTTON_SIZE_DP), dp(MAIN_BUTTON_SIZE_DP),
                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM
        );
        params.bottomMargin = dp(MAIN_BUTTON_MARGIN_DP);
        root.addView(mainButton, params);
        applyNavigationBarInset(mainButton, params.bottomMargin);
    }

    private void applyNavigationBarInset(View target, int baseBottomMargin) {
        ViewCompat.setOnApplyWindowInsetsListener(target, (view, windowInsets) -> {
            Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.bottomMargin = baseBottomMargin + navigationBars.bottom;
            view.setLayoutParams(params);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(target);
    }

    private void updateMainButton() {
        if (mainButton == null) {
            return;
        }
        MenuItem selectedItem = navigationView.getMenu().findItem(selectedItemId);
        if (selectedItem == null) {
            return;
        }
        Drawable icon = selectedItem.getIcon();
        mainButton.setImageDrawable(icon);
        mainButton.setContentDescription(selectedItem.getTitle());
    }

    private void applyMenuButtonState(MaterialCardView card, boolean selected) {
        card.setCardBackgroundColor(selected ? selectedSurfaceColor : surfaceColor);
        View child = card.getChildAt(0);
        if (child instanceof ImageButton) {
            ((ImageButton) child).setImageTintList(ColorStateList.valueOf(
                    selected ? selectedIconColor : iconColor
            ));
        }
    }

    private void showMenu() {
        expanded = true;
        scrim.setVisibility(View.VISIBLE);
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(MENU_ANIMATION_DURATION_MS).start();
        menuPanel.setVisibility(View.VISIBLE);
        for (int index = 0; index < menuPanel.getChildCount(); index++) {
            View child = menuPanel.getChildAt(index);
            child.animate().cancel();
            child.setAlpha(0f);
            child.setScaleX(0.8f);
            child.setScaleY(0.8f);
            child.setTranslationY(dp(24));
            child.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(0f)
                    .setStartDelay(index * 28L)
                    .setDuration(MENU_ANIMATION_DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void hideMenu(boolean animate) {
        if (!expanded) {
            return;
        }
        expanded = false;
        if (!animate) {
            scrim.setVisibility(View.GONE);
            menuPanel.setVisibility(View.GONE);
            return;
        }
        scrim.animate()
                .alpha(0f)
                .setDuration(MENU_ANIMATION_DURATION_MS)
                .withEndAction(() -> scrim.setVisibility(View.GONE))
                .start();
        menuPanel.animate()
                .alpha(0f)
                .scaleX(0.9f)
                .scaleY(0.9f)
                .translationY(dp(16))
                .setDuration(MENU_ANIMATION_DURATION_MS)
                .withEndAction(() -> {
                    menuPanel.setVisibility(View.GONE);
                    menuPanel.setAlpha(1f);
                    menuPanel.setScaleX(1f);
                    menuPanel.setScaleY(1f);
                    menuPanel.setTranslationY(0f);
                })
                .start();
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
