package com.dpis.module;

final class AppLoadCoordinator {
    static final int NO_REQUEST = -1;

    private int requestedVersion = 0;
    private int activeVersion = NO_REQUEST;
    private boolean loading = false;

    synchronized int onLoadRequested() {
        requestedVersion++;
        if (loading) {
            return NO_REQUEST;
        }
        loading = true;
        activeVersion = requestedVersion;
        return activeVersion;
    }

    synchronized LoadCompletion onLoadFinished(int finishedVersion) {
        if (!loading || finishedVersion != activeVersion) {
            return new LoadCompletion(false, NO_REQUEST);
        }
        boolean shouldApply = finishedVersion == requestedVersion;
        if (requestedVersion > finishedVersion) {
            activeVersion = requestedVersion;
            return new LoadCompletion(shouldApply, activeVersion);
        }
        loading = false;
        activeVersion = NO_REQUEST;
        return new LoadCompletion(shouldApply, NO_REQUEST);
    }

    static final class LoadCompletion {
        final boolean shouldApplyResult;
        final int nextRequestId;

        LoadCompletion(boolean shouldApplyResult, int nextRequestId) {
            this.shouldApplyResult = shouldApplyResult;
            this.nextRequestId = nextRequestId;
        }
    }
}
