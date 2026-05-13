package com.dpis.displaytool;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

final class CompanionLog {
    private static final String TAG = "DPIS_TEST";
    private static final String PREFIX = "DPIS_TEST ";

    void runStart(String runId, String trigger, int sceneTotal, String variantMode, String pkg) {
        Log.i(TAG, PREFIX + formatRunStart(runId, trigger, sceneTotal, variantMode, pkg));
    }

    void runEnd(RunSummary summary, String pkg) {
        Log.i(TAG, PREFIX + formatRunEnd(summary, pkg));
    }

    boolean sceneEvent(
            Context context,
            String runId,
            String scene,
            String variant,
            String event,
            View root,
            TextView textView,
            float baseSp,
            String viewName,
            String pkg
    ) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        Configuration configuration = context.getResources().getConfiguration();
        float textPx = textView.getTextSize();
        float expectedPx = baseSp * metrics.scaledDensity;
        float densityFromDpi = metrics.densityDpi / 160f;
        float widthDpFromDensity = metrics.widthPixels / densityFromDpi;
        float heightDpFromDensity = metrics.heightPixels / densityFromDpi;
        float renderedScale = expectedPx <= 0f ? 0f : textPx / expectedPx;
        SceneAnomaly anomaly = SceneAnomaly.classify(textPx, expectedPx, configuration.fontScale);
        Log.i(TAG, PREFIX + formatSceneEvent(new SceneEventFields(
                runId,
                scene,
                variant,
                event,
                pkg,
                configuration.fontScale,
                metrics.densityDpi,
                metrics.scaledDensity,
                configuration.screenWidthDp,
                configuration.screenHeightDp,
                metrics.density,
                metrics.widthPixels,
                metrics.heightPixels,
                widthDpFromDensity,
                heightDpFromDensity,
                viewName,
                textPx,
                baseSp,
                expectedPx,
                renderedScale,
                textView.getLineCount(),
                root.getMeasuredWidth(),
                root.getMeasuredHeight(),
                anomaly
        )));
        return anomaly.suspicious;
    }

    void runRejected(String runId, String trigger, String reason, String pkg) {
        Log.i(TAG, PREFIX
                + field("stage", CompanionContract.STAGE)
                + field("run_id", runId)
                + field("event", "run_rejected")
                + field("trigger", trigger)
                + field("reason", reason)
                + field("pkg", pkg));
    }

    void commandRejected(String action, String reason, String pkg) {
        Log.i(TAG, PREFIX
                + field("stage", CompanionContract.STAGE)
                + field("event", "command_rejected")
                + field("action", sanitize(action))
                + field("reason", reason)
                + field("pkg", pkg));
    }

    void summaryDump(RunSummary summary, String pkg) {
        if (summary == null) {
            Log.i(TAG, PREFIX
                    + field("stage", CompanionContract.STAGE)
                    + field("event", "summary")
                    + field("has_run", false)
                    + field("pkg", pkg));
            return;
        }
        Log.i(TAG, PREFIX
                + field("stage", CompanionContract.STAGE)
                + field("run_id", summary.runId)
                + field("event", "summary")
                + field("trigger", summary.trigger)
                + field("scene_total", summary.sceneTotal)
                + field("scene_completed", summary.sceneCompleted)
                + field("suspicious_total", summary.suspiciousTotal)
                + field("error_total", summary.errorTotal)
                + field("pkg", pkg));
    }

    void resetState(String pkg) {
        Log.i(TAG, PREFIX
                + field("stage", CompanionContract.STAGE)
                + field("event", "reset_state")
                + field("pkg", pkg));
    }

    static String formatRunStart(
            String runId,
            String trigger,
            int sceneTotal,
            String variantMode,
            String pkg
    ) {
        return field("stage", CompanionContract.STAGE)
                + field("run_id", runId)
                + field("event", "run_start")
                + field("trigger", trigger)
                + field("scene_total", sceneTotal)
                + field("variant_mode", variantMode)
                + field("pkg", pkg);
    }

    static String formatRunEnd(RunSummary summary, String pkg) {
        return field("stage", CompanionContract.STAGE)
                + field("run_id", summary.runId)
                + field("event", "run_end")
                + field("trigger", summary.trigger)
                + field("scene_total", summary.sceneTotal)
                + field("scene_completed", summary.sceneCompleted)
                + field("suspicious_total", summary.suspiciousTotal)
                + field("error_total", summary.errorTotal)
                + field("pkg", pkg);
    }

    static String formatSceneEvent(SceneEventFields fields) {
        return field("stage", CompanionContract.STAGE)
                + field("run_id", fields.runId)
                + field("scene", fields.scene)
                + field("variant", fields.variant)
                + field("event", fields.event)
                + field("pkg", fields.pkg)
                + field("font_scale", two(fields.fontScale))
                + field("density_dpi", fields.densityDpi)
                + field("scaled_density", two(fields.scaledDensity))
                + field("width_dp", fields.widthDp)
                + field("height_dp", fields.heightDp)
                + field("density", two(fields.density))
                + field("width_px", fields.widthPx)
                + field("height_px", fields.heightPx)
                + field("width_dp_from_density", one(fields.widthDpFromDensity))
                + field("height_dp_from_density", one(fields.heightDpFromDensity))
                + field("view", fields.viewName)
                + field("text_px", one(fields.textPx))
                + field("base_sp", one(fields.baseSp))
                + field("expected_text_px", one(fields.expectedTextPx))
                + field("rendered_scale", two(fields.renderedScale))
                + field("line_count", fields.lineCount)
                + field("measured_w", fields.measuredW)
                + field("measured_h", fields.measuredH)
                + field("suspicious", fields.anomaly.suspicious)
                + optionalField("suspicious_reason", fields.anomaly.reason);
    }

    static String field(String key, Object value) {
        return key + "=" + sanitize(String.valueOf(value)) + " ";
    }

    private static String optionalField(String key, String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return field(key, value);
    }

    private static String one(float value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static String two(float value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "missing";
        }
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                builder.append(c);
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }

    static final class SceneEventFields {
        final String runId;
        final String scene;
        final String variant;
        final String event;
        final String pkg;
        final float fontScale;
        final int densityDpi;
        final float scaledDensity;
        final int widthDp;
        final int heightDp;
        final float density;
        final int widthPx;
        final int heightPx;
        final float widthDpFromDensity;
        final float heightDpFromDensity;
        final String viewName;
        final float textPx;
        final float baseSp;
        final float expectedTextPx;
        final float renderedScale;
        final int lineCount;
        final int measuredW;
        final int measuredH;
        final SceneAnomaly anomaly;

        SceneEventFields(
                String runId,
                String scene,
                String variant,
                String event,
                String pkg,
                float fontScale,
                int densityDpi,
                float scaledDensity,
                int widthDp,
                int heightDp,
                float density,
                int widthPx,
                int heightPx,
                float widthDpFromDensity,
                float heightDpFromDensity,
                String viewName,
                float textPx,
                float baseSp,
                float expectedTextPx,
                float renderedScale,
                int lineCount,
                int measuredW,
                int measuredH,
                SceneAnomaly anomaly
        ) {
            this.runId = runId;
            this.scene = scene;
            this.variant = variant;
            this.event = event;
            this.pkg = pkg;
            this.fontScale = fontScale;
            this.densityDpi = densityDpi;
            this.scaledDensity = scaledDensity;
            this.widthDp = widthDp;
            this.heightDp = heightDp;
            this.density = density;
            this.widthPx = widthPx;
            this.heightPx = heightPx;
            this.widthDpFromDensity = widthDpFromDensity;
            this.heightDpFromDensity = heightDpFromDensity;
            this.viewName = viewName;
            this.textPx = textPx;
            this.baseSp = baseSp;
            this.expectedTextPx = expectedTextPx;
            this.renderedScale = renderedScale;
            this.lineCount = lineCount;
            this.measuredW = measuredW;
            this.measuredH = measuredH;
            this.anomaly = anomaly;
        }
    }
}
