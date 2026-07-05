package com.dpis.module;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.After;
import org.junit.Test;

public final class FeedbackDiagnosticRuntimeHotPathEventsTest {
    @After
    public void tearDown() {
        FeedbackDiagnosticRuntimeEvents.cancel();
        FeedbackDiagnosticRuntimeHotPathEvents.resetForTest();
    }

    @Test
    public void doesNotRecordWhenDiagnosticClosed() {
        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                "com.example.app",
                "text_appearance",
                "view=android.widget.TextView, factor=1.2"
        );

        assertTrue(FeedbackDiagnosticRuntimeEvents.snapshotForTest().isEmpty());
    }

    @Test
    public void recordsBeginAppliedEndWhenDiagnosticOpen() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request());

        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                "com.example.app",
                "text_appearance",
                "view=android.widget.TextView, factor=1.2"
        );
        FeedbackDiagnosticRuntimeHotPathEvents.applied(
                "com.example.app",
                "text_appearance",
                "view=android.widget.TextView, factor=1.2"
        );
        FeedbackDiagnosticRuntimeHotPathEvents.end(
                "com.example.app",
                "text_appearance",
                "view=android.widget.TextView, factor=1.2"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertEquals(3, events.size());
        assertTrue(events.stream().anyMatch(event -> event.contains("stage=begin")));
        assertTrue(events.stream().anyMatch(event -> event.contains("stage=applied")));
        assertTrue(events.stream().anyMatch(event -> event.contains("stage=end")));
        assertTrue(events.stream().anyMatch(event -> event.contains("durationMs=")));
    }

    @Test
    public void repeatedAppliedEmitsRepeatedWrite() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request());

        FeedbackDiagnosticRuntimeHotPathEvents.applied(
                "com.example.app",
                "paint_fallback",
                "paint=android.graphics.Paint, in=10.0, out=12.0"
        );
        FeedbackDiagnosticRuntimeHotPathEvents.applied(
                "com.example.app",
                "paint_fallback",
                "paint=android.graphics.Paint, in=10.0, out=12.0"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertTrue(events.stream().anyMatch(event -> event.contains("stage=repeated_write")));
    }

    @Test
    public void recordsViewportRouteWhenCategoryRouteProvided() {
        FeedbackDiagnosticRuntimeEvents.start("com.example.app", request());

        FeedbackDiagnosticRuntimeHotPathEvents.applied(
                "com.example.app",
                "viewport",
                "resources_read_configuration_override",
                "source=ResourcesRead(getConfiguration), widthDp=360->324"
        );

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("route=viewport"));
        assertTrue(events.get(0).contains("stage=applied"));
        assertTrue(events.get(0).contains("resources_read_configuration_override"));
    }

    @Test
    public void doesNotEmitDiagnosticFallbackLogWhenCaptureInactive() {
        FeedbackDiagnosticRuntimeHotPathEvents.begin(
                "com.example.app",
                "text_appearance",
                "view=android.widget.TextView, factor=1.2"
        );

        assertFalse(FeedbackDiagnosticRuntimeTransport.isCaptureActive());
    }

    private static FeedbackDiagnosticCoordinator.Request request() {
        return new FeedbackDiagnosticCoordinator.Request(
                "com.example.app",
                "Example",
                "1.2.3",
                true,
                true,
                true,
                false,
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                120,
                FontApplyMode.FIELD_REWRITE,
                null,
                null,
                null
        );
    }
}
