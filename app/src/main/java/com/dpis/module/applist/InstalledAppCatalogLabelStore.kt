package com.dpis.module.applist

import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Disk cache of resolved app labels. A hit requires the same locale and the same
 * package last-update time; timestamps themselves still come from PackageManager.
 */
class InstalledAppCatalogLabelStore(private val file: File) {
    private val lock = Any()
    private var memory: CatalogLabelCacheSnapshot? = null

    fun load(): CatalogLabelCacheSnapshot = synchronized(lock) {
        memory?.let { return it }
        val snapshot = decode(readFile()) ?: CatalogLabelCacheSnapshot.EMPTY
        memory = snapshot
        snapshot
    }

    fun replace(localeTag: String, records: Map<String, CatalogLabelRecord>) {
        val snapshot = CatalogLabelCacheSnapshot(localeTag, records)
        synchronized(lock) {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(encode(snapshot), StandardCharsets.UTF_8)
            if (file.exists() && !file.delete()) {
                file.writeText(encode(snapshot), StandardCharsets.UTF_8)
                tmp.delete()
            } else if (!tmp.renameTo(file)) {
                file.writeText(encode(snapshot), StandardCharsets.UTF_8)
                tmp.delete()
            }
            memory = snapshot
        }
    }

    private fun readFile(): String? {
        if (!file.isFile) return null
        return try {
            file.readText(StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val FILE_NAME = "installed-app-catalog-labels.json"

        private const val KEY_LOCALE = "locale"
        private const val KEY_RECORDS = "records"
        private const val KEY_LABEL = "label"
        private const val KEY_LAST_UPDATE_TIME = "lastUpdateTime"

        @Volatile
        private var shared: InstalledAppCatalogLabelStore? = null
        private val sharedLock = Any()

        @JvmStatic
        fun shared(file: File): InstalledAppCatalogLabelStore {
            shared?.let { return it }
            synchronized(sharedLock) {
                shared?.let { return it }
                val created = InstalledAppCatalogLabelStore(file)
                shared = created
                return created
            }
        }

        @JvmStatic
        fun encode(snapshot: CatalogLabelCacheSnapshot): String {
            val recordsJson = JSONObject()
            snapshot.records.forEach { (packageName, record) ->
                recordsJson.put(
                    packageName,
                    JSONObject()
                        .put(KEY_LABEL, record.label)
                        .put(KEY_LAST_UPDATE_TIME, record.lastUpdateTime),
                )
            }
            return JSONObject()
                .put(KEY_LOCALE, snapshot.localeTag)
                .put(KEY_RECORDS, recordsJson)
                .toString()
        }

        @JvmStatic
        fun decode(raw: String?): CatalogLabelCacheSnapshot? {
            if (raw.isNullOrBlank()) return null
            return try {
                val root = JSONObject(raw)
                val localeTag = root.optString(KEY_LOCALE, "")
                val recordsJson = root.optJSONObject(KEY_RECORDS) ?: return CatalogLabelCacheSnapshot(
                    localeTag,
                    emptyMap(),
                )
                val records = LinkedHashMap<String, CatalogLabelRecord>()
                val names = recordsJson.keys()
                while (names.hasNext()) {
                    val packageName = names.next()
                    val entry = recordsJson.optJSONObject(packageName) ?: continue
                    val label = entry.optString(KEY_LABEL, "").trim()
                    if (packageName.isBlank() || label.isEmpty()) continue
                    records[packageName] = CatalogLabelRecord(
                        label,
                        entry.optLong(KEY_LAST_UPDATE_TIME, 0L),
                    )
                }
                CatalogLabelCacheSnapshot(localeTag, records)
            } catch (_: JSONException) {
                null
            }
        }
    }
}

data class CatalogLabelRecord(
    val label: String,
    val lastUpdateTime: Long,
)

data class CatalogLabelCacheSnapshot(
    val localeTag: String,
    val records: Map<String, CatalogLabelRecord>,
) {
    fun resolvedLabel(
        localeTag: String,
        packageName: String,
        lastUpdateTime: Long,
    ): String? {
        if (localeTag.isBlank() || localeTag != this.localeTag) return null
        val record = records[packageName] ?: return null
        if (record.lastUpdateTime != lastUpdateTime) return null
        val label = record.label.trim()
        return label.takeIf { it.isNotEmpty() }
    }

    companion object {
        val EMPTY = CatalogLabelCacheSnapshot("", emptyMap())
    }
}
