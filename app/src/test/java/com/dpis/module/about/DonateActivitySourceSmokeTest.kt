package com.dpis.module.about

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DonateActivitySourceSmokeTest {
    @Test
    @Throws(IOException::class)
    fun donateEntrypointsPreserveActivityContractAndComposePresentation() {
        val source = read("src/main/java/com/dpis/module/home/DonateActivity.java")
        val compose =
            read("src/main/java/com/dpis/module/about/presentation/SupportPages.kt")
        val cards = read("src/main/java/com/dpis/module/about/presentation/SupportCards.kt")
        val activityContent =
            read("src/main/java/com/dpis/module/about/presentation/SupportActivityContent.kt")
        val manifest = read("src/main/AndroidManifest.xml")
        val homeState = read("src/main/java/com/dpis/module/home/HomeWorkspaceState.kt")
        val settingsController =
            read("src/main/java/com/dpis/module/SystemServerSettingsPageController.kt")
        val mainActivity = read("src/main/java/com/dpis/module/MainActivity.java")

        assertTrue(source.contains("static Intent createIntent(Context context)"))
        assertTrue(source.contains("return new Intent(context, DonateActivity.class);"))
        assertTrue(source.contains("SupportActivityContent.installDonate(this);"))
        assertTrue(activityContent.contains("fun installDonate(activity: ComponentActivity)"))
        assertTrue(compose.contains("fun DonateSupportPage(onBack: () -> Unit)"))
        assertTrue(compose.contains("DonateSupportPage"))
        assertTrue(cards.contains("ModalBottomSheet(") || compose.contains("ModalBottomSheet("))
        assertTrue(cards.contains("rememberBottomSheetState(") || compose.contains("rememberBottomSheetState("))
        assertTrue(
            cards.contains("enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)") || compose.contains(
                "enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)"
            )
        )
        assertTrue(cards.contains(".heightIn(min = 512.dp)") || compose.contains(".heightIn(min = 512.dp)"))
        assertTrue(cards.contains("supporters.forEachIndexed") || compose.contains("supporters.forEachIndexed"))
        assertTrue(cards.contains("R.drawable.donate_wechat") || compose.contains("R.drawable.donate_wechat"))
        assertTrue(cards.contains("R.drawable.donate_alipay") || compose.contains("R.drawable.donate_alipay"))
        assertTrue(
            cards.contains("R.string.donate_wechat_qr_description") || compose.contains(
                "R.string.donate_wechat_qr_description"
            )
        )
        assertTrue(
            cards.contains("R.string.donate_alipay_qr_description") || compose.contains(
                "R.string.donate_alipay_qr_description"
            )
        )
        assertTrue(
            cards.contains("R.string.donate_supporter_nickyoung_name") || compose.contains(
                "R.string.donate_supporter_nickyoung_name"
            )
        )
        assertTrue(
            cards.contains("R.string.donate_supporter_anonymous_name") || compose.contains(
                "R.string.donate_supporter_anonymous_name"
            )
        )
        assertTrue(manifest.contains("android:name=\".home.DonateActivity\""))
        assertTrue(homeState.contains("fun openDonate()"))
        assertTrue(settingsController.contains("DonateActivity.createIntent(activity)"))
        assertTrue(mainActivity.contains("DonateActivity.createIntent(MainActivity.this)"))
        assertFalse(mainActivity.contains("MainStandaloneRoute"))
    }

    companion object {
        @Throws(IOException::class)
        private fun read(relativePath: String): String {
            return SourceSmokeTestPaths.read(relativePath)
        }
    }
}
