package com.dpis.module;

import android.graphics.drawable.Drawable;

final class AppListItem {
    final String label;
    final String packageName;
    final boolean inScope;
    final boolean scopeKnown;
    final ViewportTargetSpec viewportTargetSpec;
    final Integer viewportWidthDp;
    final Integer viewportScalePermille;
    final String viewportMode;
    final Integer fontScalePercent;
    final String fontMode;
    final String typefaceId;
    final boolean appSpecificConfigActive;
    final boolean dpisEnabled;
    final boolean systemApp;
    final boolean hyperOsNativeProxyCandidate;
    final Drawable icon;

    AppListItem(String label,
                String packageName,
                boolean inScope,
                boolean scopeKnown,
                Integer viewportWidthDp,
                String viewportMode,
                Integer fontScalePercent,
                String fontMode,
                String typefaceId,
                boolean dpisEnabled,
                boolean systemApp,
                boolean hyperOsNativeProxyCandidate,
                Drawable icon) {
        this(label, packageName, inScope, scopeKnown, viewportWidthDp, viewportMode,
                viewportWidthDp != null
                        ? ViewportTargetSpec.absoluteDp(viewportWidthDp)
                        : ViewportTargetSpec.off(),
                fontScalePercent, fontMode, typefaceId, false, dpisEnabled, systemApp,
                hyperOsNativeProxyCandidate, icon);
    }

    AppListItem(String label,
                String packageName,
                boolean inScope,
                boolean scopeKnown,
                Integer viewportWidthDp,
                String viewportMode,
                Integer fontScalePercent,
                String fontMode,
                boolean dpisEnabled,
                boolean systemApp,
                boolean hyperOsNativeProxyCandidate,
                Drawable icon) {
        this(label, packageName, inScope, scopeKnown, viewportWidthDp, viewportMode,
                viewportWidthDp != null
                        ? ViewportTargetSpec.absoluteDp(viewportWidthDp)
                        : ViewportTargetSpec.off(),
                fontScalePercent, fontMode, null, false, dpisEnabled, systemApp,
                hyperOsNativeProxyCandidate, icon);
    }

    AppListItem(String label,
                String packageName,
                boolean inScope,
                boolean scopeKnown,
                Integer viewportWidthDp,
                Integer viewportScalePermille,
                String viewportMode,
                ViewportTargetSpec viewportTargetSpec,
                Integer fontScalePercent,
                String fontMode,
                String typefaceId,
                boolean appSpecificConfigActive,
                boolean dpisEnabled,
                boolean systemApp,
                boolean hyperOsNativeProxyCandidate,
                Drawable icon) {
        this.label = label;
        this.packageName = packageName;
        this.inScope = inScope;
        this.scopeKnown = scopeKnown;
        this.viewportTargetSpec = viewportTargetSpec != null
                ? viewportTargetSpec
                : (viewportWidthDp != null
                        ? ViewportTargetSpec.absoluteDp(viewportWidthDp)
                        : ViewportTargetSpec.off());
        this.viewportWidthDp = this.viewportTargetSpec.isAbsoluteDp()
                ? Integer.valueOf(this.viewportTargetSpec.absoluteWidthDp())
                : viewportWidthDp;
        this.viewportScalePermille = this.viewportTargetSpec.isRelativeScale()
                ? Integer.valueOf(this.viewportTargetSpec.scalePermille())
                : viewportScalePermille;
        this.viewportMode = ViewportApplyMode.normalize(viewportMode);
        this.fontScalePercent = fontScalePercent;
        this.fontMode = FontApplyMode.normalize(fontMode);
        this.typefaceId = typefaceId;
        this.appSpecificConfigActive = appSpecificConfigActive;
        this.dpisEnabled = dpisEnabled;
        this.systemApp = systemApp;
        this.hyperOsNativeProxyCandidate = hyperOsNativeProxyCandidate;
        this.icon = icon;
    }

    AppListItem(String label,
                String packageName,
                boolean inScope,
                boolean scopeKnown,
                Integer viewportWidthDp,
                String viewportMode,
                ViewportTargetSpec viewportTargetSpec,
                Integer fontScalePercent,
                String fontMode,
                String typefaceId,
                boolean appSpecificConfigActive,
                boolean dpisEnabled,
                boolean systemApp,
                boolean hyperOsNativeProxyCandidate,
                Drawable icon) {
        this(label, packageName, inScope, scopeKnown, viewportWidthDp, null, viewportMode,
                viewportTargetSpec, fontScalePercent, fontMode, typefaceId,
                appSpecificConfigActive, dpisEnabled, systemApp, hyperOsNativeProxyCandidate, icon);
    }

    boolean hasAppSpecificConfig() {
        return appSpecificConfigActive;
    }
}
