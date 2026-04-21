package com.dpis.module;

final class SystemHookStateResolver {
    private SystemHookStateResolver() {
    }

    static SystemHookState resolve(boolean desiredEnabled,
                                   boolean requestPending,
                                   boolean serviceAvailable,
                                   boolean scopeSelected) {
        if (!desiredEnabled) {
            return new SystemHookState(
                    false,
                    false,
                    false,
                    true,
                    SystemHookState.Reason.DISABLED_BY_USER);
        }
        if (requestPending) {
            return new SystemHookState(
                    true,
                    false,
                    true,
                    false,
                    SystemHookState.Reason.REQUEST_PENDING);
        }
        if (!serviceAvailable) {
            return new SystemHookState(
                    true,
                    false,
                    true,
                    true,
                    SystemHookState.Reason.SERVICE_UNAVAILABLE);
        }
        if (!scopeSelected) {
            return new SystemHookState(
                    true,
                    false,
                    true,
                    true,
                    SystemHookState.Reason.SCOPE_MISSING);
        }
        return new SystemHookState(
                true,
                true,
                true,
                true,
                SystemHookState.Reason.NONE);
    }
}
