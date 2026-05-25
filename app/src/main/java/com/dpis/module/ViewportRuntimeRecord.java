package com.dpis.module;

final class ViewportRuntimeRecord {
    static final String PROVENANCE_SYSTEM_SERVER = "s";
    static final String PROVENANCE_APP_PROCESS = "a";

    final String packageName;
    final ViewportTargetSpec targetSpec;
    final String targetFingerprint;
    final String sourceSignature;
    final int effectiveSmallestWidthDp;
    final ViewportOverride.Result viewportResult;
    final VirtualDisplayOverride.Result virtualDisplayResult;
    final String resultSignature;
    final String provenance;
    final long createdElapsedRealtime;
    final String scope;

    ViewportRuntimeRecord(String packageName,
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
