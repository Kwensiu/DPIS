package com.dpis.module;

import android.content.Context;

import java.io.File;

final class HomeUpdateUiState {
    enum Status {
        CHECKING,
        UP_TO_DATE,
        FAILED,
        AVAILABLE,
        DOWNLOADING,
        INSTALL_READY
    }

    static final HomeUpdateUiState CHECKING =
            new HomeUpdateUiState(Status.CHECKING, null, 0, null, null, null, 0, null);
    static final HomeUpdateUiState UP_TO_DATE =
            new HomeUpdateUiState(Status.UP_TO_DATE, null, 0, null, null, null, 0, null);
    static final HomeUpdateUiState FAILED =
            new HomeUpdateUiState(Status.FAILED, null, 0, null, null, null, 0, null);

    final Status status;
    final String versionName;
    final int versionCode;
    final String apkUrl;
    final String releasePage;
    final String releaseNotes;
    final int downloadProgress;
    final String downloadedApkPath;

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

    static HomeUpdateUiState available(StartupUpdateManifest manifest) {
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

    HomeUpdateUiState asDownloading(int progress) {
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

    HomeUpdateUiState asAvailable() {
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

    HomeUpdateUiState asInstallReady(File apkFile) {
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

    boolean showsUpdateActionCard() {
        return status == Status.AVAILABLE
                || status == Status.DOWNLOADING
                || status == Status.INSTALL_READY;
    }

    String subtitle(Context context) {
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
