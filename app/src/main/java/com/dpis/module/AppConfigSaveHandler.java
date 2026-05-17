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
            boolean saved = true;
            int hint = 0;
            if (store == null) {
                hint = R.string.status_save_requires_init;
                return new int[] { 1, hint };
            }
            if (widthDp == null) {
                saved = store.clearTargetViewportWidthDp(item.packageName) && saved;
                saved = store.setTargetViewportApplyMode(item.packageName, ViewportApplyMode.OFF)
                        && saved;
                ViewportPropertySyncer.clearTargetAsync(item.packageName);
            } else {
                saved = store.setTargetViewportWidthDp(item.packageName, widthDp) && saved;
                saved = store.setTargetViewportApplyMode(item.packageName, viewportMode)
                        && saved;
                ViewportPropertySyncer.publishTargetAsync(item.packageName, widthDp, viewportMode);
            }
            if (fontScalePercent == null) {
                saved = store.clearTargetFontScalePercent(item.packageName) && saved;
                saved = store.setTargetFontApplyMode(item.packageName, FontApplyMode.OFF) && saved;
                FontRuntimePropertySyncer.clearTargetAsync(item.packageName);
            } else {
                saved = store.setTargetFontScalePercent(item.packageName, fontScalePercent) && saved;
                saved = store.setTargetFontApplyMode(item.packageName, fontMode) && saved;
                FontRuntimePropertySyncer.publishTargetAsync(
                        item.packageName,
                        fontScalePercent,
                        fontMode,
                        store.isHyperOsFlutterFontHookEnabled());
            }
            if (saved && onChanged != null) {
                onChanged.run();
            }
            if (saved && emulationRequestedWithoutSystemScope) {
                hint = R.string.system_mode_requires_system_scope_hint;
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
