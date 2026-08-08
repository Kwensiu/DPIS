package com.dpis.module.applist;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.templates.TemplateConfigValueAdapters;

import com.dpis.module.templates.TemplateConfigValue;

import android.graphics.drawable.Drawable;

public final class AppListItem {
    public final String label;
    public final String packageName;
    public final boolean inScope;
    public final boolean scopeKnown;
    public final ViewportTargetSpec viewportTargetSpec;
    public final String viewportTargetType;
    public final Integer viewportWidthDp;
    public final Integer viewportScaleMilliPercent;
    public final String viewportMode;
    public final Integer fontScalePercent;
    public final String fontMode;
    public final String typefaceId;
    public final boolean appSpecificConfigActive;
    public final Integer wechatDpi;
    public final boolean dpisEnabled;
    public final boolean configured;
    public final boolean installed;
    public final boolean systemApp;
    public final boolean hyperOsNativeProxyCandidate;
    public final boolean previewFromGlobalPrefill;
    public final String previewFontHookDomainsRaw;
    public final Drawable icon;

    public AppListItem(String label,
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

    public AppListItem(String label,
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

    public AppListItem(String label,
                String packageName,
                boolean inScope,
                boolean scopeKnown,
                Integer viewportWidthDp,
                Integer viewportScaleMilliPercent,
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
        this(label, packageName, inScope, scopeKnown, viewportWidthDp, viewportScaleMilliPercent,
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
                Integer viewportScaleMilliPercent,
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
        this.viewportScaleMilliPercent = this.viewportTargetSpec.isRelativeScale()
                ? Integer.valueOf(this.viewportTargetSpec.scaleMilliPercent())
                : viewportScaleMilliPercent;
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

    public AppListItem(String label,
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

    public AppListItem(String label,
                String packageName,
                boolean inScope,
                boolean scopeKnown,
                Integer viewportWidthDp,
                Integer viewportScaleMilliPercent,
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
        this(label, packageName, inScope, scopeKnown, viewportWidthDp, viewportScaleMilliPercent,
                viewportMode, viewportTargetType, viewportTargetSpec, fontScalePercent, fontMode,
                typefaceId, appSpecificConfigActive, null, dpisEnabled, configured, installed,
                systemApp, hyperOsNativeProxyCandidate, false, null, icon);
    }

    public AppListItem(String label,
                String packageName,
                boolean inScope,
                boolean scopeKnown,
                Integer viewportWidthDp,
                Integer viewportScaleMilliPercent,
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
        this(label, packageName, inScope, scopeKnown, viewportWidthDp, viewportScaleMilliPercent,
                viewportMode, viewportTargetType, viewportTargetSpec, fontScalePercent, fontMode,
                typefaceId, appSpecificConfigActive, wechatDpi, dpisEnabled, configured, installed,
                systemApp, hyperOsNativeProxyCandidate, false, null, icon);
    }

    public boolean hasAppSpecificConfig() {
        return appSpecificConfigActive;
    }

    /**
     * Returns this app snapshot with only its asynchronously loaded icon replaced.
     * Keeping the rest of the immutable state intact lets both app-list tabs refresh together.
     */
    public AppListItem withIcon(Drawable updatedIcon) {
        if (icon == updatedIcon) {
            return this;
        }
        return new AppListItem(
                label,
                packageName,
                inScope,
                scopeKnown,
                viewportWidthDp,
                viewportScaleMilliPercent,
                viewportMode,
                viewportTargetType,
                viewportTargetSpec,
                fontScalePercent,
                fontMode,
                typefaceId,
                appSpecificConfigActive,
                wechatDpi,
                dpisEnabled,
                configured,
                installed,
                systemApp,
                hyperOsNativeProxyCandidate,
                previewFromGlobalPrefill,
                previewFontHookDomainsRaw,
                updatedIcon
        );
    }

    public AppListItem withGlobalPrefillPreview(TemplateConfigValue prefill) {
        TemplateConfigValue normalized = prefill != null ? prefill : TemplateConfigValue.EMPTY;
        ViewportTargetSpec viewportTargetSpec =
                TemplateConfigValueAdapters.toViewportTargetSpec(normalized);
        return new AppListItem(label,
                packageName,
                inScope,
                scopeKnown,
                viewportTargetSpec.isAbsoluteDp()
                        ? Integer.valueOf(viewportTargetSpec.absoluteWidthDp())
                        : viewportWidthDp,
                viewportTargetSpec.isRelativeScale()
                        ? Integer.valueOf(viewportTargetSpec.scaleMilliPercent())
                        : viewportScaleMilliPercent,
                normalized.viewportApplyMode,
                normalized.viewportTargetType,
                viewportTargetSpec,
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

    public AppListItem withWechatDpi(Integer updatedWechatDpi) {
        if (java.util.Objects.equals(wechatDpi, updatedWechatDpi)) {
            return this;
        }
        return new AppListItem(
                label,
                packageName,
                inScope,
                scopeKnown,
                viewportWidthDp,
                viewportScaleMilliPercent,
                viewportMode,
                viewportTargetType,
                viewportTargetSpec,
                fontScalePercent,
                fontMode,
                typefaceId,
                appSpecificConfigActive,
                updatedWechatDpi,
                dpisEnabled,
                configured,
                installed,
                systemApp,
                hyperOsNativeProxyCandidate,
                previewFromGlobalPrefill,
                previewFontHookDomainsRaw,
                icon
        );
    }

    private static String normalizeNullableString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
