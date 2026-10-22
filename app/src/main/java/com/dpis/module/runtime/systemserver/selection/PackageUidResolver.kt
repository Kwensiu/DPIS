package com.dpis.module.runtime.systemserver.selection

import android.content.Context
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class PackageUidResolver(
    private val lookup: UidLookup?,
    suppliedTtlMillis: Long,
    suppliedClock: Clock?,
) {
    fun interface Clock {
        fun nowMillis(): Long
    }

    fun interface UidLookup {
        fun resolve(packageName: String?, userId: Int): Int?
    }

    private val ttlMillis = max(0L, suppliedTtlMillis)
    private val clock: Clock = suppliedClock ?: Clock { SystemClock.elapsedRealtime() }
    private val cache = ConcurrentHashMap<Key, Entry>()

    constructor(ttlMillis: Long) : this(ReflectionUidLookup(), ttlMillis, null)

    fun resolve(packageName: String?, callingUid: Int): Int {
        if (packageName.isNullOrBlank() || callingUid <= 0) {
            return -1
        }
        val userId = userIdFromUid(callingUid) ?: return -1
        val now = clock.nowMillis()
        val key = Key(packageName, userId)
        val cached = cache[key]
        if (cached != null && !cached.isExpired(now, ttlMillis)) {
            return cached.uidOrMiss()
        }
        val loaded = load(packageName, userId, now)
        cache[key] = loaded
        return loaded.uidOrMiss()
    }

    private fun load(packageName: String, userId: Int, now: Long): Entry {
        try {
            val uid = lookup?.resolve(packageName, userId)
            if (uid != null && uid > 0) {
                return Entry(uid, now)
            }
        } catch (_: Throwable) {
        }
        return Entry(UID_MISS, now)
    }

    private class ReflectionUidLookup : UidLookup {
        @Volatile
        private var packageManager: Any? = null

        @Volatile
        private var packageUidCall: PackageUidCall? = null

        override fun resolve(packageName: String?, userId: Int): Int? {
            val uid = resolveFromAppGlobals(packageName, userId)
            if (uid != null) {
                return uid
            }
            return if (userId == 0) resolveFromSystemContext(packageName) else null
        }

        private fun resolveFromAppGlobals(packageName: String?, userId: Int): Int? {
            try {
                val pm = resolvePackageManager() ?: return null
                val call = resolvePackageUidCall(pm.javaClass) ?: return null
                return call.invoke(pm, packageName, userId)
            } catch (_: Throwable) {
                return null
            }
        }

        @Throws(ReflectiveOperationException::class)
        private fun resolvePackageManager(): Any? {
            val local = packageManager
            if (local != null) {
                return local
            }
            val resolved = Class.forName("android.app.AppGlobals")
                .getMethod("getPackageManager")
                .invoke(null)
            if (resolved != null) {
                packageManager = resolved
            }
            return resolved
        }

        private fun resolvePackageUidCall(packageManagerClass: Class<*>): PackageUidCall? {
            val local = packageUidCall
            if (local != null) {
                return local
            }
            val resolved = PackageUidCall.resolve(packageManagerClass) ?: return null
            packageUidCall = resolved
            return resolved
        }

        private fun resolveFromSystemContext(packageName: String?): Int? {
            try {
                val activityThread = Class.forName("android.app.ActivityThread")
                    .getMethod("currentActivityThread")
                    .invoke(null) ?: return null
                val context = activityThread.javaClass
                    .getMethod("getSystemContext")
                    .invoke(activityThread)
                if (context !is Context) {
                    return null
                }
                val name = packageName ?: return null
                val uid = context.packageManager.getPackageUid(name, 0)
                return if (uid > 0) uid else null
            } catch (_: Throwable) {
                return null
            }
        }
    }

    private fun interface PackageUidCall {
        @Throws(ReflectiveOperationException::class)
        fun invoke(packageManager: Any, packageName: String?, userId: Int): Int?

        companion object {
            fun resolve(packageManagerClass: Class<*>): PackageUidCall? =
                resolveStringLongInt(packageManagerClass)
                    ?: resolveStringIntInt(packageManagerClass)

            private fun resolveStringLongInt(packageManagerClass: Class<*>): PackageUidCall? =
                try {
                    val method = packageManagerClass.getMethod(
                        "getPackageUid",
                        String::class.java,
                        java.lang.Long.TYPE,
                        Integer.TYPE,
                    )
                    PackageUidCall { packageManager, packageName, userId ->
                        asUid(method.invoke(packageManager, packageName, 0L, userId))
                    }
                } catch (_: ReflectiveOperationException) {
                    null
                }

            private fun resolveStringIntInt(packageManagerClass: Class<*>): PackageUidCall? =
                try {
                    val method = packageManagerClass.getMethod(
                        "getPackageUid",
                        String::class.java,
                        Integer.TYPE,
                        Integer.TYPE,
                    )
                    PackageUidCall { packageManager, packageName, userId ->
                        asUid(method.invoke(packageManager, packageName, 0, userId))
                    }
                } catch (_: ReflectiveOperationException) {
                    null
                }

            private fun asUid(value: Any?): Int? =
                if (value is Int && value > 0) value else null
        }
    }

    private data class Key(val packageName: String, val userId: Int)

    private class Entry(private val uid: Int, private val loadedAtMillis: Long) {
        fun isExpired(nowMillis: Long, ttlMillis: Long): Boolean =
            ttlMillis == 0L || (nowMillis - loadedAtMillis) >= ttlMillis

        fun uidOrMiss(): Int = if (uid == UID_MISS) -1 else uid
    }

    companion object {
        private const val PER_USER_RANGE = 100000
        private const val UID_MISS = Int.MIN_VALUE

        @JvmStatic
        fun userIdFromUid(uid: Int): Int? {
            if (uid < 0) {
                return null
            }
            try {
                val userHandle = android.os.UserHandle::class.java
                    .getMethod("getUserHandleForUid", Integer.TYPE)
                    .invoke(null, uid)
                if (userHandle != null) {
                    val identifier = userHandle.javaClass
                        .getMethod("getIdentifier")
                        .invoke(userHandle)
                    if (identifier is Int) {
                        return identifier
                    }
                }
            } catch (_: Throwable) {
            }
            return uid / PER_USER_RANGE
        }
    }
}
