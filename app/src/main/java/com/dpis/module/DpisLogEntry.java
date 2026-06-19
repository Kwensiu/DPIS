package com.dpis.module;

final class DpisLogEntry {
    final long timestampMillis;
    final String timestamp;
    final String level;
    final String source;
    final String process;
    final String modulePackage;
    final String tag;
    final String message;
    final boolean external;

    DpisLogEntry(String timestamp,
            String level,
            String process,
            String modulePackage,
            String tag,
            String message,
            boolean external) {
        this(0L, timestamp, level, external ? "LSPosed" : "DPIS",
                process, modulePackage, tag, message, external);
    }

    DpisLogEntry(long timestampMillis,
            String timestamp,
            String level,
            String source,
            String process,
            String modulePackage,
            String tag,
            String message,
            boolean external) {
        this.timestampMillis = timestampMillis;
        this.timestamp = timestamp != null ? timestamp : "";
        this.level = level != null ? level : "";
        this.source = source != null ? source : "";
        this.process = process != null ? process : "";
        this.modulePackage = modulePackage != null ? modulePackage : "";
        this.tag = tag != null ? tag : "";
        this.message = message != null ? message : "";
        this.external = external;
    }
}
