package com.dpis.module;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class FontHookDomainRegistry {
    static final String ID_RESOURCES_FONT = "resources_font";
    static final String ID_SYSTEM_SERVER_FONT = "system_server_font";
    static final String ID_ACTIVITY_THREAD_FONT = "activity_thread_font";
    static final String ID_TEXTVIEW_SP_REWRITE = "textview_sp_rewrite";
    static final String ID_TEXTVIEW_ABSOLUTE_REWRITE = "textview_absolute_rewrite";
    static final String ID_TEXTVIEW_CURRENT_PX_FALLBACK = "textview_current_px_fallback";
    static final String ID_PAINT_TEXT_SIZE_FALLBACK = "paint_text_size_fallback";
    static final String ID_WEBVIEW_TEXT_ZOOM = "webview_text_zoom";
    static final String ID_FLUTTER_SETTINGS = "flutter_settings";
    static final String ID_HYPEROS_NATIVE_FLUTTER = "hyperos_native_flutter";

    static final String GROUP_RESOURCES = "resources";
    static final String GROUP_TEXT_VIEW_FALLBACK = "text_view_fallback";
    static final String GROUP_WEB = "web";
    static final String GROUP_CROSS_RUNTIME = "cross_runtime";

    private static final int NOT_CUSTOMIZABLE = -1;

    private static final List<DomainSpec> DOMAIN_SPECS = List.of(
            new DomainSpec(ID_RESOURCES_FONT, GROUP_RESOURCES,
                    R.string.dialog_font_hook_domain_resources_font,
                    0, 0, true),
            new DomainSpec(ID_SYSTEM_SERVER_FONT, GROUP_RESOURCES,
                    R.string.dialog_font_hook_domain_system_server_font,
                    NOT_CUSTOMIZABLE, 1, false),
            new DomainSpec(ID_ACTIVITY_THREAD_FONT, GROUP_RESOURCES,
                    R.string.dialog_font_hook_domain_activity_thread_font,
                    NOT_CUSTOMIZABLE, 2, false),
            new DomainSpec(ID_TEXTVIEW_SP_REWRITE, GROUP_TEXT_VIEW_FALLBACK,
                    R.string.dialog_font_hook_domain_textview_sp_rewrite,
                    1, 3, true),
            new DomainSpec(ID_TEXTVIEW_ABSOLUTE_REWRITE, GROUP_TEXT_VIEW_FALLBACK,
                    R.string.dialog_font_hook_domain_textview_absolute_rewrite,
                    2, 4, true),
            new DomainSpec(ID_TEXTVIEW_CURRENT_PX_FALLBACK, GROUP_TEXT_VIEW_FALLBACK,
                    R.string.dialog_font_hook_domain_textview_current_px_fallback,
                    3, 5, true),
            new DomainSpec(ID_PAINT_TEXT_SIZE_FALLBACK, GROUP_TEXT_VIEW_FALLBACK,
                    R.string.dialog_font_hook_domain_paint_text_size_fallback,
                    4, 6, true),
            new DomainSpec(ID_WEBVIEW_TEXT_ZOOM, GROUP_WEB,
                    R.string.dialog_font_hook_domain_webview_text_zoom,
                    5, 7, true),
            new DomainSpec(ID_FLUTTER_SETTINGS, GROUP_CROSS_RUNTIME,
                    R.string.dialog_font_hook_domain_flutter_settings,
                    6, 8, false),
            new DomainSpec(ID_HYPEROS_NATIVE_FLUTTER, GROUP_CROSS_RUNTIME,
                    R.string.dialog_font_hook_domain_hyperos_native_flutter,
                    7, 9, false));

    private FontHookDomainRegistry() {
    }

    static Set<String> knownDomainIds() {
        return idsSortedByStableOrder(DOMAIN_SPECS);
    }

    static boolean isKnown(String domainId) {
        return specFor(domainId) != null;
    }

    static Set<String> orderedKnownSubset(Set<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (DomainSpec spec : DOMAIN_SPECS) {
            if (domains.contains(spec.id)) {
                ordered.add(spec.id);
            }
        }
        return ordered;
    }

    static List<String> orderedIdsList() {
        return knownDomainIds().stream().toList();
    }

    static List<String> orderedDisplayIdsList() {
        return specsSortedByDisplayOrder(DOMAIN_SPECS).stream()
                .map(spec -> spec.id)
                .toList();
    }

    static List<String> orderedCustomizableIdsList() {
        return customizableSpecs().stream()
                .sorted(Comparator.comparingInt(spec -> spec.customizableOrder))
                .map(spec -> spec.id)
                .toList();
    }

    static List<String> orderedCustomizableDisplayIdsList() {
        return customizableSpecs().stream()
                .sorted(Comparator.comparingInt(spec -> spec.displayOrder))
                .map(spec -> spec.id)
                .toList();
    }

    static Set<String> recommendedTemplateKnownDomains() {
        LinkedHashSet<String> recommended = new LinkedHashSet<>();
        for (DomainSpec spec : DOMAIN_SPECS) {
            if (spec.recommended) {
                recommended.add(spec.id);
            }
        }
        return orderedCustomizableDisplaySubset(recommended);
    }

    static Set<String> orderedCustomizableSubset(Set<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String id : orderedCustomizableIdsList()) {
            if (domains.contains(id)) {
                ordered.add(id);
            }
        }
        return ordered;
    }

    static Set<String> orderedCustomizableDisplaySubset(Set<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String id : orderedCustomizableDisplayIdsList()) {
            if (domains.contains(id)) {
                ordered.add(id);
            }
        }
        return ordered;
    }

    static List<String> orderedGroups() {
        return List.of(
                GROUP_RESOURCES,
                GROUP_TEXT_VIEW_FALLBACK,
                GROUP_WEB,
                GROUP_CROSS_RUNTIME);
    }

    static String groupFor(String domainId) {
        DomainSpec spec = specFor(domainId);
        return spec != null ? spec.group : "";
    }

    static int titleResFor(String domainId) {
        DomainSpec spec = specFor(domainId);
        if (spec == null) {
            throw new IllegalArgumentException("Unknown domain id: " + domainId);
        }
        return spec.titleRes;
    }

    private static List<DomainSpec> customizableSpecs() {
        return DOMAIN_SPECS.stream()
                .filter(spec -> spec.customizableOrder != NOT_CUSTOMIZABLE)
                .toList();
    }

    private static Set<String> idsSortedByStableOrder(List<DomainSpec> specs) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (DomainSpec spec : specs) {
            ids.add(spec.id);
        }
        return ids;
    }

    private static List<DomainSpec> specsSortedByDisplayOrder(List<DomainSpec> specs) {
        return specs.stream()
                .sorted(Comparator.comparingInt(spec -> spec.displayOrder))
                .toList();
    }

    private static DomainSpec specFor(String domainId) {
        if (domainId == null) {
            return null;
        }
        for (DomainSpec spec : DOMAIN_SPECS) {
            if (spec.id.equals(domainId)) {
                return spec;
            }
        }
        return null;
    }

    private static final class DomainSpec {
        final String id;
        final String group;
        final int titleRes;
        final int customizableOrder;
        final int displayOrder;
        final boolean recommended;

        DomainSpec(String id,
                   String group,
                   int titleRes,
                   int customizableOrder,
                   int displayOrder,
                   boolean recommended) {
            this.id = id;
            this.group = group;
            this.titleRes = titleRes;
            this.customizableOrder = customizableOrder;
            this.displayOrder = displayOrder;
            this.recommended = recommended;
        }
    }
}
