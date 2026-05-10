package com.dpis.module;

final class StartupUpdateManifest {
    final String versionName;
    final int versionCode;
    final String apkUrl;
    final String releasePage;
    final String releaseNotes;

    StartupUpdateManifest(String versionName,
            int versionCode,
            String apkUrl,
            String releasePage,
            String releaseNotes) {
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.apkUrl = apkUrl;
        this.releasePage = releasePage;
        this.releaseNotes = releaseNotes;
    }
}
