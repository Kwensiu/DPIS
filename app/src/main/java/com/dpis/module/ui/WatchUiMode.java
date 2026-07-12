package com.dpis.module.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;

/** Shared form-factor policy for UI that needs a compact watch presentation. */
public final class WatchUiMode {
    public static final int ROUND_SMALL_SCREEN_MAX_DP = 280;

    /** Single form-factor classification used by compact workspace chrome and layout policies. */
    public enum Profile {
        STANDARD,
        COMPACT_WATCH,
        COMPACT_ROUND
    }

    private WatchUiMode() {
    }

    public static boolean shouldUseCompactUi(Context context) {
        return resolve(context) != Profile.STANDARD;
    }

    public static Profile resolve(Context context) {
        if (context == null) {
            return Profile.STANDARD;
        }
        Configuration configuration = context.getResources().getConfiguration();
        int shortSide = Math.min(configuration.screenWidthDp, configuration.screenHeightDp);
        boolean round = configuration.isScreenRound();
        boolean compact = shouldUseCompactUi(
                context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH), round, shortSide);
        if (!compact) {
            return Profile.STANDARD;
        }
        return round ? Profile.COMPACT_ROUND : Profile.COMPACT_WATCH;
    }

    public static boolean shouldApplyRoundSafePadding(Context context) {
        if (context == null) {
            return false;
        }
        return resolve(context) == Profile.COMPACT_ROUND;
    }

    /** The persistent search field is already visible on compact screens, so a duplicate FAB adds no value. */
    public static boolean shouldUseFloatingAppSearch(Context context) {
        return shouldUseFloatingAppSearch(shouldUseCompactUi(context));
    }

    static boolean shouldUseFloatingAppSearch(boolean compactUi) {
        return !compactUi;
    }

    static boolean shouldUseCompactUi(boolean isWatch, boolean isRound, int shortSideDp) {
        return isWatch || (isRound && shortSideDp > 0 && shortSideDp <= ROUND_SMALL_SCREEN_MAX_DP);
    }
}
