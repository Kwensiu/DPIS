package com.dpis.module;

import com.dpis.module.applist.AppListPage;
import com.dpis.module.applist.AppListPagerAdapter;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class AppListRowIndicatorSmokeTest {
    @Test
    public void itemRowDoesNotRenderTrailingIndicator() throws IOException {
        String layout = read("src/main/res/layout/item_app_entry.xml");
        String styles = read("src/main/res/values/styles.xml");
        String adapter = read("src/main/java/com/dpis/module/applist/AppListPagerAdapter.java");

        assertTrue(!layout.contains("@+id/expand_indicator"));
        assertTrue(!layout.contains("@drawable/ic_chevron_right_24"));
        assertTrue(!adapter.contains("expandIndicator"));
        assertTrue(layout.contains("@+id/app_icon_skeleton"));
        assertTrue(layout.contains("@drawable/bg_app_icon_skeleton_mask"));
        assertTrue(layout.contains("@dimen/land_app_entry_icon_size"));
        assertTrue(layout.contains("@dimen/land_app_entry_icon_spacing_end"));
        assertTrue(layout.contains("@dimen/land_app_entry_padding_vertical"));
        assertTrue(!layout.contains("land_app_entry_icon_slot_size"));
        assertTrue(!layout.contains("android:layout_gravity=\"center\""));
        assertTrue(!layout.contains("android:layout_marginTop=\"1dp\""));
        assertTrue(layout.contains("@style/Widget.Dpis.AppIdentityTitle"));
        assertTrue(layout.contains("@style/Widget.Dpis.AppIdentitySecondaryText"));
        assertTrue(layout.contains("@style/Widget.Dpis.AppIdentityStatusText"));
        assertTrue(styles.contains("name=\"Widget.Dpis.AppIdentityTitle\""));
        assertTrue(styles.contains("name=\"Widget.Dpis.AppIdentitySecondaryText\""));
        assertTrue(styles.contains("name=\"Widget.Dpis.AppIdentityStatusText\""));
        assertTrue(countMatches(styles, "<item name=\"android:includeFontPadding\">false</item>") >= 2);
        assertTrue(countMatches(styles, "<item name=\"android:lineSpacingExtra\">0dp</item>") >= 2);
        assertTrue(countMatches(styles, "@dimen/app_identity_secondary_text_line_padding_vertical")
                == 2);
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static int countMatches(String text, String target) {
        int count = 0;
        int index = 0;
        while (true) {
            int found = text.indexOf(target, index);
            if (found < 0) {
                return count;
            }
            count++;
            index = found + target.length();
        }
    }
}
