package com.dpis.module;

import com.dpis.module.home.HomeWorkspaceBinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class HomeWorkspaceLayoutSmokeTest {
    @Test
    public void statusMetricCardsUseStableDashboardLayout() throws IOException {
        String layout = read("src/main/res/layout/home_workspace.xml");
        String binder = read("src/main/java/com/dpis/module/home/HomeWorkspaceBinder.java");

        assertTrue(layout.contains("android:id=\"@+id/home_configured_apps_card\""));
        assertTrue(layout.contains("android:id=\"@+id/home_imported_fonts_card\""));
        assertTrue(layout.contains("android:id=\"@+id/home_templates_card\""));
        assertStatusCardUsesNaturalHeight(layout, "home_configured_apps_card");
        assertStatusCardUsesNaturalHeight(layout, "home_imported_fonts_card");
        assertStatusCardUsesNaturalHeight(layout, "home_templates_card");
        assertStatusCardPinsValueAndDescription(layout, "home_configured_apps_card");
        assertStatusCardPinsValueAndDescription(layout, "home_imported_fonts_card");
        assertStatusCardPinsValueAndDescription(layout, "home_templates_card");
        assertTrue(binder.contains("equalizeStatusCardHeights(workspaceView);"));
        assertTrue(binder.contains("setLayoutHeight(card, ViewGroup.LayoutParams.WRAP_CONTENT);"));
        assertTrue(binder.contains("addOnPreDrawListener"));
        assertTrue(binder.contains("removeOnPreDrawListener(this);"));
        assertTrue(binder.contains("generation != statusCardEqualizationGeneration"));
        assertEquals(88, HomeWorkspaceBinder.equalStatusCardHeightForTest(66, 88, 72));
        assertEquals(0, HomeWorkspaceBinder.equalStatusCardHeightForTest());
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static void assertStatusCardUsesNaturalHeight(String layout, String idName) {
        int idIndex = layout.indexOf("android:id=\"@+id/" + idName + "\"");
        assertTrue(idIndex >= 0);
        int nextCardIndex = layout.indexOf("<com.google.android.material.card.MaterialCardView",
                idIndex + 1);
        String cardBlock = nextCardIndex >= 0
                ? layout.substring(idIndex, nextCardIndex)
                : layout.substring(idIndex);
        assertTrue(cardBlock.contains("android:layout_height=\"wrap_content\""));
    }

    private static void assertStatusCardPinsValueAndDescription(String layout, String idName) {
        int idIndex = layout.indexOf("android:id=\"@+id/" + idName + "\"");
        assertTrue(idIndex >= 0);
        int nextCardIndex = layout.indexOf("<com.google.android.material.card.MaterialCardView",
                idIndex + 1);
        String cardBlock = nextCardIndex >= 0
                ? layout.substring(idIndex, nextCardIndex)
                : layout.substring(idIndex);
        assertTrue(cardBlock.contains("android:minHeight=\"@dimen/home_workspace_status_min_height\""));
        assertTrue(cardBlock.contains("<Space"));
        assertTrue(cardBlock.contains("android:layout_height=\"0dp\""));
        assertTrue(cardBlock.contains("android:layout_weight=\"1\""));
        assertTrue(cardBlock.contains("android:maxLines=\"2\""));
    }
}
