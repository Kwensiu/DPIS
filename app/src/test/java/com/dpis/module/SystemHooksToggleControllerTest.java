package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SystemHooksToggleControllerTest {
    @Test
    public void syncFromStore_reflectsStoredState() {
        DpiConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.syncFromStore();

        assertFalse(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertFalse(view.lastState.desiredEnabled);
        assertFalse(view.lastState.effectiveEnabled);
    }

    @Test
    public void disable_setsStoreFalse() {
        DpiConfigStore store = createStore(true);
        FakeScopeGateway gateway = new FakeScopeGateway();
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(false);

        assertFalse(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertFalse(store.isSystemServerHooksEnabled());
        assertEquals(1, gateway.removeScopeCallCount);
    }

    @Test
    public void disable_whenScopeRemoveFails_keepsStoreStateAndShowsError() {
        DpiConfigStore store = createStore(true);
        FakeScopeGateway gateway = new FakeScopeGateway();
        gateway.removeScopeSuccess = false;
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(false);

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertTrue(store.isSystemServerHooksEnabled());
        assertEquals(1, view.scopeRemoveFailedCount);
        assertEquals(1, gateway.removeScopeCallCount);
    }

    @Test
    public void disable_whenStoreWriteFails_rollsBackToOnState() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        FakeScopeGateway gateway = new FakeScopeGateway();
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);
        prefs.setCommitResult(false);

        controller.onUserToggle(false);

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertEquals(1, view.saveFailedCount);
        assertTrue(store.isSystemServerHooksEnabled());
    }

    @Test
    public void enable_withoutService_keepsDesiredOnAndShowsInitRequired() {
        DpiConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        gateway.serviceAvailable = false;
        gateway.hasSystemScopeSelected = false;
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(true);

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertFalse(view.lastState.effectiveEnabled);
        assertEquals(1, view.initRequiredCount);
        assertFalse(gateway.scopeRequestTriggered);
        assertTrue(store.isSystemServerHooksEnabled());
    }

    @Test
    public void syncFromStore_whenServiceBecomesAvailable_requestsScopeForDesiredOnState() {
        DpiConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        gateway.serviceAvailable = false;
        gateway.hasSystemScopeSelected = false;
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(true);
        assertEquals(0, gateway.scopeRequestCount);

        gateway.serviceAvailable = true;
        controller.syncFromStore();

        assertEquals(1, gateway.scopeRequestCount);
        assertTrue(view.lastState.switchChecked);
        assertFalse(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertFalse(view.lastState.effectiveEnabled);
        assertEquals(1, view.scopeRequestNoticeCount);
    }

    @Test
    public void enable_staysPendingUntilScopeCallback() {
        DpiConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        gateway.hasSystemScopeSelected = false;
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(true);

        assertTrue(view.lastState.switchChecked);
        assertFalse(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertFalse(view.lastState.effectiveEnabled);
        assertEquals(1, view.scopeRequestNoticeCount);
        assertTrue(gateway.scopeRequestTriggered);
        assertNotNull(gateway.callback);
    }

    @Test
    public void enable_whenScopeApproved_setsOnState() {
        DpiConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(true);
        gateway.approve(true);

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertTrue(view.lastState.effectiveEnabled);
        assertTrue(store.isSystemServerHooksEnabled());
    }

    @Test
    public void enable_whenScopeDenied_showsScopeRequiredAndKeepsDesiredOn() {
        DpiConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        gateway.hasSystemScopeSelected = false;
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(true);
        gateway.approve(false);

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertFalse(view.lastState.effectiveEnabled);
        assertEquals(1, view.scopeRequiredCount);
        assertTrue(store.isSystemServerHooksEnabled());
    }

    @Test
    public void enable_whenDeniedButScopeAlreadySelected_setsOnState() {
        DpiConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        gateway.hasSystemScopeSelected = true;
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(true);
        gateway.approve(false);

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertTrue(view.lastState.effectiveEnabled);
        assertEquals(0, view.scopeRequiredCount);
        assertTrue(store.isSystemServerHooksEnabled());
    }

    @Test
    public void syncFromStore_whenScopeRequestPending_keepsSwitchDisabled() {
        DpiConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        gateway.hasSystemScopeSelected = false;
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(true);
        controller.syncFromStore();

        assertTrue(view.lastState.switchChecked);
        assertFalse(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertFalse(view.lastState.effectiveEnabled);
        assertTrue(gateway.scopeRequestTriggered);
    }

    @Test
    public void enable_whenScopeCallbackFails_showsAddFailedAndKeepsDesiredOn() {
        DpiConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        gateway.hasSystemScopeSelected = false;
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(true);
        gateway.fail("denied by manager");

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertFalse(view.lastState.effectiveEnabled);
        assertEquals(1, view.scopeAddFailedCount);
        assertEquals("denied by manager", view.lastScopeAddFailedMessage);
        assertTrue(store.isSystemServerHooksEnabled());
    }

    private static DpiConfigStore createStore(boolean hooksEnabled) {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        if (!hooksEnabled) {
            store.setSystemServerHooksEnabled(false);
        }
        return store;
    }

    private static final class FakeView implements SystemHooksToggleController.View {
        SystemHookState lastState;
        int initRequiredCount;
        int saveFailedCount;
        int scopeRequestNoticeCount;
        int scopeRequiredCount;
        int scopeRemoveFailedCount;
        int scopeAddFailedCount;
        String lastScopeAddFailedMessage;

        @Override
        public void render(SystemHookState state) {
            this.lastState = state;
        }

        @Override
        public void showInitRequired() {
            initRequiredCount++;
        }

        @Override
        public void showSaveFailed() {
            saveFailedCount++;
        }

        @Override
        public void showScopeRequestNotice() {
            scopeRequestNoticeCount++;
        }

        @Override
        public void showScopeRequired() {
            scopeRequiredCount++;
        }

        @Override
        public void showScopeRemoveFailed() {
            scopeRemoveFailedCount++;
        }

        @Override
        public void showScopeAddFailed(String message) {
            scopeAddFailedCount++;
            lastScopeAddFailedMessage = message;
        }
    }

    private static final class FakeScopeGateway implements SystemHooksToggleController.ScopeGateway {
        boolean serviceAvailable = true;
        boolean removeScopeSuccess = true;
        boolean hasSystemScopeSelected = true;
        boolean scopeRequestTriggered;
        int scopeRequestCount;
        int removeScopeCallCount;
        ScopeRequestCallback callback;

        @Override
        public boolean isServiceAvailable() {
            return serviceAvailable;
        }

        @Override
        public boolean removeSystemScopeIfAvailable() {
            removeScopeCallCount++;
            return removeScopeSuccess;
        }

        @Override
        public boolean hasSystemScopeSelected() {
            return hasSystemScopeSelected;
        }

        @Override
        public void requestSystemScope(ScopeRequestCallback callback) {
            scopeRequestTriggered = true;
            scopeRequestCount++;
            this.callback = callback;
        }

        void approve(boolean granted) {
            if (callback != null) {
                callback.onApproved(granted);
            }
        }

        void fail(String message) {
            if (callback != null) {
                callback.onFailed(message);
            }
        }
    }
}
