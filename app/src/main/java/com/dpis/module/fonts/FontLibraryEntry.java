package com.dpis.module.fonts;

import java.util.Objects;

public final class FontLibraryEntry {
    public final String id;
    public final String displayName;
    public final String sourceFileName;
    public final String storedFileName;
    public final String storedPath;
    public final String sha256;
    public final long importedAtEpochMs;
    public final int ttcIndex;
    public final String collectionId;
    public final String collectionDisplayName;
    public final FontPublicationStatus publicationStatus;

    private static String collectionIdFromLegacyId(String id) {
        FontFace face = FontFace.fromLegacyId(id);
        return face != null ? face.collectionId : "";
    }

    FontLibraryEntry(String id,
                     String displayName,
                     String sourceFileName,
                     String storedFileName,
                     String storedPath,
                     String sha256,
                     long importedAtEpochMs) {
        this(id, displayName, sourceFileName, storedFileName, storedPath, sha256, importedAtEpochMs, 0);
    }

    FontLibraryEntry(String id,
                     String displayName,
                     String sourceFileName,
                     String storedFileName,
                     String storedPath,
                     String sha256,
                     long importedAtEpochMs,
                     int ttcIndex) {
        this(id, displayName, sourceFileName, storedFileName, storedPath, sha256, importedAtEpochMs,
                ttcIndex, collectionIdFromLegacyId(id), displayName,
                FontPublicationStatus.PRIVATE);
    }

    FontLibraryEntry(String id,
                     String displayName,
                     String sourceFileName,
                     String storedFileName,
                     String storedPath,
                     String sha256,
                     long importedAtEpochMs,
                     int ttcIndex,
                     String collectionId,
                     FontPublicationStatus publicationStatus) {
        this(id, displayName, sourceFileName, storedFileName, storedPath, sha256, importedAtEpochMs,
                ttcIndex, collectionId, displayName, publicationStatus);
    }

    FontLibraryEntry(String id,
                     String displayName,
                     String sourceFileName,
                     String storedFileName,
                     String storedPath,
                     String sha256,
                     long importedAtEpochMs,
                     int ttcIndex,
                     String collectionId,
                     String collectionDisplayName,
                     FontPublicationStatus publicationStatus) {
        this.id = id;
        this.displayName = displayName;
        this.sourceFileName = sourceFileName;
        this.storedFileName = storedFileName;
        this.storedPath = storedPath;
        this.sha256 = sha256;
        this.importedAtEpochMs = importedAtEpochMs;
        this.ttcIndex = Math.max(0, ttcIndex);
        FontFace face = FontFace.fromLegacyId(id);
        this.collectionId = collectionId != null && !collectionId.isBlank()
                ? collectionId
                : face != null ? face.collectionId : "";
        this.collectionDisplayName = collectionDisplayName != null && !collectionDisplayName.isBlank()
                ? collectionDisplayName
                : displayName;
        this.publicationStatus = publicationStatus != null
                ? publicationStatus
                : FontPublicationStatus.PRIVATE;
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
                && ttcIndex == other.ttcIndex
                && Objects.equals(id, other.id)
                && Objects.equals(displayName, other.displayName)
                && Objects.equals(sourceFileName, other.sourceFileName)
                && Objects.equals(storedFileName, other.storedFileName)
                && Objects.equals(storedPath, other.storedPath)
                && Objects.equals(sha256, other.sha256)
                && Objects.equals(collectionId, other.collectionId)
                && Objects.equals(collectionDisplayName, other.collectionDisplayName)
                && publicationStatus == other.publicationStatus;
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
                importedAtEpochMs,
                ttcIndex,
                collectionId,
                collectionDisplayName,
                publicationStatus);
    }
}
