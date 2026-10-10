package com.dpis.module

import com.dpis.module.runtime.systemserver.SystemServerPackageUidResolver
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class SystemServerPackageUidResolverTest {
    @Test
    fun derivesUserIdFromCallingUid() {
        assertEquals(0, SystemServerPackageUidResolver.userIdFromUid(10042))
        assertEquals(10, SystemServerPackageUidResolver.userIdFromUid(1010042))
    }

    @Test
    fun cachesPackageUidWithinTtl() {
        val loads = AtomicInteger()
        val now = AtomicLong(100L)
        val resolver = SystemServerPackageUidResolver(
            { _, _ -> loads.incrementAndGet(); 10042 },
            2_000L,
            now::get,
        )

        assertEquals(10042, resolver.resolve("com.example.app", 10042))
        now.set(1_000L)
        assertEquals(10042, resolver.resolve("com.example.app", 10042))
        assertEquals(1, loads.get())
    }

    @Test
    fun refreshesPackageUidAfterTtl() {
        val loads = AtomicInteger()
        val now = AtomicLong(100L)
        val resolver = SystemServerPackageUidResolver(
            { _, _ -> if (loads.incrementAndGet() == 1) 10042 else 10043 },
            2_000L,
            now::get,
        )

        assertEquals(10042, resolver.resolve("com.example.app", 10042))
        now.set(2_100L)
        assertEquals(10043, resolver.resolve("com.example.app", 10042))
    }

    @Test
    fun cachesMissWithinTtl() {
        val loads = AtomicInteger()
        val now = AtomicLong(100L)
        val resolver = SystemServerPackageUidResolver(
            { _, _ -> loads.incrementAndGet(); null },
            2_000L,
            now::get,
        )

        assertEquals(-1, resolver.resolve("com.example.app", 10042))
        now.set(1_000L)
        assertEquals(-1, resolver.resolve("com.example.app", 10042))
        assertEquals(1, loads.get())
    }

    @Test
    fun cacheKeyIncludesUserId() {
        val loads = AtomicInteger()
        val now = AtomicLong(100L)
        val userUid = mapOf(0 to 10042, 10 to 1010042)
        val resolver = SystemServerPackageUidResolver(
            { _, userId -> loads.incrementAndGet(); userUid[userId] },
            2_000L,
            now::get,
        )

        assertEquals(10042, resolver.resolve("com.example.app", 10042))
        assertEquals(1010042, resolver.resolve("com.example.app", 1010042))
        assertEquals(2, loads.get())
    }
}
