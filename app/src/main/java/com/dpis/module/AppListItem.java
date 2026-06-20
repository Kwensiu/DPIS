package com.dpis.module;

import android.graphics.drawable.Drawable;

final class AppListItem {
    final String label;
    final String packageName;
    final boolean inScope;
    final boolean scopeKnown;
    final ViewportTargetSpec viewportTargetSpec;
    final String viewportTargetType;
    final Integer viewportWidthDp;
    final Integer viewportScalePermille;
    final String viewportMode;
    final Integer fontScalePercent;
    final String fontMode;
    final String typefaceId;
    final boolean appSpecificConfigActive;
    final Integer wechatDpi;
    final boolean dpisEnabled;
    final boolean configured;
    final boolean installed;
    final boolean systemApp;
    final boolean hyperOsNativeProxyCandidate;
    final boolean previewFromGlobalPrefill;
    final String previewFontHookDomainsRaw;
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
                String viewportTargetType,
                ViewportTargetSpec viewportTargetSpec,
                Integer fontScalePercent,
                String fontMode,
                String typefaceId,
                boolean appSpecificConfigActive,
                boolean dpisEnabled,
                boolean systemApp,
                boolean hyperOsNativeProxyCandidate,
                Drawable icon) {
        this(label, packageName, inScope, scopeKnown, viewportWidthDp, viewportScalePermille,
                viewportMode, viewportTargetType, viewportTargetSpec, fontScalePercent, fontMode, typefaceId,
                appSpecificConfigActive, null, dpisEnabled, false, true,
                systemApp, hyperOsNativeProxyCandidate,
                false, null, icon);
    }

    private AppListItem(String label,
                String packageName,
                boolean inScope,
                boolean scopeKnown,
                Integer viewportWidthDp,
                Integer viewportScalePermille,
                String viewportMode,
                String viewportTargetType,
                ViewportTargetSpec viewportTargetSpec,
                Integer fontScalePercent,
                String fontMode,
                String typefaceId,
                boolean appSpecificConfigActive,
                Integer wechatDpi,
                boolean dpisEnabled,
                boolean configured,
                boolean installed,
                boolean systemApp,
                boolean hyperOsNativeProxyCandidate,
                boolean previewFromGlobalPrefill,
                String previewFontHookDomainsRaw,
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
        this.viewportTargetType = this.viewportTargetSpec.isEnabled()
                ? this.viewportTargetSpec.type()
                : ViewportTargetType.normalize(viewportTargetType);
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
        this.wechatDpi = wechatDpi;
        this.dpisEnabled = dpisEnabled;
        this.configured = configured;
        this.installed = installed;
        this.systemApp = systemApp;
        this.hyperOsNativeProxyCandidate = hyperOsNativeProxyCandidate;
        this.previewFromGlobalPrefill = previewFromGlobalPrefill;
        this.previewFontHookDomainsRaw = normalizeNullableString(previewFontHookDomainsRaw);
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
                null, viewportTargetSpec, fontScalePercent, fontMode, typefaceId,
                appSpecificConfigActive, dpisEnabled, systemApp, hyperOsNativeProxyCandidate, icon);
    }

    AppListItem(String label,
                String packageName,
                boolean inScope,
                boolean scopeKnown,
                Integer viewportWidthDp,
                Integer viewportScalePermille,
                String viewportMode,
                String viewportTargetType,
                ViewportTargetSpec viewportTargetSpec,
                Integer fontScalePercent,
                String fontMode,
                String typefaceId,
                boolean appSpecificConfigActive,
                boolean dpisEnabled,
                boolean configured,
                boolean installed,
                boolean systemApp,
                boolean hyperOsNativeProxyCandidate,
                Drawable icon) {
        this(label, packageName, inScope, scopeKnown, viewportWidthDp, viewportScalePermille,
                viewportMode, viewportTargetType, viewportTargetSpec, fontScalePercent, fontMode,
                typefaceId, appSpecificConfigActive, null, dpisEnabled, configured, installed,
                systemApp, hyperOsNativeProxyCandidate, false, null, icon);
    }

    AppListItem(String label,
                String packageName,
                boolean inScope,
                boolean scopeKnown,
                Integer viewportWidthDp,
                Integer viewportScalePermille,
                String viewportMode,
                String viewportTargetType,
                ViewportTargetSpec viewportTargetSpec,
                Integer fontScalePercent,
                String fontMode,
                String typefaceId,
                boolean appSpecificConfigActive,
                Integer wechatDpi,
                boolean dpisEnabled,
                boolean configured,
                boolean installed,
                boolean systemApp,
                boolean hyperOsNativeProxyCandidate,
                Drawable icon) {
        this(label, packageName, inScope, scopeKnown, viewportWidthDp, viewportScalePermille,
                viewportMode, viewportTargetType, viewportTargetSpec, fontScalePercent, fontMode,
                typefaceId, appSpecificConfigActive, wechatDpi, dpisEnabled, configured, installed,
                systemApp, hyperOsNativeProxyCandidate, false, null, icon);
    }

    boolean hasAppSpecificConfig() {
        return appSpecificConfigActive;
    }

    AppListItem withGlobalPrefillPreview(TemplateConfigValue prefill) {
        TemplateConfigValue normalized = prefill != null ? prefill : TemplateConfigValue.EMPTY;
        return new AppListItem(label,
                packageName,
                inScope,
                scopeKnown,
                normalized.viewportTargetSpec.isAbsoluteDp()
                        ? Integer.valueOf(normalized.viewportTargetSpec.absoluteWidthDp())
                        : viewportWidthDp,
                normalized.viewportTargetSpec.isRelativeScale()
                        ? Integer.valueOf(normalized.viewportTargetSpec.scalePermille())
                        : viewportScalePermille,
                normalized.viewportApplyMode,
                normalized.viewportTargetType,
                normalized.viewportTargetSpec,
                normalized.fontScalePercent,
                normalized.fontApplyMode,
                normalized.typefaceId,
                appSpecificConfigActive,
                wechatDpi,
                dpisEnabled,
                configured,
                installed,
                systemApp,
                hyperOsNativeProxyCandidate,
                true,
                normalized.fontHookDomainsRaw,
                icon);
    }

    private static String normalizeNullableString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
