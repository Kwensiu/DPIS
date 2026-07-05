package com.dpis.module.runtime.systemserver;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SystemServerPackageUidResolver {
    private static final int PER_USER_RANGE = 100000;
    private static final int UID_MISS = Integer.MIN_VALUE;

    public interface Clock {
        long nowMillis();
    }

    public interface UidLookup {
        Integer resolve(String packageName, int userId);
    }

    private final UidLookup lookup;
    private final long ttlMillis;
    private final Clock clock;
    private final ConcurrentMap<Key, Entry> cache = new ConcurrentHashMap<>();

    public SystemServerPackageUidResolver(long ttlMillis) {
        this(new ReflectionUidLookup(), ttlMillis, android.os.SystemClock::elapsedRealtime);
    }

    public SystemServerPackageUidResolver(UidLookup lookup, long ttlMillis, Clock clock) {
        this.lookup = lookup;
        this.ttlMillis = Math.max(0L, ttlMillis);
        this.clock = clock != null ? clock : android.os.SystemClock::elapsedRealtime;
    }

    public int resolve(String packageName, int callingUid) {
        if (packageName == null || packageName.isBlank() || callingUid <= 0) {
            return -1;
        }
        int userId = userIdFromUid(callingUid);
        long now = clock.nowMillis();
        Key key = new Key(packageName, userId);
        Entry cached = cache.get(key);
        if (cached != null && !cached.isExpired(now, ttlMillis)) {
            return cached.uidOrMiss();
        }
        Entry loaded = load(packageName, userId, now);
        cache.put(key, loaded);
        return loaded.uidOrMiss();
    }

    public static int userIdFromUid(int uid) {
        if (uid <= 0) {
            return 0;
        }
        try {
            Object userHandle = android.os.UserHandle.class
                    .getMethod("getUserHandleForUid", int.class)
                    .invoke(null, uid);
            if (userHandle != null) {
                Object identifier = userHandle.getClass()
                        .getMethod("getIdentifier")
                        .invoke(userHandle);
                if (identifier instanceof Integer userId) {
                    return userId;
                }
            }
        } catch (Throwable ignored) {
        }
        return uid / PER_USER_RANGE;
    }

    private Entry load(String packageName, int userId, long now) {
        try {
            Integer uid = lookup != null ? lookup.resolve(packageName, userId) : null;
            if (uid != null && uid > 0) {
                return new Entry(uid, now);
            }
        } catch (Throwable ignored) {
        }
        return new Entry(UID_MISS, now);
    }

    private static final class ReflectionUidLookup implements UidLookup {
        private volatile Object packageManager;
        private volatile PackageUidCall packageUidCall;

        @Override
        public Integer resolve(String packageName, int userId) {
            Integer uid = resolveFromAppGlobals(packageName, userId);
            if (uid != null) {
                return uid;
            }
            return userId == 0 ? resolveFromSystemContext(packageName) : null;
        }

        private Integer resolveFromAppGlobals(String packageName, int userId) {
            try {
                Object pm = resolvePackageManager();
                if (pm == null) {
                    return null;
                }
                PackageUidCall call = resolvePackageUidCall(pm.getClass());
                return call != null ? call.invoke(pm, packageName, userId) : null;
            } catch (Throwable ignored) {
                return null;
            }
        }

        private Object resolvePackageManager() throws ReflectiveOperationException {
            Object local = packageManager;
            if (local != null) {
                return local;
            }
            Object resolved = Class.forName("android.app.AppGlobals")
                    .getMethod("getPackageManager")
                    .invoke(null);
            if (resolved != null) {
                packageManager = resolved;
            }
            return resolved;
        }

        private PackageUidCall resolvePackageUidCall(Class<?> packageManagerClass) {
            PackageUidCall local = packageUidCall;
            if (local != null) {
                return local;
            }
            PackageUidCall resolved = PackageUidCall.resolve(packageManagerClass);
            if (resolved != null) {
                packageUidCall = resolved;
            }
            return resolved;
        }

        private Integer resolveFromSystemContext(String packageName) {
            try {
                Object activityThread = Class.forName("android.app.ActivityThread")
                        .getMethod("currentActivityThread")
                        .invoke(null);
                if (activityThread == null) {
                    return null;
                }
                Object context = activityThread.getClass()
                        .getMethod("getSystemContext")
                        .invoke(activityThread);
                if (!(context instanceof android.content.Context androidContext)) {
                    return null;
                }
                int uid = androidContext.getPackageManager().getPackageUid(packageName, 0);
                return uid > 0 ? uid : null;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private interface PackageUidCall {
        Integer invoke(Object packageManager, String packageName, int userId) throws ReflectiveOperationException;

        static PackageUidCall resolve(Class<?> packageManagerClass) {
            PackageUidCall call = resolveStringLongInt(packageManagerClass);
            if (call != null) {
                return call;
            }
            call = resolveStringIntInt(packageManagerClass);
            if (call != null) {
                return call;
            }
            return null;
        }

        private static PackageUidCall resolveStringLongInt(Class<?> packageManagerClass) {
            try {
                Method method = packageManagerClass.getMethod(
                        "getPackageUid", String.class, long.class, int.class);
                return (packageManager, packageName, userId) ->
                        asUid(method.invoke(packageManager, packageName, 0L, userId));
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        private static PackageUidCall resolveStringIntInt(Class<?> packageManagerClass) {
            try {
                Method method = packageManagerClass.getMethod(
                        "getPackageUid", String.class, int.class, int.class);
                return (packageManager, packageName, userId) ->
                        asUid(method.invoke(packageManager, packageName, 0, userId));
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        private static Integer asUid(Object value) {
            return value instanceof Integer uid && uid > 0 ? uid : null;
        }
    }

    private static final class Key {
        private final String packageName;
        private final int userId;

        private Key(String packageName, int userId) {
            this.packageName = packageName;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Key other)) {
                return false;
            }
            return userId == other.userId && packageName.equals(other.packageName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packageName, userId);
        }
    }

    private static final class Entry {
        private final int uid;
        private final long loadedAtMillis;

        private Entry(int uid, long loadedAtMillis) {
            this.uid = uid;
            this.loadedAtMillis = loadedAtMillis;
        }

        private boolean isExpired(long nowMillis, long ttlMillis) {
            return ttlMillis == 0L || (nowMillis - loadedAtMillis) >= ttlMillis;
        }

        private int uidOrMiss() {
            return uid == UID_MISS ? -1 : uid;
        }
    }
}
