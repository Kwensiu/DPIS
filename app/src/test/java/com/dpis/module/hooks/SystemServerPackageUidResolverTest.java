package com.dpis.module;

import com.dpis.module.runtime.systemserver.SystemServerPackageUidResolver;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class SystemServerPackageUidResolverTest {
    @Test
    public void derivesUserIdFromCallingUid() {
        assertEquals(0, SystemServerPackageUidResolver.userIdFromUid(10042));
        assertEquals(10, SystemServerPackageUidResolver.userIdFromUid(1010042));
    }

    @Test
    public void cachesPackageUidWithinTtl() {
        AtomicInteger loads = new AtomicInteger();
        AtomicLong now = new AtomicLong(100L);
        SystemServerPackageUidResolver resolver = new SystemServerPackageUidResolver(
                (packageName, userId) -> {
                    loads.incrementAndGet();
                    return 10042;
                },
                2_000L,
                now::get);

        assertEquals(10042, resolver.resolve("com.example.app", 10042));
        now.set(1_000L);
        assertEquals(10042, resolver.resolve("com.example.app", 10042));

        assertEquals(1, loads.get());
    }

    @Test
    public void refreshesPackageUidAfterTtl() {
        AtomicInteger loads = new AtomicInteger();
        AtomicLong now = new AtomicLong(100L);
        SystemServerPackageUidResolver resolver = new SystemServerPackageUidResolver(
                (packageName, userId) -> loads.incrementAndGet() == 1 ? 10042 : 10043,
                2_000L,
                now::get);

        assertEquals(10042, resolver.resolve("com.example.app", 10042));
        now.set(2_100L);
        assertEquals(10043, resolver.resolve("com.example.app", 10042));
    }

    @Test
    public void cachesMissWithinTtl() {
        AtomicInteger loads = new AtomicInteger();
        AtomicLong now = new AtomicLong(100L);
        SystemServerPackageUidResolver resolver = new SystemServerPackageUidResolver(
                (packageName, userId) -> {
                    loads.incrementAndGet();
                    return null;
                },
                2_000L,
                now::get);

        assertEquals(-1, resolver.resolve("com.example.app", 10042));
        now.set(1_000L);
        assertEquals(-1, resolver.resolve("com.example.app", 10042));

        assertEquals(1, loads.get());
    }

    @Test
    public void cacheKeyIncludesUserId() {
        AtomicInteger loads = new AtomicInteger();
        AtomicLong now = new AtomicLong(100L);
        Map<Integer, Integer> userUid = new HashMap<>();
        userUid.put(0, 10042);
        userUid.put(10, 1010042);
        SystemServerPackageUidResolver resolver = new SystemServerPackageUidResolver(
                (packageName, userId) -> {
                    loads.incrementAndGet();
                    return userUid.get(userId);
                },
                2_000L,
                now::get);

        assertEquals(10042, resolver.resolve("com.example.app", 10042));
        assertEquals(1010042, resolver.resolve("com.example.app", 1010042));

        assertEquals(2, loads.get());
    }
}
