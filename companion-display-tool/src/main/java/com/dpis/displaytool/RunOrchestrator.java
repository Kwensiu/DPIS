package com.dpis.displaytool;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dpis.displaytool.scene.DisplayScene;
import com.dpis.displaytool.scene.ScenePresentation;
import com.dpis.displaytool.scene.SceneRegistry;
import com.dpis.displaytool.scene.SceneRuntime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class RunOrchestrator {
    private static final int COMPOSE_READY_MAX_ATTEMPTS = 8;
    private static final long COMPOSE_READY_RETRY_MS = 16L;
    private static final AtomicInteger RUN_SEQUENCE = new AtomicInteger();

    private final Activity activity;
    private final SceneRegistry registry;
    private final FrameLayout detailHost;
    private final CompanionLog log;
    private final String packageName;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean active;
    private RunSummary latestSummary;
    private Dialog activeDialog;

    RunOrchestrator(
            Activity activity,
            SceneRegistry registry,
            FrameLayout detailHost,
            CompanionLog log,
            String packageName
    ) {
        this.activity = activity;
        this.registry = registry;
        this.detailHost = detailHost;
        this.log = log;
        this.packageName = packageName;
    }

    void runAll(String trigger) {
        runAll(trigger, null);
    }

    void runAll(String trigger, Runnable afterRun) {
        startRun(
                trigger,
                CompanionContract.VARIANT_MODE_NORMAL_ONLY,
                runsFor(registry.coreScenes(), CompanionContract.VARIANT_NORMAL),
                afterRun
        );
    }

    void runComposeColdStart(String trigger) {
        runComposeColdStart(trigger, null);
    }

    void runComposeColdStart(String trigger, Runnable afterRun) {
        startRun(
                trigger,
                CompanionContract.VARIANT_MODE_NORMAL_ONLY,
                runsFor(registry.composeColdStartScenes(), CompanionContract.VARIANT_NORMAL),
                afterRun
        );
    }

    void runScene(String sceneId, String variant, String trigger) {
        DisplayScene scene = registry.findById(sceneId);
        if (scene == null) {
            rejectCommand(CompanionContract.ACTION_RUN_SCENE, "missing_scene");
            return;
        }
        if (!scene.supportsVariant(variant)) {
            rejectCommand(CompanionContract.ACTION_RUN_SCENE, "unsupported_variant");
            return;
        }
        List<SceneRun> runs = new ArrayList<>();
        runs.add(new SceneRun(scene, variant));
        startRun(trigger, CompanionContract.VARIANT_MODE_SINGLE, runs, null);
    }

    void showScene(DisplayScene scene, String variant) {
        clearDialog();
        detailHost.removeAllViews();
        SceneRuntime runtime = new SceneRuntime(activity, detailHost, scene.id(), variant);
        ScenePresentation presentation = scene.create(runtime, variant);
        if (presentation.kind() == ScenePresentation.Kind.DIALOG) {
            presentation.dialog().show();
            activeDialog = presentation.dialog();
        } else {
            detailHost.addView(
                    presentation.view(),
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );
        }
    }

    void dumpSummary() {
        log.summaryDump(latestSummary, packageName);
    }

    void resetState() {
        clearDialog();
        active = false;
        latestSummary = null;
        detailHost.removeAllViews();
        log.resetState(packageName);
    }

    void rejectCommand(String action, String reason) {
        log.commandRejected(action, reason, packageName);
    }

    private void startRun(
            String trigger,
            String variantMode,
            List<SceneRun> runs,
            Runnable afterRun
    ) {
        if (active) {
            log.runRejected(currentRunId(), trigger, "active_run", packageName);
            return;
        }
        clearDialog();
        active = true;
        String runId = nextRunId();
        RunSummary summary = new RunSummary(runId, trigger, runs.size());
        latestSummary = summary;
        log.runStart(runId, trigger, runs.size(), variantMode, packageName);
        runNext(summary, runs, 0, afterRun);
    }

    private void runNext(RunSummary summary, List<SceneRun> runs, int index, Runnable afterRun) {
        if (index >= runs.size()) {
            active = false;
            log.runEnd(summary, packageName);
            if (afterRun != null) {
                afterRun.run();
            }
            return;
        }
        SceneRun run = runs.get(index);
        clearDialog();
        detailHost.removeAllViews();
        SceneRuntime runtime = new SceneRuntime(
                activity,
                detailHost,
                run.scene.id(),
                run.variant
        );
        ScenePresentation presentation;
        try {
            presentation = run.scene.create(runtime, run.variant);
        } catch (RuntimeException exception) {
            summary.errorTotal++;
            mainHandler.post(() -> runNext(summary, runs, index + 1, afterRun));
            return;
        }
        if (presentation.kind() == ScenePresentation.Kind.DIALOG) {
            runDialogScene(summary, runs, index, run, presentation, afterRun);
        } else {
            runViewScene(summary, runs, index, run, presentation, afterRun);
        }
    }

    private void runViewScene(
            RunSummary summary,
            List<SceneRun> runs,
            int index,
            SceneRun run,
            ScenePresentation presentation,
            Runnable afterRun
    ) {
        View view = presentation.view();
        detailHost.addView(
                view,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
        logAfterLayout(view, presentation.textView(), presentation.baseSp(), presentation.viewName(),
                run, presentation.event(), presentation.composeFieldsProvider(), summary, () -> {
                    summary.sceneCompleted++;
                    mainHandler.post(() -> runNext(summary, runs, index + 1, afterRun));
                });
    }

    private void runDialogScene(
            RunSummary summary,
            List<SceneRun> runs,
            int index,
            SceneRun run,
            ScenePresentation presentation,
            Runnable afterRun
    ) {
        activeDialog = presentation.dialog();
        activeDialog.setOnShowListener(dialog -> {
            View root = activeDialog.getWindow() == null
                    ? presentation.view()
                    : activeDialog.getWindow().getDecorView();
            logAfterLayout(root, presentation.textView(), presentation.baseSp(), presentation.viewName(),
                    run, presentation.event(), presentation.composeFieldsProvider(), summary, () -> {
                        summary.sceneCompleted++;
                        activeDialog.dismiss();
                        activeDialog = null;
                        mainHandler.post(() -> runNext(summary, runs, index + 1, afterRun));
                    });
        });
        activeDialog.show();
    }

    private void logAfterLayout(
            View root,
            TextView textView,
            float baseSp,
            String viewName,
            SceneRun run,
            String event,
            ScenePresentation.ComposeFieldsProvider composeFieldsProvider,
            RunSummary summary,
            Runnable afterLog
    ) {
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean logged;

            @Override
            public void onGlobalLayout() {
                if (logged) {
                    return;
                }
                logged = true;
                root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if (composeFieldsProvider != null) {
                    logComposeWhenReady(
                            root,
                            baseSp,
                            viewName,
                            run,
                            event,
                            composeFieldsProvider,
                            summary,
                            afterLog,
                            0
                    );
                    return;
                }
                TextView resolvedTextView = textView == null
                        ? root.findViewById(com.dpis.displaytool.R.id.text_primary)
                        : textView;
                if (resolvedTextView == null) {
                    summary.errorTotal++;
                    afterLog.run();
                    return;
                }
                boolean suspicious = log.sceneEvent(
                        activity,
                        currentRunId(),
                        run.scene.id(),
                        run.variant,
                        event,
                        root,
                        resolvedTextView,
                        baseSp,
                        viewName,
                        packageName
                );
                if (suspicious) {
                    summary.suspiciousTotal++;
                }
                afterLog.run();
            }
        });
        root.requestLayout();
    }

    private void logComposeWhenReady(
            View root,
            float baseSp,
            String viewName,
            SceneRun run,
            String event,
            ScenePresentation.ComposeFieldsProvider composeFieldsProvider,
            RunSummary summary,
            Runnable afterLog,
            int attempt
    ) {
        if (!composeFieldsProvider.isReady()) {
            if (attempt >= COMPOSE_READY_MAX_ATTEMPTS) {
                summary.errorTotal++;
                afterLog.run();
                return;
            }
            mainHandler.postDelayed(() -> logComposeWhenReady(
                    root,
                    baseSp,
                    viewName,
                    run,
                    event,
                    composeFieldsProvider,
                    summary,
                    afterLog,
                    attempt + 1
            ), COMPOSE_READY_RETRY_MS);
            return;
        }

        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        Configuration configuration = activity.getResources().getConfiguration();
        ComposeRunFields composeFields = composeFieldsProvider.fields(metrics.scaledDensity);
        float expectedPx = baseSp * metrics.scaledDensity;
        float densityFromDpi = metrics.densityDpi / 160f;
        float widthDpFromDensity = metrics.widthPixels / densityFromDpi;
        float heightDpFromDensity = metrics.heightPixels / densityFromDpi;
        SceneAnomaly anomaly = SceneAnomaly.classify(
                composeFields.composeTextPx,
                expectedPx,
                configuration.fontScale
        );
        log.composeSceneEvent(
                new CompanionLog.SceneEventFields(
                        currentRunId(),
                        run.scene.id(),
                        run.variant,
                        event,
                        packageName,
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
                        composeFields.composeTextPx,
                        baseSp,
                        expectedPx,
                        expectedPx <= 0f ? 0f : composeFields.composeTextPx / expectedPx,
                        composeFields.composeLineCount,
                        root.getMeasuredWidth(),
                        root.getMeasuredHeight(),
                        anomaly
                ),
                composeFields
        );
        if (anomaly.suspicious) {
            summary.suspiciousTotal++;
        }
        afterLog.run();
    }

    private static List<SceneRun> runsFor(List<DisplayScene> scenes, String variant) {
        List<SceneRun> runs = new ArrayList<>();
        for (DisplayScene scene : scenes) {
            runs.add(new SceneRun(scene, variant));
        }
        return runs;
    }

    private void clearDialog() {
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }
        activeDialog = null;
    }

    private String currentRunId() {
        if (latestSummary == null) {
            return "none";
        }
        return latestSummary.runId;
    }

    private static String nextRunId() {
        return System.currentTimeMillis() + "_" + RUN_SEQUENCE.incrementAndGet();
    }

    private static final class SceneRun {
        final DisplayScene scene;
        final String variant;

        SceneRun(DisplayScene scene, String variant) {
            this.scene = scene;
            this.variant = variant;
        }
    }
}
