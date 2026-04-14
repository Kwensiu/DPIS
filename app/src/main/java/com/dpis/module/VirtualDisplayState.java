package com.dpis.module;

final class VirtualDisplayState {
    private static volatile VirtualDisplayOverride.Result current;

    private VirtualDisplayState() {
    }

    static void set(VirtualDisplayOverride.Result result) {
        current = result;
    }

    static VirtualDisplayOverride.Result get() {
        return current;
    }
}
