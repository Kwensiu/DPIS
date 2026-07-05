package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class DonateActivitySourceSmokeTest {
    @Test
    public void donateEntrypointsShareOneLocalQrPage() throws IOException {
        String source = read("src/main/java/com/dpis/module/DonateActivity.java");
        String layout = read("src/main/res/layout/activity_donate.xml");
        String supportersSheet = read("src/main/res/layout/sheet_donate_supporters.xml");
        String strings = read("src/main/res/values/strings.xml");
        String dimens = read("src/main/res/values/dimens.xml");
        String manifest = read("src/main/AndroidManifest.xml");
        String homeLayout = read("src/main/res/layout/home_workspace.xml");
        String homeEntryLayout = read("src/main/res/layout/view_home_donate.xml");
        String homeBinder = read("src/main/java/com/dpis/module/home/HomeWorkspaceBinder.java");
        String settingsController = read(
                "src/main/java/com/dpis/module/SystemServerSettingsPageController.java");

        assertTrue(source.contains("static Intent createIntent(Context context)"));
        assertTrue(source.contains("return new Intent(context, DonateActivity.class);"));
        assertTrue(source.contains("setContentView(R.layout.activity_donate);"));
        assertTrue(source.contains("R.id.donate_back_button"));
        assertTrue(source.contains("R.id.donate_toolbar"));
        assertTrue(source.contains("WindowInsetsBinder.applySafeDrawingPadding(toolbar, false, true, false, false);"));
        assertTrue(source.contains("WindowInsetsBinder.applySafeDrawingPadding(content, false, false, false, true);"));
        assertTrue(source.contains("R.id.donate_supporters_card"));
        assertTrue(source.contains("showSupportersSheet()"));
        assertTrue(source.contains("new BottomSheetDialog(this)"));
        assertTrue(source.contains("R.layout.sheet_donate_supporters"));
        assertTrue(source.contains("setSkipCollapsed(true)"));
        assertTrue(source.contains("BottomSheetBehavior.STATE_EXPANDED"));
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
        assertTrue(layout.contains("android:id=\"@+id/donate_toolbar\""));
        assertTrue(layout.contains("android:id=\"@+id/donate_scroll\""));
        assertTrue(layout.contains("android:layout_height=\"0dp\""));
        assertTrue(layout.contains("android:layout_weight=\"1\""));
        assertTrue(layout.contains("@dimen/page_toolbar_padding_horizontal"));
        assertTrue(layout.contains("android:id=\"@+id/donate_wechat_qr\""));
        assertTrue(layout.contains("android:id=\"@+id/donate_alipay_qr\""));
        assertTrue(layout.contains("android:id=\"@+id/donate_supporters_card\""));
        assertTrue(layout.contains("@string/donate_supporters_title"));
        assertTrue(layout.contains("@string/donate_supporters_summary"));
        assertTrue(layout.contains("android:src=\"@drawable/donate_wechat\""));
        assertTrue(layout.contains("android:src=\"@drawable/donate_alipay\""));
        assertTrue(layout.contains("android:adjustViewBounds=\"true\""));
        assertTrue(layout.contains("android:layout_width=\"match_parent\""));
        assertTrue(layout.contains("android:layout_gravity=\"top|start\""));
        assertTrue(layout.contains("@drawable/bg_donate_qr_title_pill"));
        assertTrue(layout.contains("@dimen/donate_qr_title_pill_margin"));
        assertTrue(layout.contains("@string/donate_trust_note"));
        assertTrue(supportersSheet.contains("@string/donate_supporters_title"));
        assertTrue(supportersSheet.contains("@string/donate_supporters_summary"));
        assertTrue(supportersSheet.contains("@dimen/donate_supporters_sheet_min_height"));
        assertEquals(5, countOccurrences(supportersSheet,
                "<com.google.android.material.card.MaterialCardView"));
        assertTrue(supportersSheet.contains("@dimen/donate_supporters_sheet_card_spacing_top"));
        assertTrue(supportersSheet.contains("@string/donate_supporter_nickyoung_name"));
        assertTrue(supportersSheet.contains("@string/donate_supporter_nickyoung_amount"));
        assertTrue(supportersSheet.contains("@string/donate_supporter_tadow_name"));
        assertTrue(supportersSheet.contains("@string/donate_supporter_tadow_amount"));
        assertTrue(supportersSheet.contains("@string/donate_supporter_han_name"));
        assertTrue(supportersSheet.contains("@string/donate_supporter_han_amount"));
        assertTrue(supportersSheet.contains("@string/donate_supporter_spine_name"));
        assertTrue(supportersSheet.contains("@string/donate_supporter_spine_amount"));
        assertTrue(supportersSheet.contains("@string/donate_supporter_anonymous_name"));
        assertTrue(supportersSheet.contains("@string/donate_supporter_anonymous_amount"));
        assertTrue(supportersSheet.contains("@string/donate_supporters_sheet_note"));
        assertTrue(strings.contains("name=\"donate_supporter_nickyoung_name\" translatable=\"false\""));
        assertTrue(strings.contains("\\@xyuYoung"));
        assertTrue(strings.contains("name=\"donate_supporter_nickyoung_amount\" translatable=\"false\""));
        assertTrue(strings.contains("10.00\uFFE5"));
        assertTrue(strings.contains("name=\"donate_supporter_tadow_name\" translatable=\"false\""));
        assertTrue(strings.contains("\\@Tadow_"));
        assertTrue(strings.contains("name=\"donate_supporter_tadow_amount\" translatable=\"false\""));
        assertTrue(strings.contains("6.66\uFFE5"));
        assertTrue(strings.contains("name=\"donate_supporter_han_name\" translatable=\"false\""));
        assertTrue(strings.contains("\\@**瀚"));
        assertTrue(strings.contains("name=\"donate_supporter_han_amount\" translatable=\"false\""));
        assertTrue(strings.contains("20.00\uFFE5"));
        assertTrue(strings.contains("name=\"donate_supporter_spine_name\" translatable=\"false\""));
        assertTrue(strings.contains("\\@脊椎健康者"));
        assertTrue(strings.contains("name=\"donate_supporter_spine_amount\" translatable=\"false\""));
        assertTrue(strings.contains("7.77\uFFE5"));
        assertTrue(strings.contains("name=\"donate_supporter_anonymous_name\" translatable=\"false\""));
        assertTrue(strings.contains("\\@Anonymous(user)"));
        assertTrue(strings.contains("name=\"donate_supporter_anonymous_amount\" translatable=\"false\""));
        assertTrue(strings.contains("5.00\uFFE5"));
        assertTrue(dimens.contains("name=\"donate_supporters_sheet_min_height\">512dp"));
        assertTrue(dimens.contains("name=\"donate_supporters_sheet_card_spacing_top\">8dp"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
