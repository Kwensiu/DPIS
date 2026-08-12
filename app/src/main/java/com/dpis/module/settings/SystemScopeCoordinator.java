package com.dpis.module.settings;

import com.dpis.module.BuildConfig;
import com.dpis.module.DpisApplication;
import com.dpis.module.DpisConfigStore;
import com.dpis.module.DpisLog;
import com.dpis.module.R;

import java.util.Collections;
import java.util.List;

import io.github.libxposed.service.XposedService;

public final class SystemScopeCoordinator {
    public interface Host {
        void showToast(int messageResId, Object... formatArgs);

        void requestAppsLoad();

        void runOnUiThread(Runnable runnable);
    }

    private final Host host;

    public SystemScopeCoordinator(Host host) {
        this.host = host;
    }

    public void toggleScope(String packageName,
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

    public boolean requestScope(String packageName,
            String appLabel,
            Runnable onTurnedInScope,
            Runnable onRequestFinished) {
        return requestScope(packageName, appLabel, onTurnedInScope, onRequestFinished, true);
    }

    public boolean requestScope(String packageName,
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

    public static boolean resolveSystemHookEffectiveEnabled(DpisConfigStore store) {
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
                scopeSelected = SystemFrameworkScope.containsSystemScope(scope);
            } catch (RuntimeException ignored) {
                scopeSelected = false;
            }
        }
        boolean effectiveEnabled = resolveSystemHookEffectiveEnabled(
                desiredEnabled,
                serviceAvailable,
                scopeSelected);
        DpisLog.i("system hook resolve: desired=" + desiredEnabled
                + ", serviceAvailable=" + serviceAvailable
                + ", scopeSelected=" + scopeSelected
                + ", effective=" + effectiveEnabled);
        return effectiveEnabled;
    }

    public static boolean resolveSystemHookEffectiveEnabled(boolean desiredEnabled,
                                                     boolean serviceAvailable,
                                                     boolean scopeSelected) {
        return resolveSystemHookEffectiveEnabled(
                desiredEnabled,
                serviceAvailable,
                scopeSelected,
                "legacy".equals(BuildConfig.FLAVOR));
    }

    public static boolean resolveSystemHookEffectiveEnabled(boolean desiredEnabled,
                                                     boolean serviceAvailable,
                                                     boolean scopeSelected,
                                                     boolean legacyFlavor) {
        if (legacyFlavor && !serviceAvailable) {
            // Legacy keeps a stored-toggle fallback when the libxposed service
            // is unavailable, but it still prefers a real scope read when the
            // service exists.
            return desiredEnabled;
        }
        return SystemHookEffectiveView.resolve(
                desiredEnabled,
                serviceAvailable,
                scopeSelected).effectiveEnabled;
    }
}
