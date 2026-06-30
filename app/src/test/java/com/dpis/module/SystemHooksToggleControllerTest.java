package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemHooksToggleControllerTest {
    @Test
    public void syncFromStore_reflectsStoredState() {
        DpisConfigStore store = createStore(false);
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
    public void disable_setsStoreFalse_withoutScopeMutation() {
        DpisConfigStore store = createStore(true);
        FakeScopeGateway gateway = new FakeScopeGateway();
        FakeView view = new FakeView();
        FakeDelivery delivery = new FakeDelivery();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view, delivery);

        controller.onUserToggle(false);

        assertFalse(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertFalse(store.isSystemServerHooksEnabled());
        assertEquals(0, view.scopeRequiredCount);
        assertEquals(0, view.initRequiredCount);
        assertEquals(1, delivery.resyncCount);
    }

    @Test
    public void disable_whenStoreWriteFails_rollsBackToOnState() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        FakeScopeGateway gateway = new FakeScopeGateway();
        FakeView view = new FakeView();
        FakeDelivery delivery = new FakeDelivery();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view, delivery);
        prefs.setCommitResult(false);

        controller.onUserToggle(false);

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertEquals(1, view.saveFailedCount);
        assertTrue(store.isSystemServerHooksEnabled());
        assertEquals(0, delivery.resyncCount);
    }

    @Test
    public void enable_withoutService_keepsDesiredOnAndShowsInitRequired() {
        DpisConfigStore store = createStore(false);
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
        assertEquals(0, view.scopeRequiredCount);
        assertTrue(store.isSystemServerHooksEnabled());
    }

    @Test
    public void enable_withScopeMissing_showsScopeRequiredAndKeepsDesiredOn() {
        DpisConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        gateway.hasSystemScopeSelected = false;
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(true);

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertFalse(view.lastState.effectiveEnabled);
        assertEquals(SystemHookState.Reason.SCOPE_MISSING, view.lastState.reason);
        assertEquals(1, view.scopeRequiredCount);
        assertEquals(0, view.initRequiredCount);
        assertTrue(store.isSystemServerHooksEnabled());
    }

    @Test
    public void enable_withScopeReady_setsOnStateWithoutWarnings() {
        DpisConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        FakeView view = new FakeView();
        FakeDelivery delivery = new FakeDelivery();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view, delivery);

        controller.onUserToggle(true);

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertTrue(view.lastState.effectiveEnabled);
        assertEquals(0, view.initRequiredCount);
        assertEquals(0, view.scopeRequiredCount);
        assertTrue(store.isSystemServerHooksEnabled());
        assertEquals(1, delivery.resyncCount);
    }

    @Test
    public void syncFromStore_withDesiredOnAndScopeMissing_rendersMissingState() {
        DpisConfigStore store = createStore(true);
        FakeScopeGateway gateway = new FakeScopeGateway();
        gateway.hasSystemScopeSelected = false;
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.syncFromStore();

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertFalse(view.lastState.effectiveEnabled);
        assertEquals(SystemHookState.Reason.SCOPE_MISSING, view.lastState.reason);
        assertEquals(0, view.scopeRequiredCount);
    }

    @Test
    public void syncFromStore_whenScopeQueryThrows_treatsAsScopeMissing() {
        DpisConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        gateway.throwOnHasScopeSelected = true;
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(true);

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertFalse(view.lastState.effectiveEnabled);
        assertEquals(SystemHookState.Reason.SCOPE_MISSING, view.lastState.reason);
        assertEquals(1, view.scopeRequiredCount);
        assertTrue(store.isSystemServerHooksEnabled());
    }

    private static DpisConfigStore createStore(boolean hooksEnabled) {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        if (!hooksEnabled) {
            store.setSystemServerHooksEnabled(false);
        }
        return store;
    }

    private static final class FakeView implements SystemHooksToggleController.View {
        SystemHookState lastState;
        int initRequiredCount;
        int saveFailedCount;
        int scopeRequiredCount;

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
        public void showScopeRequired() {
            scopeRequiredCount++;
        }
    }

    private static final class FakeScopeGateway implements SystemHooksToggleController.ScopeGateway {
        boolean serviceAvailable = true;
        boolean hasSystemScopeSelected = true;
        boolean throwOnHasScopeSelected;

        @Override
        public boolean isServiceAvailable() {
            return serviceAvailable;
        }

        @Override
        public boolean hasSystemScopeSelected() {
            if (throwOnHasScopeSelected) {
                throw new RuntimeException("scope unavailable");
            }
            return hasSystemScopeSelected;
        }
    }

    private static final class FakeDelivery implements Runnable {
        int resyncCount;

        @Override
        public void run() {
            resyncCount++;
        }
    }
}
