package com.dpis.module.viewport;

import com.dpis.module.viewport.ViewportOverride;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetSpec;

public final class ViewportRuntimeRecord {
    public static final String PROVENANCE_SYSTEM_SERVER = "s";
    public static final String PROVENANCE_APP_PROCESS = "a";

    public final String packageName;
    public final ViewportTargetSpec targetSpec;
    public final String targetFingerprint;
    public final String sourceSignature;
    public final int effectiveSmallestWidthDp;
    public final ViewportOverride.Result viewportResult;
    public final VirtualDisplayOverride.Result virtualDisplayResult;
    public final String resultSignature;
    public final String provenance;
    public final long createdElapsedRealtime;
    public final String scope;

    public ViewportRuntimeRecord(String packageName,
                          ViewportTargetSpec targetSpec,
                          String sourceSignature,
                          int effectiveSmallestWidthDp,
                          ViewportOverride.Result viewportResult,
                          VirtualDisplayOverride.Result virtualDisplayResult,
                          String resultSignature,
                          String provenance,
                          long createdElapsedRealtime,
                          String scope) {
        this.packageName = packageName;
        this.targetSpec = targetSpec != null ? targetSpec : ViewportTargetSpec.off();
        this.targetFingerprint = this.targetSpec.fingerprint();
        this.sourceSignature = sourceSignature;
        this.effectiveSmallestWidthDp = effectiveSmallestWidthDp;
        this.viewportResult = viewportResult;
        this.virtualDisplayResult = virtualDisplayResult;
        this.resultSignature = resultSignature;
        this.provenance = provenance != null ? provenance : PROVENANCE_APP_PROCESS;
        this.createdElapsedRealtime = createdElapsedRealtime;
        this.scope = scope != null ? scope : ViewportSourceSnapshot.SCOPE_UNKNOWN;
    }

    boolean matchesPackageAndTarget(String packageName, ViewportTargetSpec targetSpec) {
        return this.packageName != null
                && this.packageName.equals(packageName)
                && this.targetFingerprint.equals(targetSpec != null ? targetSpec.fingerprint() : "off");
    }

    boolean displayScoped() {
        return ViewportSourceSnapshot.SCOPE_DISPLAY.equals(scope);
    }
}
