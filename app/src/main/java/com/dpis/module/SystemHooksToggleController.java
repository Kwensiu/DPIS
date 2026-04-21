package com.dpis.module;

final class SystemHooksToggleController {
    interface View {
        void render(SystemHookState state);

        void showInitRequired();

        void showSaveFailed();

        void showScopeRequestNotice();

        void showScopeRequired();

        void showScopeRemoveFailed();

        void showScopeAddFailed(String message);
    }

    interface ScopeGateway {
        interface ScopeRequestCallback {
            void onApproved(boolean granted);

            void onFailed(String message);
        }

        boolean isServiceAvailable();

        boolean removeSystemScopeIfAvailable();

        boolean hasSystemScopeSelected();

        void requestSystemScope(ScopeRequestCallback callback);
    }

    private final DpiConfigStore store;
    private final ScopeGateway scopeGateway;
    private final View view;
    private boolean requestPending;
    private boolean scopeRequestDeferredUntilServiceAvailable;

    SystemHooksToggleController(DpiConfigStore store, ScopeGateway scopeGateway, View view) {
        this.store = store;
        this.scopeGateway = scopeGateway;
        this.view = view;
    }

    void syncFromStore() {
        if (shouldResumeDeferredScopeRequest()) {
            requestSystemScope();
            return;
        }
        renderCurrentState();
    }

    void onUserToggle(boolean enabled) {
        if (!enabled) {
            disableHooks();
            return;
        }
        enableHooks();
    }

    private void disableHooks() {
        requestPending = false;
        scopeRequestDeferredUntilServiceAvailable = false;
        if (!scopeGateway.removeSystemScopeIfAvailable()) {
            renderCurrentState();
            view.showScopeRemoveFailed();
            return;
        }
        if (!store.setSystemServerHooksEnabled(false)) {
            renderCurrentState();
            view.showSaveFailed();
            return;
        }
        renderCurrentState();
    }

    private void enableHooks() {
        if (!store.setSystemServerHooksEnabled(true)) {
            renderCurrentState();
            view.showSaveFailed();
            return;
        }
        if (!scopeGateway.isServiceAvailable()) {
            scopeRequestDeferredUntilServiceAvailable = true;
            view.showInitRequired();
            renderCurrentState();
            return;
        }
        scopeRequestDeferredUntilServiceAvailable = false;
        if (safeHasSystemScopeSelected()) {
            renderCurrentState();
            return;
        }
        requestSystemScope();
    }

    private void requestSystemScope() {
        scopeRequestDeferredUntilServiceAvailable = false;
        requestPending = true;
        renderCurrentState();
        view.showScopeRequestNotice();
        scopeGateway.requestSystemScope(new ScopeGateway.ScopeRequestCallback() {
            @Override
            public void onApproved(boolean approved) {
                requestPending = false;
                boolean granted = approved || safeHasSystemScopeSelected();
                if (!granted) {
                    renderCurrentState();
                    view.showScopeRequired();
                    return;
                }
                renderCurrentState();
            }

            @Override
            public void onFailed(String message) {
                requestPending = false;
                renderCurrentState();
                view.showScopeAddFailed(message);
            }
        });
    }

    private boolean shouldResumeDeferredScopeRequest() {
        if (requestPending || !scopeRequestDeferredUntilServiceAvailable) {
            return false;
        }
        if (!store.isSystemServerHooksEnabled()) {
            scopeRequestDeferredUntilServiceAvailable = false;
            return false;
        }
        if (!scopeGateway.isServiceAvailable()) {
            return false;
        }
        if (safeHasSystemScopeSelected()) {
            scopeRequestDeferredUntilServiceAvailable = false;
            return false;
        }
        return true;
    }

    private void renderCurrentState() {
        boolean desiredEnabled = store.isSystemServerHooksEnabled();
        boolean serviceAvailable = scopeGateway.isServiceAvailable();
        boolean scopeSelected = serviceAvailable && safeHasSystemScopeSelected();
        SystemHookState state = SystemHookStateResolver.resolve(
                desiredEnabled,
                requestPending,
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
