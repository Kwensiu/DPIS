package com.dpis.module.fonts;

import java.util.Set;

public final class FontLibraryConfigStore {
    public interface Delegate {
        boolean clearTargetTypefaceId(String packageName);

        Set<String> getConfiguredPackages();

        String getTargetTypefaceId(String packageName);

        boolean setTargetTypefaceId(String packageName, String typefaceId);
    }

    private final Delegate delegate;

    public FontLibraryConfigStore(Delegate delegate) {
        this.delegate = delegate;
    }

    public boolean clearTargetTypefaceId(String packageName) {
        return delegate != null && delegate.clearTargetTypefaceId(packageName);
    }

    public Set<String> getConfiguredPackages() {
        return delegate != null ? delegate.getConfiguredPackages() : Set.of();
    }

    public String getTargetTypefaceId(String packageName) {
        return delegate != null ? delegate.getTargetTypefaceId(packageName) : null;
    }

    public boolean setTargetTypefaceId(String packageName, String typefaceId) {
        return delegate != null && delegate.setTargetTypefaceId(packageName, typefaceId);
    }
}
