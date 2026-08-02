package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class DonateActivitySourceSmokeTest {
    @Test
    public void donateEntrypointsPreserveActivityContractAndComposePresentation() throws IOException {
        String source = read("src/main/java/com/dpis/module/home/DonateActivity.java");
        String compose = read("src/main/java/com/dpis/module/ui/compose/SupportPages.kt");
        String activityContent = read("src/main/java/com/dpis/module/ui/compose/SupportActivityContent.kt");
        String manifest = read("src/main/AndroidManifest.xml");
        String homeBinder = read("src/main/java/com/dpis/module/home/HomeWorkspaceBinder.java");
        String settingsController = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");

        assertTrue(source.contains("static Intent createIntent(Context context)"));
        assertTrue(source.contains("return new Intent(context, DonateActivity.class);"));
        assertTrue(source.contains("SupportActivityContent.installDonate(this);"));
        assertTrue(activityContent.contains("fun installDonate(activity: ComponentActivity)"));
        assertTrue(compose.contains("fun DonateSupportPage(onBack: () -> Unit)"));
        assertTrue(compose.contains("ModalBottomSheet("));
        assertTrue(compose.contains("rememberBottomSheetState("));
        assertTrue(compose.contains("enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)"));
        assertTrue(compose.contains(".heightIn(min = 512.dp)"));
        assertTrue(compose.contains("supporters.forEachIndexed"));
        assertTrue(compose.contains("R.drawable.donate_wechat"));
        assertTrue(compose.contains("R.drawable.donate_alipay"));
        assertTrue(compose.contains("R.string.donate_wechat_qr_description"));
        assertTrue(compose.contains("R.string.donate_alipay_qr_description"));
        assertTrue(compose.contains("R.string.donate_supporter_nickyoung_name"));
        assertTrue(compose.contains("R.string.donate_supporter_anonymous_name"));
        assertTrue(manifest.contains("android:name=\".home.DonateActivity\""));
        assertTrue(homeBinder.contains("DonateActivity.createIntent(context)"));
        assertTrue(settingsController.contains("DonateActivity.createIntent(activity)"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
