package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.github.libxposed.service.XposedService;

public class BatchScopeRequestCoordinatorTest {
    @Test
    public void modernRequestsMissingPackagesInOneBatch() {
        RecordingHost host = new RecordingHost();
        FakeRequester requester = new FakeRequester();
        requester.scope = List.of("com.example.in.scope");
        BatchScopeRequestCoordinator coordinator =
                new BatchScopeRequestCoordinator(host, requester, true);

        BatchScopeRequestCoordinator.Result result = coordinator.requestMissingScope(
                List.of("com.example.in.scope", "com.example.missing.one", "com.example.missing.two"));

        assertTrue(result.requestStarted);
        assertEquals(List.of("com.example.missing.one", "com.example.missing.two"),
                requester.requestedPackages);
        assertEquals(1, requester.requestCallCount);
        assertEquals(R.string.quick_template_scope_request_started, host.toastIds.get(0).intValue());
    }

    @Test
    public void compatDoesNotRequestAndShowsManualGuidance() {
        RecordingHost host = new RecordingHost();
        FakeRequester requester = new FakeRequester();
        BatchScopeRequestCoordinator coordinator =
                new BatchScopeRequestCoordinator(host, requester, false);

        BatchScopeRequestCoordinator.Result result = coordinator.requestMissingScope(
                List.of("com.example.app"));

        assertFalse(result.requestStarted);
        assertTrue(result.manualRequired);
        assertEquals(0, requester.requestCallCount);
        assertEquals(R.string.quick_template_scope_manual_required, host.toastIds.get(0).intValue());
    }

    @Test
    public void unknownScopeDoesNotRollbackAndShowsManualGuidance() {
        RecordingHost host = new RecordingHost();
        FakeRequester requester = new FakeRequester();
        requester.throwOnGetScope = true;
        BatchScopeRequestCoordinator coordinator =
                new BatchScopeRequestCoordinator(host, requester, true);

        BatchScopeRequestCoordinator.Result result = coordinator.requestMissingScope(
                List.of("com.example.app"));

        assertFalse(result.requestStarted);
        assertTrue(result.manualRequired);
        assertEquals(0, requester.requestCallCount);
        assertEquals(R.string.quick_template_scope_manual_required, host.toastIds.get(0).intValue());
    }

    @Test
    public void partialApprovalRefreshesActualScopeState() {
        RecordingHost host = new RecordingHost();
        FakeRequester requester = new FakeRequester();
        requester.scope = List.of();
        BatchScopeRequestCoordinator coordinator =
                new BatchScopeRequestCoordinator(host, requester, true);

        coordinator.requestMissingScope(List.of("com.example.one", "com.example.two"));
        requester.listener.onScopeRequestApproved(List.of("com.example.one"));

        assertEquals(1, host.requestAppsLoadCount);
        assertEquals(R.string.quick_template_scope_request_approved,
                host.toastIds.get(host.toastIds.size() - 1).intValue());
    }

    private static final class FakeRequester implements BatchScopeRequestCoordinator.ScopeRequester {
        List<String> scope = List.of();
        List<String> requestedPackages = List.of();
        int requestCallCount;
        boolean throwOnGetScope;
        XposedService.OnScopeEventListener listener;

        @Override
        public List<String> getScope() {
            if (throwOnGetScope) {
                throw new RuntimeException("scope unavailable");
            }
            return scope;
        }

        @Override
        public void requestScope(List<String> packages, XposedService.OnScopeEventListener listener) {
            requestCallCount++;
            requestedPackages = new ArrayList<>(packages);
            this.listener = listener;
        }
    }

    private static final class RecordingHost implements BatchScopeRequestCoordinator.Host {
        final ArrayList<Integer> toastIds = new ArrayList<>();
        int requestAppsLoadCount;

        @Override
        public void showToast(int messageResId, Object... formatArgs) {
            toastIds.add(messageResId);
        }

        @Override
        public void requestAppsLoad() {
            requestAppsLoadCount++;
        }

        @Override
        public void runOnUiThread(Runnable runnable) {
            runnable.run();
        }
    }
}
