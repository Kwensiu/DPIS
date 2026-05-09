package com.dpis.module;

import com.google.android.material.textfield.TextInputEditText;

final class AppConfigSaveHandler {
    int[] save(AppListItem item,
            TextInputEditText viewportInput,
            TextInputEditText fontScaleInput,
            String viewportMode,
            String fontMode,
            boolean systemHooksEnabled,
            DpiConfigStore store,
            Runnable onChanged) {
        try {
            Integer widthDp = parsePositiveIntOrNull(viewportInput);
            Integer fontScalePercent = parseFontScalePercentOrNull(fontScaleInput);
            boolean viewportEmulationIneffective = widthDp != null
                    && ViewportApplyMode.SYSTEM_EMULATION.equals(
                            ViewportApplyMode.normalize(viewportMode))
                    && !ViewportApplyMode.SYSTEM_EMULATION.equals(
                            EffectiveModeResolver.resolveViewportMode(
                                    viewportMode,
                                    systemHooksEnabled));
            boolean fontEmulationIneffective = fontScalePercent != null
                    && FontApplyMode.SYSTEM_EMULATION.equals(FontApplyMode.normalize(fontMode))
                    && !FontApplyMode.SYSTEM_EMULATION.equals(
                            EffectiveModeResolver.resolveFontMode(fontMode, systemHooksEnabled));
            boolean emulationRequestedWithoutSystemScope =
                    viewportEmulationIneffective || fontEmulationIneffective;
            boolean changed = true;
            int hint = 0;
            if (store == null) {
                hint = R.string.status_save_requires_init;
                return new int[] { 1, hint };
            }
            if (widthDp == null) {
                changed = store.clearTargetViewportWidthDp(item.packageName) && changed;
                changed = store.setTargetViewportApplyMode(item.packageName, ViewportApplyMode.OFF)
                        && changed;
                ViewportPropertySyncer.clearTargetAsync(item.packageName);
            } else {
                changed = store.setTargetViewportWidthDp(item.packageName, widthDp) && changed;
                changed = store.setTargetViewportApplyMode(item.packageName, viewportMode)
                        && changed;
                if (ViewportApplyMode.isEnabled(viewportMode)) {
                    ViewportPropertySyncer.publishTargetAsync(item.packageName, widthDp);
                } else {
                    ViewportPropertySyncer.clearTargetAsync(item.packageName);
                }
            }
            if (fontScalePercent == null) {
                changed = store.clearTargetFontScalePercent(item.packageName) && changed;
                changed = store.setTargetFontApplyMode(item.packageName, FontApplyMode.OFF) && changed;
                HyperOsNativeFontPropertySyncer.clearFontTargetAsync(item.packageName);
            } else {
                changed = store.setTargetFontScalePercent(item.packageName, fontScalePercent) && changed;
                changed = store.setTargetFontApplyMode(item.packageName, fontMode) && changed;
                if (FontApplyMode.isEnabled(fontMode)) {
                    HyperOsNativeFontPropertySyncer.publishForceFontTargetAsync(
                            item.packageName, fontScalePercent);
                } else {
                    HyperOsNativeFontPropertySyncer.clearFontTargetAsync(item.packageName);
                }
            }
            if (changed && onChanged != null) {
                onChanged.run();
            }
            if (changed && emulationRequestedWithoutSystemScope) {
                hint = R.string.emulation_requires_system_scope_hint;
            }
            return new int[] { 1, hint };
        } catch (NumberFormatException exception) {
            return new int[] { 0, R.string.status_save_invalid };
        }
    }

    private static Integer parsePositiveIntOrNull(TextInputEditText inputView)
            throws NumberFormatException {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return null;
        }
        int value = Integer.parseInt(raw);
        if (value <= 0) {
            throw new NumberFormatException("must be positive");
        }
        return value;
    }

    private static Integer parseFontScalePercentOrNull(TextInputEditText inputView)
            throws NumberFormatException {
        String raw = inputView.getText() != null ? inputView.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return null;
        }
        int value = Integer.parseInt(raw);
        if (value < 50 || value > 300) {
            throw new NumberFormatException("font scale out of range");
        }
        return value;
    }
}
