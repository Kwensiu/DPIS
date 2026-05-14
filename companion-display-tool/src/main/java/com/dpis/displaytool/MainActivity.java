package com.dpis.displaytool;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dpis.displaytool.scene.DisplayScene;
import com.dpis.displaytool.scene.SceneRegistry;

public final class MainActivity extends Activity {
    private SceneRegistry sceneRegistry;
    private RunOrchestrator runOrchestrator;
    private FrameLayout detailHost;
    private TextView detailTitle;
    private boolean coldStartScheduled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_companion_display_tool);

        sceneRegistry = SceneRegistry.create();
        detailHost = findViewById(R.id.detail_host);
        detailTitle = findViewById(R.id.detail_title);
        runOrchestrator = new RunOrchestrator(
                this,
                sceneRegistry,
                detailHost,
                new CompanionLog(),
                BuildConfig.APPLICATION_ID
        );

        bindSceneList();
        showScene(sceneRegistry.coreScenes().get(0).id(), CompanionContract.VARIANT_NORMAL);

        Intent initialIntent = getIntent();
        boolean controlLaunch = initialIntent.getBooleanExtra(
                CompanionContract.EXTRA_FROM_CONTROL_RECEIVER,
                false
        );
        boolean handledControl = handleControlIntent(initialIntent);
        if (savedInstanceState == null && !controlLaunch && !handledControl) {
            scheduleColdStartRun();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleControlIntent(intent);
    }

    private void bindSceneList() {
        LinearLayout sceneList = findViewById(R.id.scene_list);
        LayoutInflater inflater = LayoutInflater.from(this);
        addSceneGroupLabel(sceneList, inflater, R.string.scene_group_native);
        for (DisplayScene scene : sceneRegistry.coreScenes()) {
            addSceneRow(sceneList, inflater, scene);
        }
        addSceneGroupLabel(sceneList, inflater, R.string.scene_group_compose);
        for (DisplayScene scene : sceneRegistry.composeScenes()) {
            addSceneRow(sceneList, inflater, scene);
        }
    }

    private void addSceneGroupLabel(LinearLayout sceneList, LayoutInflater inflater, int labelResId) {
        TextView label = (TextView) inflater.inflate(R.layout.item_scene_row, sceneList, false);
        label.setText(labelResId);
        label.setEnabled(false);
        sceneList.addView(label);
    }

    private void addSceneRow(LinearLayout sceneList, LayoutInflater inflater, DisplayScene scene) {
        TextView row = (TextView) inflater.inflate(R.layout.item_scene_row, sceneList, false);
        row.setText(scene.id());
        row.setOnClickListener(view -> showScene(scene.id(), CompanionContract.VARIANT_NORMAL));
        sceneList.addView(row);
    }

    private void scheduleColdStartRun() {
        if (coldStartScheduled) {
            return;
        }
        coldStartScheduled = true;
        detailHost.post(() -> runOrchestrator.runAll(
                CompanionContract.TRIGGER_COLD_START,
                () -> runOrchestrator.runComposeColdStart(CompanionContract.TRIGGER_COLD_START)
        ));
    }

    private boolean handleControlIntent(Intent intent) {
        String action = intent.getStringExtra(CompanionContract.EXTRA_ACTION);
        if (action == null || action.isEmpty()) {
            return false;
        }
        String trigger = nonEmpty(
                intent.getStringExtra(CompanionContract.EXTRA_TRIGGER),
                CompanionContract.TRIGGER_ADB
        );
        String scene = intent.getStringExtra(CompanionContract.EXTRA_SCENE);
        String group = intent.getStringExtra(CompanionContract.EXTRA_GROUP);
        String variant = nonEmpty(
                intent.getStringExtra(CompanionContract.EXTRA_VARIANT),
                CompanionContract.VARIANT_NORMAL
        );
        switch (action) {
            case CompanionContract.ACTION_RUN_ALL:
                if (CompanionContract.GROUP_COMPOSE.equals(group)) {
                    runOrchestrator.runComposeColdStart(trigger);
                } else {
                    runOrchestrator.runAll(trigger);
                }
                return true;
            case CompanionContract.ACTION_RUN_SCENE:
                runOrchestrator.runScene(scene, variant, trigger);
                return true;
            case CompanionContract.ACTION_SHOW_SCENE:
                showScene(scene, variant);
                return true;
            case CompanionContract.ACTION_DUMP_SUMMARY:
                runOrchestrator.dumpSummary();
                return true;
            case CompanionContract.ACTION_RESET_STATE:
                runOrchestrator.resetState();
                return true;
            default:
                runOrchestrator.rejectCommand(action, "unsupported_action");
                return true;
        }
    }

    private void showScene(String sceneId, String variant) {
        DisplayScene scene = sceneRegistry.findById(sceneId);
        if (scene == null) {
            runOrchestrator.rejectCommand(CompanionContract.ACTION_SHOW_SCENE, "missing_scene");
            return;
        }
        if (!scene.supportsVariant(variant)) {
            runOrchestrator.rejectCommand(CompanionContract.ACTION_SHOW_SCENE, "unsupported_variant");
            return;
        }
        detailTitle.setText(scene.id() + " / " + variant);
        runOrchestrator.showScene(scene, variant);
    }

    private static String nonEmpty(String value, String fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        return value;
    }
}
