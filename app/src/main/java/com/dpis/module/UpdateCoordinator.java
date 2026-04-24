package com.dpis.module;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class UpdateCoordinator {
    static final long DEFAULT_STARTUP_CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L;
    static final long DEFAULT_FAILURE_RETRY_INTERVAL_MS = 30L * 60L * 1000L;

    private static final Pattern LEADING_NUMBER_PATTERN = Pattern.compile("^(\\d+)");

    private final long startupCheckIntervalMs;
    private final long failureRetryIntervalMs;

    UpdateCoordinator() {
        this(DEFAULT_STARTUP_CHECK_INTERVAL_MS, DEFAULT_FAILURE_RETRY_INTERVAL_MS);
    }

    UpdateCoordinator(long startupCheckIntervalMs, long failureRetryIntervalMs) {
        if (startupCheckIntervalMs <= 0L) {
            throw new IllegalArgumentException("startupCheckIntervalMs must be > 0");
        }
        if (failureRetryIntervalMs <= 0L) {
            throw new IllegalArgumentException("failureRetryIntervalMs must be > 0");
        }
        this.startupCheckIntervalMs = startupCheckIntervalMs;
        this.failureRetryIntervalMs = failureRetryIntervalMs;
    }

    StartupCheckGate evaluateStartupCheck(State state, long nowMs) {
        if (state == null) {
            throw new IllegalArgumentException("state == null");
        }
        if (state.startupCheckInProgress) {
            return new StartupCheckGate(
                    false,
                    StartupCheckReason.CHECK_IN_PROGRESS,
                    0L,
                    0L,
                    nowMs);
        }

        long requiredIntervalMs = state.lastUpdateCheckFailed
                ? failureRetryIntervalMs
                : startupCheckIntervalMs;
        long elapsedMs = nowMs - state.lastUpdateCheckTimestampMs;
        long nextAllowedAtMs = safeAdd(state.lastUpdateCheckTimestampMs, requiredIntervalMs);
        if (elapsedMs < requiredIntervalMs) {
            return new StartupCheckGate(
                    false,
                    StartupCheckReason.WAITING_FOR_INTERVAL,
                    requiredIntervalMs,
                    elapsedMs,
                    nextAllowedAtMs);
        }
        return new StartupCheckGate(
                true,
                StartupCheckReason.ALLOWED,
                requiredIntervalMs,
                elapsedMs,
                nowMs);
    }

    State markStartupCheckStarted(State state) {
        if (state == null) {
            throw new IllegalArgumentException("state == null");
        }
        if (state.startupCheckInProgress) {
            return state;
        }
        return state.copyWith(
                state.lastUpdateCheckTimestampMs,
                state.lastUpdateCheckFailed,
                state.lastPromptedUpdateVersionCode,
                true,
                state.downloadInProgress,
                state.downloadCancelRequested);
    }

    State markStartupCheckFinished(State state, long finishedAtMs, boolean requestSucceeded) {
        if (state == null) {
            throw new IllegalArgumentException("state == null");
        }
        return state.copyWith(
                finishedAtMs,
                !requestSucceeded,
                state.lastPromptedUpdateVersionCode,
                false,
                state.downloadInProgress,
                state.downloadCancelRequested);
    }

    PromptDecision evaluatePromptDecision(State state,
                                          int remoteVersionCode,
                                          String remoteVersionName,
                                          int localVersionCode,
                                          String localVersionName) {
        if (state == null) {
            throw new IllegalArgumentException("state == null");
        }
        if (!isRemoteVersionNewer(remoteVersionCode,
                remoteVersionName,
                localVersionCode,
                localVersionName)) {
            return new PromptDecision(false, PromptReason.REMOTE_NOT_NEWER);
        }
        if (remoteVersionCode <= state.lastPromptedUpdateVersionCode) {
            return new PromptDecision(false, PromptReason.ALREADY_PROMPTED);
        }
        return new PromptDecision(true, PromptReason.SHOULD_PROMPT);
    }

    State markPromptedVersion(State state, int promptedVersionCode) {
        if (state == null) {
            throw new IllegalArgumentException("state == null");
        }
        return state.copyWith(
                state.lastUpdateCheckTimestampMs,
                state.lastUpdateCheckFailed,
                Math.max(0, promptedVersionCode),
                state.startupCheckInProgress,
                state.downloadInProgress,
                state.downloadCancelRequested);
    }

    DownloadDecision requestDownloadStart(State state, String downloadUrl) {
        if (state == null) {
            throw new IllegalArgumentException("state == null");
        }
        if (state.downloadInProgress) {
            return new DownloadDecision(state, false, DownloadStartReason.ALREADY_IN_PROGRESS, null);
        }
        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            return new DownloadDecision(state, false, DownloadStartReason.EMPTY_URL, null);
        }

        String trimmedUrl = downloadUrl.trim();
        final URI parsed;
        try {
            parsed = URI.create(trimmedUrl);
        } catch (IllegalArgumentException error) {
            return new DownloadDecision(state, false, DownloadStartReason.INVALID_URL, null);
        }
        String scheme = parsed.getScheme();
        if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
            return new DownloadDecision(state, false, DownloadStartReason.HTTPS_REQUIRED, null);
        }

        State next = state.copyWith(
                state.lastUpdateCheckTimestampMs,
                state.lastUpdateCheckFailed,
                state.lastPromptedUpdateVersionCode,
                state.startupCheckInProgress,
                true,
                false);
        return new DownloadDecision(next, true, DownloadStartReason.STARTED, trimmedUrl);
    }

    State requestDownloadCancel(State state) {
        if (state == null) {
            throw new IllegalArgumentException("state == null");
        }
        if (!state.downloadInProgress) {
            return state;
        }
        return state.copyWith(
                state.lastUpdateCheckTimestampMs,
                state.lastUpdateCheckFailed,
                state.lastPromptedUpdateVersionCode,
                state.startupCheckInProgress,
                true,
                true);
    }

    State markDownloadFinished(State state) {
        if (state == null) {
            throw new IllegalArgumentException("state == null");
        }
        if (!state.downloadInProgress && !state.downloadCancelRequested) {
            return state;
        }
        return state.copyWith(
                state.lastUpdateCheckTimestampMs,
                state.lastUpdateCheckFailed,
                state.lastPromptedUpdateVersionCode,
                state.startupCheckInProgress,
                false,
                false);
    }

    static boolean isRemoteVersionNewer(int remoteCode,
                                        String remoteName,
                                        int localCode,
                                        String localName) {
        if (remoteCode > localCode) {
            return true;
        }
        if (remoteCode < localCode) {
            return false;
        }
        return compareSemVer(remoteName, localName) > 0;
    }

    static int compareSemVer(String left, String right) {
        int[] leftParts = parseSemVer(left);
        int[] rightParts = parseSemVer(right);
        if (leftParts == null || rightParts == null) {
            return 0;
        }
        for (int i = 0; i < leftParts.length; i++) {
            if (leftParts[i] == rightParts[i]) {
                continue;
            }
            return leftParts[i] > rightParts[i] ? 1 : -1;
        }
        return 0;
    }

    static int[] parseSemVer(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        String[] segments = normalized.split("\\.");
        if (segments.length < 3) {
            return null;
        }

        int[] result = new int[3];
        for (int i = 0; i < 3; i++) {
            Matcher matcher = LEADING_NUMBER_PATTERN.matcher(segments[i]);
            if (!matcher.find()) {
                return null;
            }
            try {
                result[i] = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return result;
    }

    private static long safeAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        if (right < 0L && left < Long.MIN_VALUE - right) {
            return Long.MIN_VALUE;
        }
        return left + right;
    }

    enum StartupCheckReason {
        ALLOWED,
        CHECK_IN_PROGRESS,
        WAITING_FOR_INTERVAL
    }

    static final class StartupCheckGate {
        final boolean shouldStart;
        final StartupCheckReason reason;
        final long requiredIntervalMs;
        final long elapsedMs;
        final long nextAllowedAtMs;

        StartupCheckGate(boolean shouldStart,
                         StartupCheckReason reason,
                         long requiredIntervalMs,
                         long elapsedMs,
                         long nextAllowedAtMs) {
            this.shouldStart = shouldStart;
            this.reason = reason;
            this.requiredIntervalMs = requiredIntervalMs;
            this.elapsedMs = elapsedMs;
            this.nextAllowedAtMs = nextAllowedAtMs;
        }
    }

    enum PromptReason {
        SHOULD_PROMPT,
        REMOTE_NOT_NEWER,
        ALREADY_PROMPTED
    }

    static final class PromptDecision {
        final boolean shouldPrompt;
        final PromptReason reason;

        PromptDecision(boolean shouldPrompt, PromptReason reason) {
            this.shouldPrompt = shouldPrompt;
            this.reason = reason;
        }
    }

    enum DownloadStartReason {
        STARTED,
        ALREADY_IN_PROGRESS,
        EMPTY_URL,
        INVALID_URL,
        HTTPS_REQUIRED
    }

    static final class DownloadDecision {
        final State nextState;
        final boolean started;
        final DownloadStartReason reason;
        final String normalizedUrl;

        DownloadDecision(State nextState,
                         boolean started,
                         DownloadStartReason reason,
                         String normalizedUrl) {
            this.nextState = nextState;
            this.started = started;
            this.reason = reason;
            this.normalizedUrl = normalizedUrl;
        }
    }

    static final class State {
        final long lastUpdateCheckTimestampMs;
        final boolean lastUpdateCheckFailed;
        final int lastPromptedUpdateVersionCode;
        final boolean startupCheckInProgress;
        final boolean downloadInProgress;
        final boolean downloadCancelRequested;

        static State initial(long lastUpdateCheckTimestampMs,
                             boolean lastUpdateCheckFailed,
                             int lastPromptedUpdateVersionCode) {
            return new State(
                    lastUpdateCheckTimestampMs,
                    lastUpdateCheckFailed,
                    lastPromptedUpdateVersionCode,
                    false,
                    false,
                    false);
        }

        static State empty() {
            return initial(0L, false, 0);
        }

        State(long lastUpdateCheckTimestampMs,
              boolean lastUpdateCheckFailed,
              int lastPromptedUpdateVersionCode,
              boolean startupCheckInProgress,
              boolean downloadInProgress,
              boolean downloadCancelRequested) {
            this.lastUpdateCheckTimestampMs = lastUpdateCheckTimestampMs;
            this.lastUpdateCheckFailed = lastUpdateCheckFailed;
            this.lastPromptedUpdateVersionCode = Math.max(0, lastPromptedUpdateVersionCode);
            this.startupCheckInProgress = startupCheckInProgress;
            this.downloadInProgress = downloadInProgress;
            this.downloadCancelRequested = downloadInProgress && downloadCancelRequested;
        }

        private State copyWith(long lastUpdateCheckTimestampMs,
                               boolean lastUpdateCheckFailed,
                               int lastPromptedUpdateVersionCode,
                               boolean startupCheckInProgress,
                               boolean downloadInProgress,
                               boolean downloadCancelRequested) {
            return new State(
                    lastUpdateCheckTimestampMs,
                    lastUpdateCheckFailed,
                    lastPromptedUpdateVersionCode,
                    startupCheckInProgress,
                    downloadInProgress,
                    downloadCancelRequested);
        }
    }
}
