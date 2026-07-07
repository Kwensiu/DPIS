package com.dpis.module.diagnostics;

public final class DpisLogEntry {
    public final long timestampMillis;
    public final String timestamp;
    public final String level;
    public final String source;
    public final String process;
    public final String modulePackage;
    public final String tag;
    public final String message;
    public final boolean external;

    public DpisLogEntry(String timestamp,
            String level,
            String process,
            String modulePackage,
            String tag,
            String message,
            boolean external) {
        this(0L, timestamp, level, external ? "LSPosed" : "DPIS",
                process, modulePackage, tag, message, external);
    }

    public DpisLogEntry(long timestampMillis,
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
