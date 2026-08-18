package com.dpis.module.appconfig;
import com.dpis.module.appconfig.EditorDraft;

import com.dpis.module.applist.AppListItem;
import com.dpis.module.ConfigEditorDestination;
import com.dpis.module.appconfig.AppConfigInputValidation;
import com.dpis.module.appconfig.WechatDpiConfig;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.viewport.ViewportTargetType;
import java.util.Set;

/** Immutable Compose-facing projection for the per-app configuration editor. */
public final class EditorPresentation {
    private EditorPresentation() {
    }

    public interface Actions {
        void updateViewportInput(String value);
        void changeViewportMode(String targetType);
        void updateFontInput(String value);
        void changeFontMode(String mode);
        void updateWechatDpiInput(String value);
        void showWechatDpiHelp();
        void updateTypeface(String typefaceId);
        void updateHookChain(String rawDomains, boolean resetDomains,
                String viewportApplyMode, boolean resetViewportApplyMode);
        void navigate(ConfigEditorDestination destination);
        void reset();
        void toggleScope();
        void toggleDpisEnabled();
        void startProcess();
        void restartProcess();
        void stopProcess();
        void startFeedbackDiagnostic();
        void save();
        void close();
    }

    public static final class State {
        public final AppListItem item;
        public final String versionName;
        public final EditorDraft draft;
        public final String typefaceSelectorText;
        public final String hookChainText;
        public final boolean dirty;
        public final boolean saveFeedbackVisible;
        public final boolean viewportInputValid;
        public final boolean fontInputValid;
        public final boolean wechatDpiInputValid;
        public final boolean saveEnabled;
        public final boolean systemHooksEnabled;
        public final Set<String> automaticFontHookDomains;
        public final ConfigEditorDestination destination;
        public final Actions actions;

        public State(AppListItem item, String versionName, EditorDraft draft, String typefaceSelectorText,
                String hookChainText, boolean dirty, boolean saveFeedbackVisible,
                boolean systemHooksEnabled, Set<String> automaticFontHookDomains,
                ConfigEditorDestination destination, Actions actions) {
            this.item = item;
            this.versionName = versionName != null ? versionName : "";
            this.draft = draft;
            this.typefaceSelectorText = typefaceSelectorText;
            this.hookChainText = hookChainText;
            this.dirty = dirty;
            this.saveFeedbackVisible = saveFeedbackVisible;
            this.systemHooksEnabled = systemHooksEnabled;
            this.automaticFontHookDomains = Set.copyOf(automaticFontHookDomains);
            this.destination = destination != null
                    ? destination
                    : ConfigEditorDestination.MAIN;
            this.actions = actions;
            viewportInputValid = AppConfigInputValidation.isViewportInputValid(
                    draft.viewportInputFor(draft.viewportMode), draft.viewportMode);
            fontInputValid = AppConfigInputValidation.isFontScaleInputValid(draft.fontInput);
            wechatDpiInputValid = !WechatDpiConfig.appliesTo(item.packageName)
                    || WechatDpiConfig.isInputValid(draft.wechatDpiInput);
            saveEnabled = viewportInputValid && fontInputValid && wechatDpiInputValid;
        }

        public boolean usesAbsoluteViewport() {
            return ViewportTargetType.ABSOLUTE_DP.equals(
                    ViewportTargetType.normalize(draft.viewportMode));
        }

        public boolean usesSystemFontMode() {
            return FontApplyMode.SYSTEM_EMULATION.equals(
                    FontApplyMode.normalize(draft.fontMode));
        }

        public boolean showsWechatDpi() {
            return WechatDpiConfig.appliesTo(item.packageName);
        }

        public boolean isDpisEnabled() {
            return draft.dpisEnabled;
        }

        public boolean isScopeSelected() {
            return draft.scopeSelected;
        }
    }
}
