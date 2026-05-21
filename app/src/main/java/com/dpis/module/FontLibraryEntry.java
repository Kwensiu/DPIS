package com.dpis.module;

import java.util.Objects;

final class FontLibraryEntry {
    final String id;
    final String displayName;
    final String sourceFileName;
    final String storedFileName;
    final String storedPath;
    final String sha256;
    final long importedAtEpochMs;

    FontLibraryEntry(String id,
                     String displayName,
                     String sourceFileName,
                     String storedFileName,
                     String storedPath,
                     String sha256,
                     long importedAtEpochMs) {
        this.id = id;
        this.displayName = displayName;
        this.sourceFileName = sourceFileName;
        this.storedFileName = storedFileName;
        this.storedPath = storedPath;
        this.sha256 = sha256;
        this.importedAtEpochMs = importedAtEpochMs;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof FontLibraryEntry other)) {
            return false;
        }
        return importedAtEpochMs == other.importedAtEpochMs
                && Objects.equals(id, other.id)
                && Objects.equals(displayName, other.displayName)
                && Objects.equals(sourceFileName, other.sourceFileName)
                && Objects.equals(storedFileName, other.storedFileName)
                && Objects.equals(storedPath, other.storedPath)
                && Objects.equals(sha256, other.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                displayName,
                sourceFileName,
                storedFileName,
                storedPath,
                sha256,
                importedAtEpochMs);
    }
}
