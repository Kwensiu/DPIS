package com.dpis.module.updates;

public final class StartupUpdateManifest {
    public final String versionName;
    public final int versionCode;
    public final String apkUrl;
    public final String releasePage;
    public final String releaseNotes;

    public StartupUpdateManifest(String versionName,
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
