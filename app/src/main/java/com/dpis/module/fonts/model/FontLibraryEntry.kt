package com.dpis.module.fonts

import java.util.Objects
import kotlin.math.max

class FontLibraryEntry @JvmOverloads internal constructor(
    @JvmField val id: String?,
    @JvmField val displayName: String?,
    @JvmField val sourceFileName: String?,
    @JvmField val storedFileName: String?,
    @JvmField val storedPath: String?,
    @JvmField val sha256: String?,
    @JvmField val importedAtEpochMs: Long,
    ttcIndex: Int = 0,
    collectionId: String? = collectionIdFromLegacyId(id),
    collectionDisplayName: String? = displayName,
    publicationStatus: FontPublicationStatus? = FontPublicationStatus.PRIVATE
) {
    @JvmField
    val ttcIndex: Int
    @JvmField
    val collectionId: String?
    @JvmField
    val collectionDisplayName: String?
    @JvmField
    val publicationStatus: FontPublicationStatus

    internal constructor(
        id: String?,
        displayName: String?,
        sourceFileName: String?,
        storedFileName: String?,
        storedPath: String?,
        sha256: String?,
        importedAtEpochMs: Long,
        ttcIndex: Int,
        collectionId: String?,
        publicationStatus: FontPublicationStatus
    ) : this(
        id, displayName, sourceFileName, storedFileName, storedPath, sha256, importedAtEpochMs,
        ttcIndex, collectionId, displayName, publicationStatus
    )

    init {
        this.ttcIndex = max(0, ttcIndex)
        val face = FontFace.fromLegacyId(id)
        this.collectionId = if (collectionId != null && !collectionId.isBlank())
            collectionId
        else
            if (face != null) face.collectionId else ""
        this.collectionDisplayName =
            if (collectionDisplayName != null && !collectionDisplayName.isBlank())
                collectionDisplayName
            else
                displayName
        this.publicationStatus = if (publicationStatus != null)
            publicationStatus
        else
            FontPublicationStatus.PRIVATE
    }

    override fun equals(`object`: Any?): Boolean {
        if (this === `object`) {
            return true
        }
        if (`object` !is FontLibraryEntry) {
            return false
        }
        return importedAtEpochMs == `object`.importedAtEpochMs && ttcIndex == `object`.ttcIndex && id == `object`.id
                && displayName == `object`.displayName
                && sourceFileName == `object`.sourceFileName
                && storedFileName == `object`.storedFileName
                && storedPath == `object`.storedPath
                && sha256 == `object`.sha256
                && collectionId == `object`.collectionId
                && collectionDisplayName == `object`.collectionDisplayName
                && publicationStatus == `object`.publicationStatus
    }

    override fun hashCode(): Int {
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
            publicationStatus
        )
    }

    companion object {
        private fun collectionIdFromLegacyId(id: String?): String? {
            val face = FontFace.fromLegacyId(id)
            return if (face != null) face.collectionId else ""
        }
    }
}
