package com.dpis.module.settings;

public final class ExperimentalSettingsStore {
    public interface Delegate {
        boolean isTtcFontImportEnabled();

        boolean setTtcFontImportEnabled(boolean enabled);
    }

    private final Delegate delegate;

    public ExperimentalSettingsStore(Delegate delegate) {
        this.delegate = delegate;
    }

    public boolean isTtcFontImportEnabled() {
        return delegate != null && delegate.isTtcFontImportEnabled();
    }

    public boolean setTtcFontImportEnabled(boolean enabled) {
        return delegate != null && delegate.setTtcFontImportEnabled(enabled);
    }
}
