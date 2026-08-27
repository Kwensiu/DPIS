package com.dpis.module.home;

import android.content.Context;

import com.dpis.module.R;
import com.dpis.module.updates.StartupUpdateManifest;

/**
 * Presentation-only result of the latest update check. The update dialog owns
 * downloading and installation so the home surface only reports check status.
 */
public final class HomeUpdateUiState {
    public enum Status {
        CHECKING,
        UP_TO_DATE,
        FAILED,
        AVAILABLE
    }

    public static final HomeUpdateUiState CHECKING = new HomeUpdateUiState(Status.CHECKING, null);
    public static final HomeUpdateUiState UP_TO_DATE = new HomeUpdateUiState(Status.UP_TO_DATE, null);
    public static final HomeUpdateUiState FAILED = new HomeUpdateUiState(Status.FAILED, null);

    public final Status status;
    public final String versionName;

    private HomeUpdateUiState(Status status, String versionName) {
        this.status = status != null ? status : Status.UP_TO_DATE;
        this.versionName = versionName != null ? versionName.trim() : "";
    }

    public static HomeUpdateUiState available(StartupUpdateManifest manifest) {
        return manifest == null
                ? UP_TO_DATE
                : new HomeUpdateUiState(Status.AVAILABLE, manifest.versionName);
    }

    public String subtitle(Context context) {
        if (context == null) {
            return "";
        }
        return switch (status) {
            case CHECKING -> context.getString(R.string.home_update_checking);
            case UP_TO_DATE -> context.getString(R.string.home_update_up_to_date);
            case FAILED -> context.getString(R.string.home_update_check_failed_retry);
            case AVAILABLE -> context.getString(
                    R.string.home_update_available,
                    versionName.isEmpty()
                            ? context.getString(R.string.home_update_version_unknown)
                            : versionName
            );
        };
    }
}
