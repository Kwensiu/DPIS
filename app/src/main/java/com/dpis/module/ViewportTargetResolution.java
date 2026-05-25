package com.dpis.module;

final class ViewportTargetResolution {
    final ViewportTargetSpec spec;
    final int effectiveSmallestWidthDp;
    final ViewportSourceSnapshot source;
    final ViewportRuntimeRecord record;
    final String reason;

    private ViewportTargetResolution(ViewportTargetSpec spec,
                                     int effectiveSmallestWidthDp,
                                     ViewportSourceSnapshot source,
                                     ViewportRuntimeRecord record,
                                     String reason) {
        this.spec = spec != null ? spec : ViewportTargetSpec.off();
        this.effectiveSmallestWidthDp = effectiveSmallestWidthDp;
        this.source = source;
        this.record = record;
        this.reason = reason;
    }

    static ViewportTargetResolution resolved(ViewportTargetSpec spec,
                                             int effectiveSmallestWidthDp,
                                             ViewportSourceSnapshot source,
                                             String reason) {
        return new ViewportTargetResolution(
                spec, Math.max(1, effectiveSmallestWidthDp), source, null, reason);
    }

    static ViewportTargetResolution fromRecord(ViewportRuntimeRecord record, String reason) {
        if (record == null) {
            return none(reason);
        }
        return new ViewportTargetResolution(
                record.targetSpec,
                record.effectiveSmallestWidthDp,
                null,
                record,
                reason);
    }

    static ViewportTargetResolution none(String reason) {
        return new ViewportTargetResolution(ViewportTargetSpec.off(), 0, null, null, reason);
    }

    boolean hasTarget() {
        return effectiveSmallestWidthDp > 0 && spec.isEnabled();
    }
}
