package com.dpis.module.runtime.systemserver;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ReflectionProbeCache {
    private final ConcurrentMap<Class<?>, List<Field>> allFieldsCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<FieldKey, Optional<Field>> fieldCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodKey, Optional<Method>> noArgMethodCache = new ConcurrentHashMap<>();

    public List<Field> getAllFields(Class<?> type) {
        if (type == null) {
            return Collections.emptyList();
        }
        return allFieldsCache.computeIfAbsent(type, ReflectionProbeCache::collectAllFields);
    }

    public Field findField(Class<?> type, String fieldName) {
        if (type == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        return fieldCache.computeIfAbsent(new FieldKey(type, fieldName),
                ReflectionProbeCache::resolveField).orElse(null);
    }

    public Method findNoArgMethod(Class<?> type, String methodName) {
        if (type == null || methodName == null || methodName.isEmpty()) {
            return null;
        }
        return noArgMethodCache.computeIfAbsent(new MethodKey(type, methodName),
                ReflectionProbeCache::resolveNoArgMethod).orElse(null);
    }

    private static List<Field> collectAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null) {
            Field[] declared = current.getDeclaredFields();
            Collections.addAll(fields, declared);
            current = current.getSuperclass();
        }
        return Collections.unmodifiableList(fields);
    }

    private static Optional<Field> resolveField(FieldKey key) {
        Class<?> current = key.type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(key.name);
                return Optional.of(field);
            } catch (ReflectiveOperationException ignored) {
                current = current.getSuperclass();
            }
        }
        return Optional.empty();
    }

    private static Optional<Method> resolveNoArgMethod(MethodKey key) {
        try {
            Method method = key.type.getMethod(key.name);
            return Optional.of(method);
        } catch (ReflectiveOperationException ignored) {
            return Optional.empty();
        }
    }

    private static final class FieldKey {
        private final Class<?> type;
        private final String name;

        private FieldKey(Class<?> type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof FieldKey other)) {
                return false;
            }
            return type.equals(other.type) && name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name);
        }
    }

    private static final class MethodKey {
        private final Class<?> type;
        private final String name;

        private MethodKey(Class<?> type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MethodKey other)) {
                return false;
            }
            return type.equals(other.type) && name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name);
        }
    }
}
