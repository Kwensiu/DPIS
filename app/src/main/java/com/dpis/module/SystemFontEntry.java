package com.dpis.module;

import java.util.Objects;

final class SystemFontEntry {
    final String id;
    final String displayName;

    SystemFontEntry(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SystemFontEntry other)) {
            return false;
        }
        return Objects.equals(id, other.id)
                && Objects.equals(displayName, other.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName);
    }
}
