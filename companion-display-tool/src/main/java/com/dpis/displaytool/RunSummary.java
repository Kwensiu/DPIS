package com.dpis.displaytool;

final class RunSummary {
    final String runId;
    final String trigger;
    final int sceneTotal;
    int sceneCompleted;
    int suspiciousTotal;
    int errorTotal;

    RunSummary(String runId, String trigger, int sceneTotal) {
        this.runId = runId;
        this.trigger = trigger;
        this.sceneTotal = sceneTotal;
    }
}
