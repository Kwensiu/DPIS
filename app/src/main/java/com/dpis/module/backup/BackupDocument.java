package com.dpis.module.backup;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Typed boundary for decoded backup data; legacy preference keys stay below this layer. */
public final class BackupDocument {
    public final BackupMetadata metadata;
    public final Map<String, Object> entries;

    public BackupDocument(BackupMetadata metadata, Map<String, Object> entries) {
        this.metadata = metadata;
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }
}
