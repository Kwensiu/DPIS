package com.dpis.module;

import java.util.Collections;
import java.util.List;

import io.github.libxposed.service.XposedService;

final class SystemScopeCoordinator {
    private static final String SYSTEM_SCOPE_MODERN = "system";

    interface Host {
        void showToast(int messageResId, Object... formatArgs);

        void requestAppsLoad();

        void runOnUiThread(Runnable runnable);
    }

    private final Host host;

    SystemScopeCoordinator(Host host) {
        this.host = host;
    }

    void toggleScope(String packageName,
            String appLabel,
            boolean currentlyInScope,
            Runnable onTurnedInScope,
            Runnable onTurnedOutScope) {
        XposedService service = DpisApplication.getXposedService();
        if (service == null) {
            host.showToast(R.string.status_save_requires_init);
            return;
        }
        if (currentlyInScope) {
            try {
                service.removeScope(Collections.singletonList(packageName));
                host.showToast(R.string.scope_remove_success, appLabel);
                if (onTurnedOutScope != null) {
                    onTurnedOutScope.run();
                }
                host.requestAppsLoad();
            } catch (RuntimeException exception) {
                host.showToast(R.string.scope_remove_failed);
            }
            return;
        }
        host.showToast(R.string.system_hooks_scope_request_notice);
        try {
            service.requestScope(Collections.singletonList(packageName),
                    new XposedService.OnScopeEventListener() {
                        @Override
                        public void onScopeRequestApproved(List<String> approved) {
                            host.runOnUiThread(() -> {
                                host.showToast(R.string.scope_add_success, appLabel);
                                if (onTurnedInScope != null) {
                                    onTurnedInScope.run();
                                }
                                host.requestAppsLoad();
                            });
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            host.runOnUiThread(
                                    () -> host.showToast(R.string.scope_add_failed, message));
                        }
                    });
        } catch (RuntimeException exception) {
            host.showToast(R.string.scope_add_failed, exception.getMessage());
        }
    }

    boolean resolveSystemHookEffectiveEnabled(DpiConfigStore store) {
        if (store == null) {
            return false;
        }
        boolean desiredEnabled = store.isSystemServerHooksEnabled();
        XposedService service = DpisApplication.getXposedService();
        boolean serviceAvailable = service != null;
        boolean scopeSelected = false;
        if (serviceAvailable) {
            try {
                List<String> scope = service.getScope();
                scopeSelected = scope != null && scope.contains(SYSTEM_SCOPE_MODERN);
            } catch (RuntimeException ignored) {
                scopeSelected = false;
            }
        }
        return SystemHookEffectiveView.resolve(
                desiredEnabled,
                serviceAvailable,
                scopeSelected).effectiveEnabled;
    }
}
