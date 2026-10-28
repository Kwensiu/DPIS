package com.dpis.module.fonts

import com.dpis.module.config.PackageIdentity
import java.util.Locale

/** Wire names shared by Java/Kotlin publishers and native font consumers. */
object FontPropertyKeys {
    fun name(packageName: String, userId: Int, field: String, persistent: Boolean = false): String {
        require(PackageIdentity.isValidPackageName(packageName)) { "packageName is invalid" }
        require(userId >= 0) { "userId must not be negative" }
        val prefix = if (persistent) "persist.debug.dpis." else "debug.dpis."
        return "$prefix$field.$userId.${String.format(Locale.US, "%08x", packageName.hashCode())}"
    }
}
