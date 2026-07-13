package com.dpis.module.settings;

import android.content.Context;

/** View-free state/action boundary for the system font scale tool. */
public final class SystemFontScaleToolPresenter {
    public interface Listener {
        void onStateChanged(SystemFontScaleToolState state);
        void onWriteFailed();
    }

    private final Context context;
    private final SystemFontScaleSettingsGateway gateway;
    private final Listener listener;
    private SystemFontScaleToolState state;

    public SystemFontScaleToolPresenter(Context context, Listener listener) {
        this(context, new SystemFontScaleSettingsGateway(), listener);
    }

    SystemFontScaleToolPresenter(Context context, SystemFontScaleSettingsGateway gateway, Listener listener) {
        this.context = context;
        this.gateway = gateway;
        this.listener = listener;
    }

    public SystemFontScaleToolState state() {
        return state;
    }

    public void refresh() {
        boolean canWrite = gateway.canWrite(context);
        Integer current = gateway.readPercent(context);
        int pending = state != null && state.userSelectedPending
                ? state.pendingPercent : SystemFontScaleToolState.initialPendingPercent(current);
        state = new SystemFontScaleToolState(canWrite, current, pending,
                state != null && state.userSelectedPending, current == null);
        publish();
    }

    public void selectPendingPercent(int percent) {
        if (state == null) refresh();
        state = new SystemFontScaleToolState(state.canWrite, state.currentPercent,
                SystemFontScaleToolState.clampPercent(percent), true, state.unavailable);
        publish();
    }

    public void apply() {
        if (state == null || !state.canApply()) return;
        if (!gateway.writePercent(context, state.pendingPercent)) {
            if (listener != null) listener.onWriteFailed();
            return;
        }
        refresh();
    }

    public void restoreDefault() {
        if (state == null || !state.canRestore()) return;
        selectPendingPercent(SystemFontScaleToolState.DEFAULT_PERCENT);
        apply();
    }

    private void publish() {
        if (listener != null) listener.onStateChanged(state);
    }
}
