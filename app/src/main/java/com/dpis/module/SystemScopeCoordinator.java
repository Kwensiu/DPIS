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
        requestScope(packageName, appLabel, onTurnedInScope, null);
    }

    boolean requestScope(String packageName,
            String appLabel,
            Runnable onTurnedInScope,
            Runnable onRequestFinished) {
        return requestScope(packageName, appLabel, onTurnedInScope, onRequestFinished, true);
    }

    boolean requestScope(String packageName,
            String appLabel,
            Runnable onTurnedInScope,
            Runnable onRequestFinished,
            boolean showNotice) {
        XposedService service = DpisApplication.getXposedService();
        if (service == null) {
            return false;
        }
        if (showNotice) {
            host.showToast(R.string.system_hooks_scope_request_notice);
        }
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
                                if (onRequestFinished != null) {
                                    onRequestFinished.run();
                                }
                                host.requestAppsLoad();
                            });
                        }

                        @Override
                        public void onScopeRequestFailed(String message) {
                            host.runOnUiThread(() -> {
                                host.showToast(R.string.scope_add_failed, message);
                                if (onRequestFinished != null) {
                                    onRequestFinished.run();
                                }
                            });
                        }
                    });
            return true;
        } catch (RuntimeException exception) {
            host.showToast(R.string.scope_add_failed, exception.getMessage());
            if (onRequestFinished != null) {
                onRequestFinished.run();
            }
            return false;
        }
    }

    boolean resolveSystemHookEffectiveEnabled(DpiConfigStore store) {
        if (store == null) {
            return false;
        }
        boolean desiredEnabled = store.isSystemServerHooksEnabled();
        if ("compat100".equals(BuildConfig.FLAVOR)) {
            // Legacy LSPosed builds do not expose libxposed service scope state,
            // but the system_server hook is still driven by the stored toggle and LSPosed scope.
            return desiredEnabled;
        }
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
