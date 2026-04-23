package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void disable_setsStoreFalse_withoutScopeMutation() {
        DpiConfigStore store = createStore(true);
        FakeScopeGateway gateway = new FakeScopeGateway();
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(false);

        assertFalse(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertFalse(store.isSystemServerHooksEnabled());
        assertEquals(0, view.scopeRequiredCount);
        assertEquals(0, view.initRequiredCount);
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
        assertEquals(0, view.scopeRequiredCount);
        assertTrue(store.isSystemServerHooksEnabled());
    }

    @Test
    public void enable_withScopeMissing_showsScopeRequiredAndKeepsDesiredOn() {
        DpiConfigStore store = createStore(false);
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
        DpiConfigStore store = createStore(false);
        FakeScopeGateway gateway = new FakeScopeGateway();
        FakeView view = new FakeView();
        SystemHooksToggleController controller = new SystemHooksToggleController(
                store, gateway, view);

        controller.onUserToggle(true);

        assertTrue(view.lastState.switchChecked);
        assertTrue(view.lastState.switchEnabled);
        assertTrue(view.lastState.desiredEnabled);
        assertTrue(view.lastState.effectiveEnabled);
        assertEquals(0, view.initRequiredCount);
        assertEquals(0, view.scopeRequiredCount);
        assertTrue(store.isSystemServerHooksEnabled());
    }

    @Test
    public void syncFromStore_withDesiredOnAndScopeMissing_rendersMissingState() {
        DpiConfigStore store = createStore(true);
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
        DpiConfigStore store = createStore(false);
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
}
