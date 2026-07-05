package com.dpis.module.viewport;

import com.dpis.module.viewport.ViewportRuntimeRecord;

public final class ViewportTargetResolution {
    public static final String REASON_APP_PROCESS_BORROW_TARGET = "app-process-borrow-target";
    public static final String REASON_APP_PROCESS_RELATIVE_SCALE = "app-process-relative-scale";

    public final ViewportTargetSpec spec;
    public final int effectiveSmallestWidthDp;
    public final ViewportSourceSnapshot source;
    public final ViewportRuntimeRecord record;
    public final String reason;

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

    public static ViewportTargetResolution resolved(ViewportTargetSpec spec,
                                             int effectiveSmallestWidthDp,
                                             ViewportSourceSnapshot source,
                                             String reason) {
        return new ViewportTargetResolution(
                spec, Math.max(1, effectiveSmallestWidthDp), source, null, reason);
    }

    public static ViewportTargetResolution fromRecord(ViewportRuntimeRecord record, String reason) {
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

    public static ViewportTargetResolution fromAppProcessBorrowRecord(ViewportRuntimeRecord record) {
        return fromRecord(record, REASON_APP_PROCESS_BORROW_TARGET);
    }

    public static ViewportTargetResolution none(String reason) {
        return new ViewportTargetResolution(ViewportTargetSpec.off(), 0, null, null, reason);
    }

    public boolean hasTarget() {
        return effectiveSmallestWidthDp > 0 && spec.isEnabled();
    }

    public boolean isAppProcessBorrowTarget() {
        return spec.isRelativeScale()
                && (REASON_APP_PROCESS_BORROW_TARGET.equals(reason)
                || REASON_APP_PROCESS_RELATIVE_SCALE.equals(reason));
    }

    public boolean isAppProcessDisplayBorrowTarget() {
        return spec.isRelativeScale()
                && REASON_APP_PROCESS_BORROW_TARGET.equals(reason);
    }
}
