package com.dpis.module.fonts;

import java.util.Objects;

public final class SystemFontEntry {
    private final String id;
    private final String displayName;

    SystemFontEntry(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
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
