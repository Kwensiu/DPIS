package com.dpis.displaytool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CompanionLogTest {
    @Test
    public void runStartUsesStableFieldOrder() {
        String line = CompanionLog.formatRunStart(
                "123_1",
                "cold_start",
                6,
                "normal_only",
                "io.github.kwensiu.dpis.displaytool"
        );

        assertEquals(
                "stage=phase1 run_id=123_1 event=run_start trigger=cold_start "
                        + "scene_total=6 variant_mode=normal_only "
                        + "pkg=io.github.kwensiu.dpis.displaytool ",
                line
        );
        assertAscii(line);
    }

    @Test
    public void sceneEventKeepsCommonPrefixBeforeExtensions() {
        String line = CompanionLog.formatSceneEvent(new CompanionLog.SceneEventFields(
                "123_1",
                "baseline_text_sp",
                "normal",
                "first_layout",
                "io.github.kwensiu.dpis.displaytool",
                1.0f,
                346,
                2.1625f,
                360,
                792,
                2.1625f,
                1080,
                2376,
                499.4f,
                1098.7f,
                "text_primary",
                90.8f,
                14f,
                30.3f,
                3.0f,
                1,
                958,
                173,
                SceneAnomaly.classify(90.8f, 30.3f, 1.0f)
        ));

        assertTrue(line.startsWith(
                "stage=phase1 run_id=123_1 scene=baseline_text_sp variant=normal "
                        + "event=first_layout pkg=io.github.kwensiu.dpis.displaytool "
                        + "font_scale=1.00 density_dpi=346 scaled_density=2.16 "
                        + "width_dp=360 height_dp=792 "
        ));
        assertTrue(line.contains("width_dp_from_density=499.4 "));
        assertTrue(line.contains("rendered_scale=3.00 "));
        assertTrue(line.endsWith("suspicious=true suspicious_reason=inconsistent_readings "));
        assertAscii(line);
    }

    @Test
    public void composeSceneEventKeepsPrefixBeforeComposeFields() {
        String line = CompanionLog.formatComposeSceneEvent(new CompanionLog.SceneEventFields(
                "123_2",
                "compose_baseline_text",
                "normal",
                "compose_first_text_layout",
                "io.github.kwensiu.dpis.displaytool",
                1.0f,
                346,
                2.1625f,
                360,
                792,
                2.1625f,
                1080,
                2376,
                499.4f,
                1098.7f,
                "compose_text_primary",
                30.3f,
                14f,
                30.3f,
                1.0f,
                1,
                240,
                48,
                SceneAnomaly.NONE
        ), new ComposeRunFields(
                2.1625f,
                1.0f,
                14f,
                30.3f,
                1,
                240,
                48,
                1.0f,
                -1,
                -1,
                "baseline",
                ""
        ));

        assertEquals(
                "stage=phase1 run_id=123_2 scene=compose_baseline_text variant=normal "
                        + "event=compose_first_text_layout pkg=io.github.kwensiu.dpis.displaytool "
                        + "font_scale=1.00 density_dpi=346 scaled_density=2.16 "
                        + "width_dp=360 height_dp=792 density=2.16 "
                        + "width_px=1080 height_px=2376 "
                        + "width_dp_from_density=499.4 height_dp_from_density=1098.7 "
                        + "view=compose_text_primary text_px=30.3 base_sp=14.0 "
                        + "expected_text_px=30.3 rendered_scale=1.00 "
                        + "line_count=1 measured_w=240 measured_h=48 suspicious=false "
                        + "surface=compose compose_density=2.16 compose_font_scale=1.00 "
                        + "compose_text_sp=14.0 compose_text_px=30.3 compose_line_count=1 "
                        + "compose_layout_w=240 compose_layout_h=48 "
                        + "compose_rendered_scale=1.00 style_source=baseline ",
                line
        );
        assertFalse(line.contains("item_index=-1 "));
        assertFalse(line.contains("lazy_first_visible_index=-1 "));
        assertFalse(line.contains("container="));
        assertAscii(line);
    }

    @Test
    public void unsupportedCharactersAreSanitized() {
        String field = CompanionLog.field("reason", "bad value:中文");

        assertEquals("reason=bad_value___ ", field);
        assertAscii(field);
    }

    private static void assertAscii(String value) {
        for (int i = 0; i < value.length(); i++) {
            assertFalse("non-ascii at index " + i, value.charAt(i) > 0x7f);
        }
    }
}
