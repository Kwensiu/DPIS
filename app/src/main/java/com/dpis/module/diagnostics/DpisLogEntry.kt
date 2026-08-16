package com.dpis.module.diagnostics

/** Immutable normalized log entry shared by the parser, exporter, and log page. */
class DpisLogEntry private constructor(
    @JvmField val timestampMillis: Long,
    @JvmField val timestamp: String,
    @JvmField val level: String,
    @JvmField val source: String,
    @JvmField val process: String,
    @JvmField val modulePackage: String,
    @JvmField val tag: String,
    @JvmField val message: String,
    @JvmField val external: Boolean,
    @Suppress("UNUSED_PARAMETER") marker: Unit,
) {
    constructor(
        timestamp: String?,
        level: String?,
        process: String?,
        modulePackage: String?,
        tag: String?,
        message: String?,
        external: Boolean,
    ) : this(
        timestampMillis = 0L,
        timestamp = timestamp.orEmpty(),
        level = level.orEmpty(),
        source = if (external) "LSPosed" else "DPIS",
        process = process.orEmpty(),
        modulePackage = modulePackage.orEmpty(),
        tag = tag.orEmpty(),
        message = message.orEmpty(),
        external = external,
        marker = Unit,
    )

    constructor(
        timestampMillis: Long,
        timestamp: String?,
        level: String?,
        source: String?,
        process: String?,
        modulePackage: String?,
        tag: String?,
        message: String?,
        external: Boolean,
    ) : this(
        timestampMillis = timestampMillis,
        timestamp = timestamp.orEmpty(),
        level = level.orEmpty(),
        source = source.orEmpty(),
        process = process.orEmpty(),
        modulePackage = modulePackage.orEmpty(),
        tag = tag.orEmpty(),
        message = message.orEmpty(),
        external = external,
        marker = Unit,
    )
}
