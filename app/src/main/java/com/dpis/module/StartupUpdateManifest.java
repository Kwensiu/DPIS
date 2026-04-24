package com.dpis.module;

final class StartupUpdateManifest {
    final String versionName;
    final int versionCode;
    final String apkUrl;
    final String releasePage;

    StartupUpdateManifest(String versionName,
            int versionCode,
            String apkUrl,
            String releasePage) {
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.apkUrl = apkUrl;
        this.releasePage = releasePage;
    }
}
