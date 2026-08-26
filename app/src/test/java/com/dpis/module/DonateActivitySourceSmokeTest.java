package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class DonateActivitySourceSmokeTest {
    @Test
    public void donateEntrypointsPreserveActivityContractAndComposePresentation() throws IOException {
        String source = read("src/main/java/com/dpis/module/home/DonateActivity.java");
        String compose = read("src/main/java/com/dpis/module/ui/compose/SupportPages.kt");
        String cards = read("src/main/java/com/dpis/module/ui/compose/SupportCards.kt");
        String activityContent = read("src/main/java/com/dpis/module/ui/compose/SupportActivityContent.kt");
        String manifest = read("src/main/AndroidManifest.xml");
        String homeBinder = read("src/main/java/com/dpis/module/home/HomeWorkspaceBinder.java");
        String settingsController = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");
        String mainActivity = read("src/main/java/com/dpis/module/MainActivity.java");

        assertTrue(source.contains("static Intent createIntent(Context context)"));
        assertTrue(source.contains("return new Intent(context, DonateActivity.class);"));
        assertTrue(source.contains("SupportActivityContent.installDonate(this);"));
        assertTrue(activityContent.contains("fun installDonate(activity: ComponentActivity)"));
        assertTrue(compose.contains("fun DonateSupportPage(onBack: () -> Unit)"));
        assertTrue(compose.contains("DonateSupportPage"));
        assertTrue(cards.contains("ModalBottomSheet(") || compose.contains("ModalBottomSheet("));
        assertTrue(cards.contains("rememberBottomSheetState(") || compose.contains("rememberBottomSheetState("));
        assertTrue(cards.contains("enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)") || compose.contains("enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)"));
        assertTrue(cards.contains(".heightIn(min = 512.dp)") || compose.contains(".heightIn(min = 512.dp)"));
        assertTrue(cards.contains("supporters.forEachIndexed") || compose.contains("supporters.forEachIndexed"));
        assertTrue(cards.contains("R.drawable.donate_wechat") || compose.contains("R.drawable.donate_wechat"));
        assertTrue(cards.contains("R.drawable.donate_alipay") || compose.contains("R.drawable.donate_alipay"));
        assertTrue(cards.contains("R.string.donate_wechat_qr_description") || compose.contains("R.string.donate_wechat_qr_description"));
        assertTrue(cards.contains("R.string.donate_alipay_qr_description") || compose.contains("R.string.donate_alipay_qr_description"));
        assertTrue(cards.contains("R.string.donate_supporter_nickyoung_name") || compose.contains("R.string.donate_supporter_nickyoung_name"));
        assertTrue(cards.contains("R.string.donate_supporter_anonymous_name") || compose.contains("R.string.donate_supporter_anonymous_name"));
        assertTrue(manifest.contains("android:name=\".home.DonateActivity\""));
        assertTrue(homeBinder.contains("DonateActivity.createIntent(context)"));
        assertTrue(settingsController.contains("DonateActivity.createIntent(activity)"));
        assertTrue(mainActivity.contains("DonateActivity.createIntent(MainActivity.this)"));
        assertFalse(mainActivity.contains("MainStandaloneRoute"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
