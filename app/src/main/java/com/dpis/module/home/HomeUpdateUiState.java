package com.dpis.module.home;

import android.content.Context;

import com.dpis.module.R;
import com.dpis.module.updates.StartupUpdateManifest;

import java.io.File;

public final class HomeUpdateUiState {
    public enum Status {
        CHECKING,
        UP_TO_DATE,
        FAILED,
        AVAILABLE,
        DOWNLOADING,
        INSTALL_READY
    }

    public static final HomeUpdateUiState CHECKING =
            new HomeUpdateUiState(Status.CHECKING, null, 0, null, null, null, 0, null);
    public static final HomeUpdateUiState UP_TO_DATE =
            new HomeUpdateUiState(Status.UP_TO_DATE, null, 0, null, null, null, 0, null);
    public static final HomeUpdateUiState FAILED =
            new HomeUpdateUiState(Status.FAILED, null, 0, null, null, null, 0, null);

    public final Status status;
    public final String versionName;
    public final int versionCode;
    public final String apkUrl;
    public final String releasePage;
    public final String releaseNotes;
    public final int downloadProgress;
    public final String downloadedApkPath;

    private HomeUpdateUiState(Status status,
            String versionName,
            int versionCode,
            String apkUrl,
            String releasePage,
            String releaseNotes,
            int downloadProgress,
            String downloadedApkPath) {
        this.status = status != null ? status : Status.UP_TO_DATE;
        this.versionName = normalize(versionName);
        this.versionCode = Math.max(0, versionCode);
        this.apkUrl = normalize(apkUrl);
        this.releasePage = normalize(releasePage);
        this.releaseNotes = normalize(releaseNotes);
        this.downloadProgress = Math.max(0, Math.min(100, downloadProgress));
        this.downloadedApkPath = normalize(downloadedApkPath);
    }

    public static HomeUpdateUiState available(StartupUpdateManifest manifest) {
        if (manifest == null) {
            return UP_TO_DATE;
        }
        return new HomeUpdateUiState(
                Status.AVAILABLE,
                manifest.versionName,
                manifest.versionCode,
                manifest.apkUrl,
                manifest.releasePage,
                manifest.releaseNotes,
                0,
                null);
    }

    public HomeUpdateUiState asDownloading(int progress) {
        return new HomeUpdateUiState(
                Status.DOWNLOADING,
                versionName,
                versionCode,
                apkUrl,
                releasePage,
                releaseNotes,
                progress,
                null);
    }

    public HomeUpdateUiState asAvailable() {
        return new HomeUpdateUiState(
                Status.AVAILABLE,
                versionName,
                versionCode,
                apkUrl,
                releasePage,
                releaseNotes,
                0,
                null);
    }

    public HomeUpdateUiState asInstallReady(File apkFile) {
        return new HomeUpdateUiState(
                Status.INSTALL_READY,
                versionName,
                versionCode,
                apkUrl,
                releasePage,
                releaseNotes,
                100,
                apkFile != null ? apkFile.getAbsolutePath() : null);
    }

    public boolean showsUpdateActionCard() {
        return status == Status.AVAILABLE
                || status == Status.DOWNLOADING
                || status == Status.INSTALL_READY;
    }

    public String subtitle(Context context) {
        if (context == null) {
            return "";
        }
        return switch (status) {
            case CHECKING -> context.getString(R.string.home_update_checking);
            case UP_TO_DATE -> context.getString(R.string.home_update_up_to_date);
            case FAILED -> context.getString(R.string.home_update_check_failed_retry);
            case AVAILABLE, DOWNLOADING, INSTALL_READY -> context.getString(
                    R.string.home_update_available,
                    versionName.isEmpty()
                            ? context.getString(R.string.home_update_version_unknown)
                            : versionName);
        };
    }

    private static String normalize(String value) {
        return value != null ? value.trim() : "";
    }
}
