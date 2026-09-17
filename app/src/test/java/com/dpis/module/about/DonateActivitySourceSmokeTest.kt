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
        val source = read("src/main/java/com/dpis/module/home/DonateActivity.kt")
        val compose =
            read("src/main/java/com/dpis/module/home/presentation/DonateContent.kt")
        val cards = compose
        val activityContent = compose
        val manifest = read("src/main/AndroidManifest.xml")
        val homeState = read("src/main/java/com/dpis/module/home/HomeWorkspaceState.kt")
        val settingsController =
            read("src/main/java/com/dpis/module/settings/presentation/SettingsWorkspaceSession.kt")
        val mainActivity = read("src/main/java/com/dpis/module/MainActivity.kt")
        val homeSession = read(
            "src/main/java/com/dpis/module/home/presentation/HomeWorkspaceSession.kt"
        )

        assertTrue(source.contains("fun createIntent(context: Context)"))
        assertTrue(source.contains("Intent(context, DonateActivity::class.java)"))
        assertTrue(source.contains("installDonate()"))
        assertTrue(activityContent.contains("fun ComponentActivity.installDonate()"))
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
        assertTrue(compose.contains("SheetDestinationAnimatedContent("))
        assertTrue(compose.contains("clipContentToAnimatedBounds = false"))
        assertTrue(compose.contains("animateSize = true"))
        assertTrue(compose.contains("BackHandler(enabled = selected != null)"))
        assertTrue(cards.contains("SupportersSheet(wall)") || compose.contains("SupportersSheet(wall)"))
        assertTrue(compose.contains("DonationCatalogLoader.loadWall("))
        assertTrue(compose.contains("SupporterDetailsContent("))
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
        assertTrue(compose.contains("R.string.donate_supporters_anonymous"))
        val catalog = read("src/main/java/com/dpis/module/home/DonationCatalog.kt")
        val wall = read("src/main/java/com/dpis/module/home/DonationSupportWall.kt")
        val ledger = read("src/main/res/raw/donations.json")
        val loader =
            read("src/main/java/com/dpis/module/home/presentation/DonationCatalogLoader.kt")
        assertTrue(catalog.contains("fun parseRecords(rawJson: String)"))
        assertTrue(wall.contains("fun from(records: List<DonationRecord>)"))
        assertTrue(loader.contains("R.raw.donations"))
        assertTrue(ledger.contains("\"donations\""))
        assertFalse(compose.contains("R.string.donate_supporter_nickyoung_name"))
        assertFalse(compose.contains("R.string.donate_supporter_anonymous_name"))
        assertTrue(manifest.contains("android:name=\".home.DonateActivity\""))
        assertTrue(homeState.contains("fun openDonate()"))
        assertTrue(settingsController.contains("SecondaryDestination.Donate"))
        assertTrue(homeSession.contains("SecondaryDestination.Donate"))
        assertFalse(mainActivity.contains("MainStandaloneRoute"))
    }

    companion object {
        @Throws(IOException::class)
        private fun read(relativePath: String): String {
            return SourceSmokeTestPaths.read(relativePath)
        }
    }
}
