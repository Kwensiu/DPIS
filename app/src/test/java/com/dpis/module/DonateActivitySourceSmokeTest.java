package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class DonateActivitySourceSmokeTest {
    @Test
    public void donateEntrypointsShareOneLocalQrPage() throws IOException {
        String source = read("src/main/java/com/dpis/module/DonateActivity.java");
        String layout = read("src/main/res/layout/activity_donate.xml");
        String manifest = read("src/main/AndroidManifest.xml");
        String homeLayout = read("src/main/res/layout/home_workspace.xml");
        String homeEntryLayout = read("src/main/res/layout/view_home_donate.xml");
        String homeBinder = read("src/main/java/com/dpis/module/HomeWorkspaceBinder.java");
        String settingsController = read(
                "src/main/java/com/dpis/module/SystemServerSettingsPageController.java");

        assertTrue(source.contains("static Intent createIntent(Context context)"));
        assertTrue(source.contains("return new Intent(context, DonateActivity.class);"));
        assertTrue(source.contains("setContentView(R.layout.activity_donate);"));
        assertTrue(source.contains("R.id.donate_back_button"));
        assertTrue(manifest.contains("android:name=\".DonateActivity\""));
        assertTrue(manifest.contains("android:exported=\"false\""));
        assertTrue(homeLayout.contains("@layout/view_home_donate"));
        assertTrue(homeLayout.indexOf("@layout/view_home_feedback")
                < homeLayout.indexOf("@layout/view_home_donate"));
        assertTrue(homeEntryLayout.contains("android:id=\"@+id/home_donate_entry\""));
        assertTrue(homeEntryLayout.contains("@string/home_donate_title"));
        assertTrue(!homeEntryLayout.contains("@drawable/ic_volunteer_24"));
        assertTrue(homeBinder.contains("R.id.home_donate_entry"));
        assertTrue(homeBinder.contains("DonateActivity.createIntent(context)"));
        assertTrue(settingsController.contains("R.id.row_donate"));
        assertTrue(settingsController.contains("DonateActivity.createIntent(activity)"));
        assertTrue(layout.contains("android:id=\"@+id/donate_wechat_qr\""));
        assertTrue(layout.contains("android:id=\"@+id/donate_alipay_qr\""));
        assertTrue(layout.contains("android:src=\"@drawable/donate_wechat\""));
        assertTrue(layout.contains("android:src=\"@drawable/donate_alipay\""));
        assertTrue(layout.contains("android:adjustViewBounds=\"true\""));
        assertTrue(layout.contains("android:maxWidth=\"@dimen/donate_qr_max_width\""));
        assertTrue(layout.contains("android:maxHeight=\"@dimen/donate_qr_max_height\""));
        assertTrue(layout.contains("@string/donate_trust_note"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
