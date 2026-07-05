package com.dpis.module;

import com.dpis.module.settings.SystemHookState;
import com.dpis.module.settings.SystemHookStateResolver;

final class SystemHooksToggleController {
    interface View {
        void render(SystemHookState state);

        void showInitRequired();

        void showSaveFailed();

        void showScopeRequired();
    }

    interface ScopeGateway {
        boolean isServiceAvailable();

        boolean hasSystemScopeSelected();
    }

    private final DpisConfigStore store;
    private final ScopeGateway scopeGateway;
    private final View view;
    private final Runnable onConfigSaved;

    SystemHooksToggleController(DpisConfigStore store, ScopeGateway scopeGateway, View view) {
        this(store, scopeGateway, view, RuntimeConfigDelivery::publishLocalSnapshotAfterSave);
    }

    SystemHooksToggleController(
            DpisConfigStore store,
            ScopeGateway scopeGateway,
            View view,
            Runnable onConfigSaved) {
        this.store = store;
        this.scopeGateway = scopeGateway;
        this.view = view;
        this.onConfigSaved = onConfigSaved;
    }

    void syncFromStore() {
        renderCurrentState();
    }

    void onUserToggle(boolean enabled) {
        if (!store.setSystemServerHooksEnabled(enabled)) {
            renderCurrentState();
            view.showSaveFailed();
            return;
        }
        onConfigSaved.run();
        if (enabled) {
            maybeShowMissingScopeHint();
        }
        renderCurrentState();
    }

    private void maybeShowMissingScopeHint() {
        if (!scopeGateway.isServiceAvailable()) {
            view.showInitRequired();
            return;
        }
        if (!safeHasSystemScopeSelected()) {
            view.showScopeRequired();
        }
    }

    private void renderCurrentState() {
        boolean desiredEnabled = store.isSystemServerHooksEnabled();
        boolean serviceAvailable = scopeGateway.isServiceAvailable();
        boolean scopeSelected = serviceAvailable && safeHasSystemScopeSelected();
        SystemHookState state = SystemHookStateResolver.resolve(
                desiredEnabled,
                false,
                serviceAvailable,
                scopeSelected);
        view.render(state);
    }

    private boolean safeHasSystemScopeSelected() {
        try {
            return scopeGateway.hasSystemScopeSelected();
        } catch (RuntimeException error) {
            return false;
        }
    }
}
