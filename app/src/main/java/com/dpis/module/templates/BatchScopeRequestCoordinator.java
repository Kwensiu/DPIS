package com.dpis.module.templates;

import com.dpis.module.BuildConfig;
import com.dpis.module.DpisApplication;
import com.dpis.module.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import io.github.libxposed.service.XposedService;

public final class BatchScopeRequestCoordinator {
    public interface Host {
        void showToast(int messageResId, Object... formatArgs);

        void requestAppsLoad();

        void runOnUiThread(Runnable runnable);
    }

    interface ScopeRequester {
        List<String> getScope();

        void requestScope(List<String> packages, XposedService.OnScopeEventListener listener);
    }

    private final Host host;
    private final ScopeRequester scopeRequester;
    private final boolean modernFlavor;

    public BatchScopeRequestCoordinator(Host host) {
        this(host, fromService(DpisApplication.getXposedService()),
                "modern".equals(BuildConfig.FLAVOR));
    }

    BatchScopeRequestCoordinator(Host host, ScopeRequester scopeRequester, boolean modernFlavor) {
        this.host = host;
        this.scopeRequester = scopeRequester;
        this.modernFlavor = modernFlavor;
    }

    public Result requestMissingScope(List<String> successfulPackages) {
        LinkedHashSet<String> packages = sanitizePackages(successfulPackages);
        if (packages.isEmpty()) {
            return Result.noRequest();
        }
        if (!modernFlavor || scopeRequester == null) {
            notifyManualScopeRequired();
            return Result.manualRequired(packages.size());
        }

        List<String> scope;
        try {
            scope = scopeRequester.getScope();
        } catch (RuntimeException exception) {
            notifyManualScopeRequired();
            return Result.manualRequired(packages.size());
        }
        if (scope == null) {
            notifyManualScopeRequired();
            return Result.manualRequired(packages.size());
        }
        LinkedHashSet<String> missingPackages = new LinkedHashSet<>(packages);
        missingPackages.removeAll(new LinkedHashSet<>(scope));
        if (missingPackages.isEmpty()) {
            return Result.noRequest();
        }

        ArrayList<String> requestPackages = new ArrayList<>(missingPackages);
        try {
            scopeRequester.requestScope(requestPackages, new XposedService.OnScopeEventListener() {
                @Override
                public void onScopeRequestApproved(List<String> approved) {
                    if (host != null) {
                        host.runOnUiThread(() -> {
                            host.showToast(R.string.quick_template_scope_request_approved,
                                    approved != null ? approved.size() : 0);
                            host.requestAppsLoad();
                        });
                    }
                }

                @Override
                public void onScopeRequestFailed(String message) {
                    if (host != null) {
                        host.runOnUiThread(() -> {
                            host.showToast(R.string.quick_template_scope_manual_required);
                            host.requestAppsLoad();
                        });
                    }
                }
            });
            if (host != null) {
                host.showToast(R.string.quick_template_scope_request_started,
                        requestPackages.size());
            }
            return Result.requestStarted(requestPackages);
        } catch (RuntimeException exception) {
            notifyManualScopeRequired();
            return Result.manualRequired(requestPackages.size());
        }
    }

    private void notifyManualScopeRequired() {
        if (host != null) {
            host.showToast(R.string.quick_template_scope_manual_required);
        }
    }

    private static ScopeRequester fromService(XposedService service) {
        if (service == null) {
            return null;
        }
        return new ScopeRequester() {
            @Override
            public List<String> getScope() {
                return service.getScope();
            }

            @Override
            public void requestScope(List<String> packages,
                    XposedService.OnScopeEventListener listener) {
                service.requestScope(packages, listener);
            }
        };
    }

    private static LinkedHashSet<String> sanitizePackages(List<String> packages) {
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        if (packages == null) {
            return sanitized;
        }
        for (String packageName : packages) {
            if (packageName == null) {
                continue;
            }
            String trimmed = packageName.trim();
            if (!trimmed.isEmpty()) {
                sanitized.add(trimmed);
            }
        }
        return sanitized;
    }

    public static final class Result {
        final boolean requestStarted;
        final boolean manualRequired;
        final List<String> requestedPackages;
        final int affectedPackageCount;

        private Result(boolean requestStarted,
                boolean manualRequired,
                List<String> requestedPackages,
                int affectedPackageCount) {
            this.requestStarted = requestStarted;
            this.manualRequired = manualRequired;
            this.requestedPackages = Collections.unmodifiableList(new ArrayList<>(
                    requestedPackages != null ? requestedPackages : Collections.emptyList()));
            this.affectedPackageCount = affectedPackageCount;
        }

        static Result noRequest() {
            return new Result(false, false, Collections.emptyList(), 0);
        }

        static Result requestStarted(List<String> requestedPackages) {
            return new Result(true, false, requestedPackages,
                    requestedPackages != null ? requestedPackages.size() : 0);
        }

        static Result manualRequired(int affectedPackageCount) {
            return new Result(false, true, Collections.emptyList(), affectedPackageCount);
        }
    }
}
